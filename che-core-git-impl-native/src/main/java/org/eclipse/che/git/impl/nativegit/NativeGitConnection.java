/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.git.impl.nativegit;


import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.core.util.LineConsumerFactory;
import org.eclipse.che.api.git.Config;
import org.eclipse.che.api.git.CredentialsLoader;
import org.eclipse.che.api.git.DiffPage;
import org.eclipse.che.api.git.GitConnection;
import org.eclipse.che.api.git.exception.GitException;
import org.eclipse.che.api.git.LogPage;
import org.eclipse.che.api.git.UserCredential;
import org.eclipse.che.api.git.shared.AddRequest;
import org.eclipse.che.api.git.shared.Branch;
import org.eclipse.che.api.git.shared.CheckoutRequest;
import org.eclipse.che.api.git.shared.BranchCreateRequest;
import org.eclipse.che.api.git.shared.BranchDeleteRequest;
import org.eclipse.che.api.git.shared.BranchListRequest;
import org.eclipse.che.api.git.shared.CloneRequest;
import org.eclipse.che.api.git.shared.CommitRequest;
import org.eclipse.che.api.git.shared.DiffRequest;
import org.eclipse.che.api.git.shared.FetchRequest;
import org.eclipse.che.api.git.shared.GitUser;
import org.eclipse.che.api.git.shared.InitRequest;
import org.eclipse.che.api.git.shared.LogRequest;
import org.eclipse.che.api.git.shared.LsFilesRequest;
import org.eclipse.che.api.git.shared.LsRemoteRequest;
import org.eclipse.che.api.git.shared.MergeRequest;
import org.eclipse.che.api.git.shared.MergeResult;
import org.eclipse.che.api.git.shared.MoveRequest;
import org.eclipse.che.api.git.shared.PullRequest;
import org.eclipse.che.api.git.shared.PullResponse;
import org.eclipse.che.api.git.shared.RebaseRequest;
import org.eclipse.che.api.git.shared.RebaseResponse;
import org.eclipse.che.api.git.shared.PushRequest;
import org.eclipse.che.api.git.shared.PushResponse;
import org.eclipse.che.api.git.shared.Remote;
import org.eclipse.che.api.git.shared.RemoteAddRequest;
import org.eclipse.che.api.git.shared.RemoteListRequest;
import org.eclipse.che.api.git.shared.RemoteReference;
import org.eclipse.che.api.git.shared.RemoteUpdateRequest;
import org.eclipse.che.api.git.shared.ResetRequest;
import org.eclipse.che.api.git.shared.Revision;
import org.eclipse.che.api.git.shared.RmRequest;
import org.eclipse.che.api.git.shared.Status;
import org.eclipse.che.api.git.shared.StatusFormat;
import org.eclipse.che.api.git.shared.Tag;
import org.eclipse.che.api.git.shared.TagCreateRequest;
import org.eclipse.che.api.git.shared.TagDeleteRequest;
import org.eclipse.che.api.git.shared.TagListRequest;
import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.git.impl.nativegit.commands.AddCommand;
import org.eclipse.che.git.impl.nativegit.commands.BranchCreateCommand;
import org.eclipse.che.git.impl.nativegit.commands.BranchDeleteCommand;
import org.eclipse.che.git.impl.nativegit.commands.BranchListCommand;
import org.eclipse.che.git.impl.nativegit.commands.BranchRenameCommand;
import org.eclipse.che.git.impl.nativegit.commands.CloneCommand;
import org.eclipse.che.git.impl.nativegit.commands.CommitCommand;
import org.eclipse.che.git.impl.nativegit.commands.EmptyGitCommand;
import org.eclipse.che.git.impl.nativegit.commands.FetchCommand;
import org.eclipse.che.git.impl.nativegit.commands.InitCommand;
import org.eclipse.che.git.impl.nativegit.commands.LogCommand;
import org.eclipse.che.git.impl.nativegit.commands.LsRemoteCommand;
import org.eclipse.che.git.impl.nativegit.commands.PullCommand;
import org.eclipse.che.git.impl.nativegit.commands.PushCommand;
import org.eclipse.che.git.impl.nativegit.commands.RemoteListCommand;
import org.eclipse.che.git.impl.nativegit.commands.RemoteOperationCommand;
import org.eclipse.che.git.impl.nativegit.ssh.GitSshScriptProvider;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Native implementation of GitConnection
 *
 * @author Eugene Voevodin
 */
public class NativeGitConnection implements GitConnection {

    private static final Pattern authErrorPattern =
            Pattern.compile(
                    ".*fatal: could not read (Username|Password) for '.*': No such device or address.*|" +
                    ".*fatal: could not read (Username|Password) for '.*': Input/output error.*|" +
                    ".*fatal: Authentication failed for '.*'.*|.*fatal: Could not read from remote repository\\.\\n\\nPlease make sure " +
                    "you have the correct access rights\\nand the repository exists\\.\\n.*",
                    Pattern.MULTILINE);
    private final NativeGit         nativeGit;

    private static final Pattern notInGitRepoErrorPattern = Pattern.compile("^fatal: Not a git repository.*(\\n.*)*$", Pattern.MULTILINE);
    private final CredentialsLoader credentialsLoader;
    private final File mountRoot;

    /**
     * @param mountRoot
     *          directory where mount virtual file system @see property vfs.local.fs_root_dir
     * @param repository
     *         directory where commands will be invoked
     * @param gitSshScriptProvider
     *         manager for ssh keys. If it is null default ssh will be used;
     * @param credentialsLoader
     *         loader for credentials
     * @throws GitException
     *         when some error occurs
     */
    public NativeGitConnection(File mountRoot, File repository, GitSshScriptProvider gitSshScriptProvider,
                               CredentialsLoader credentialsLoader) throws GitException {
        this(mountRoot, new NativeGit(repository, gitSshScriptProvider, credentialsLoader, new GitAskPassScript()), credentialsLoader);
    }

    /**
     * @param nativeGit
     *         native git client
     * @param credentialsLoader
     *         loader for credentials
     * @throws GitException
     *         when some error occurs
     */
    public NativeGitConnection(File mountRoot, NativeGit nativeGit, CredentialsLoader credentialsLoader)
            throws GitException {
        this.mountRoot = mountRoot;
        this.credentialsLoader = credentialsLoader;
        this.nativeGit = nativeGit;
    }

    @Override
    public File getWorkingDir() {
        return nativeGit.getRepository();
    }

    @Override
    public void add(AddRequest request) throws GitException {
        ensureExistenceRepoRootInWorkingDirectory();
        AddCommand command = nativeGit.createAddCommand();
        command.setFilePattern(request.getFilepattern() == null ?
                               AddRequest.DEFAULT_PATTERN :
                               request.getFilepattern());
        command.setUpdate(request.isUpdate());
        command.execute();
    }

    @Override
    public void checkout(CheckoutRequest request) throws ApiException {
        ensureExistenceRepoRootInWorkingDirectory();
        nativeGit.createCheckoutCommand()
                 .setBranchName(request.getName())
                 .setStartPoint(request.getStartPoint())
                 .setCreateNew(request.isCreateNew())
                 .setTrackBranch(request.getTrackBranch())
                 .setFilePaths(request.getFiles())
                 .execute();
    }

    @Override
    public Branch branchCreate(BranchCreateRequest request) throws GitException {
        ensureExistenceRepoRootInWorkingDirectory();
        BranchCreateCommand branchCreateCommand = nativeGit.createBranchCreateCommand();
        branchCreateCommand.setBranchName(request.getName())
                           .setStartPoint(request.getStartPoint())
                           .execute();
        return DtoFactory.getInstance().createDto(Branch.class).withName(getBranchRef(request.getName())).withActive(false)
                         .withDisplayName(request.getName()).withRemote(false);
    }

    @Override
    public void branchDelete(BranchDeleteRequest request) throws GitException, UnauthorizedException {
        ensureExistenceRepoRootInWorkingDirectory();
        String branchName = getBranchRef(request.getName());
        String remoteName = null;
        String remoteUri = null;

        if (branchName.startsWith("refs/remotes/")) {
            remoteName = parseRemoteName(branchName);
            remoteUri = getRemoteUri(remoteName);
        }
        branchName = parseBranchName(branchName);

        BranchDeleteCommand branchDeleteCommand = nativeGit.createBranchDeleteCommand();

        branchDeleteCommand.setBranchName(branchName)
                           .setRemote(remoteName)
                           .setDeleteFullyMerged(request.isForce())
                           .setRemoteUri(remoteUri);

        executeRemoteCommand(branchDeleteCommand);
    }

    @Override
    public void branchRename(String oldName, String newName) throws GitException, UnauthorizedException {
        ensureExistenceRepoRootInWorkingDirectory();
        String branchName = getBranchRef(oldName);
        String remoteName = null;
        String remoteUri = null;

        if (branchName.startsWith("refs/remotes/")) {
            remoteName = parseRemoteName(branchName);
            remoteUri = getRemoteUri(remoteName);
        }

        branchName = remoteName != null ? parseBranchName(branchName) : oldName;

        BranchRenameCommand branchRenameCommand = nativeGit.createBranchRenameCommand();

        branchRenameCommand.setNames(branchName, newName)
                           .setRemote(remoteName)
                           .setRemoteUri(remoteUri);

        executeRemoteCommand(branchRenameCommand);
    }

    @Override
    public List<Branch> branchList(BranchListRequest request) throws GitException {
        ensureExistenceRepoRootInWorkingDirectory();
        String listMode = request.getListMode();
        if (listMode != null
            && !(listMode.equals(BranchListRequest.LIST_ALL) || listMode.equals(BranchListRequest.LIST_REMOTE))) {
            throw new IllegalArgumentException("Unsupported list mode '" + listMode + "'. Must be either 'a' or 'r'. ");
        }
        List<Branch> branches;
        BranchListCommand branchListCommand = nativeGit.createBranchListCommand();
        if (request.getListMode() == null) {
            branches = branchListCommand.execute();
        } else if (request.getListMode().equals(BranchListRequest.LIST_ALL)) {
            branches = branchListCommand.execute();
            branches.addAll(branchListCommand.setShowRemotes(true).execute());
        } else {
            branches = branchListCommand.setShowRemotes(true).execute();
        }
        return branches;
    }

    @Override
    public List<String> listFiles(LsFilesRequest request) throws GitException {
        ensureExistenceRepoRootInWorkingDirectory();
        return nativeGit.createListFilesCommand()
                        .setOthers(request.isOthers())
                        .setModified(request.isModified())
                        .setStaged(request.isStaged())
                        .setCached(request.isCached())
                        .setDeleted(request.isDeleted())
                        .setIgnored(request.isIgnored())
                        .setExcludeStandard(request.isExcludeStandard())
                        .execute();
    }

    @Override
    public void clone(CloneRequest request) throws URISyntaxException, UnauthorizedException, GitException {
        final String remoteUri = request.getRemoteUri();
        CloneCommand clone = nativeGit.createCloneCommand();
        clone.setRemoteUri(remoteUri);
        clone.setRemoteName(request.getRemoteName());
        clone.setRecursiveEnabled(request.getRecursiveEnabled());
        if (clone.getTimeout() > 0) {
            clone.setTimeout(request.getTimeout());
        }

        executeRemoteCommand(clone);

        UserCredential credentials = credentialsLoader.getUserCredential(remoteUri);
        if (credentials != null) {
            getConfig().set("codenvy.credentialsProvider", credentials.getProviderId());
        }
        nativeGit.createRemoteUpdateCommand()
                 .setRemoteName(request.getRemoteName() == null ? "origin" : request.getRemoteName())
                 .setNewUrl(remoteUri)
                 .execute();
    }

    @Override
    public Revision commit(CommitRequest request) throws GitException {
        ensureExistenceRepoRootInWorkingDirectory();
        CommitCommand command = nativeGit.createCommitCommand();
        GitUser committer = getLocalCommitter();
        command.setCommitter(committer);

        try {
            // overrider author from .gitconfig. We may set it in previous versions.
            // We need to override it since committer can differ from the person who clone or init repository.
            getConfig().get("user.name");
            command.setAuthor(committer);
        } catch (GitException e) {
            //ignore property not found.
        }


        command.setAll(request.isAll());
        command.setAmend(request.isAmend());
        command.setMessage(request.getMessage());
        command.setFiles(request.getFiles());

        try {
            command.execute();
            LogCommand log = nativeGit.createLogCommand();
            Revision rev = log.execute().get(0);
            rev.setBranch(getCurrentBranch());
            return rev;
        } catch (Exception e) {
            Revision revision = DtoFactory.getInstance().createDto(Revision.class);
            revision.setMessage(e.getMessage());
            revision.setFake(true);
            return revision;
        }
    }

    @Override
    public DiffPage diff(DiffRequest request) throws GitException {
        ensureExistenceRepoRootInWorkingDirectory();
        return new NativeGitDiffPage(request, nativeGit);
    }

    @Override
    public void fetch(FetchRequest request) throws GitException, UnauthorizedException {
        ensureExistenceRepoRootInWorkingDirectory();
        String remoteUri = getRemoteUri(request.getRemote());
        FetchCommand fetchCommand = nativeGit.createFetchCommand();
        fetchCommand.setRemote(request.getRemote())
                    .setPrune(request.isRemoveDeletedRefs())
                    .setRefSpec(request.getRefSpec())
                    .setRemoteUri(remoteUri)
                    .setTimeout(request.getTimeout());
        executeRemoteCommand(fetchCommand);
    }

    @Override
    public void init(InitRequest request) throws GitException {
        InitCommand initCommand = nativeGit.createInitCommand();
        initCommand.setBare(request.isBare());
        initCommand.execute();
        //make initial commit.
        if (!request.isBare() && request.isInitCommit()) {
            try {
                nativeGit.createAddCommand()
                         .setFilePattern(new ArrayList<>(Collections.singletonList(".")))
                         .execute();
                nativeGit.createCommitCommand()
                         .setCommitter(getLocalCommitter())
                         .setMessage("init")
                         .execute();
            } catch (GitException ignored) {
                //if nothing to commit
            }
        }
    }

    @Override
    public LogPage log(LogRequest request) throws GitException {
        ensureExistenceRepoRootInWorkingDirectory();
        return new LogPage(nativeGit.createLogCommand().execute());
    }

    @Override
    public List<RemoteReference> lsRemote(LsRemoteRequest request) throws GitException, UnauthorizedException {
        ensureExistenceRepoRootInWorkingDirectory();
        LsRemoteCommand command = nativeGit.createLsRemoteCommand().setRemoteUrl(request.getRemoteUrl());
        executeRemoteCommand(command);
        return command.getRemoteReferences();
    }

    @Override
    public MergeResult merge(MergeRequest request) throws GitException {
        ensureExistenceRepoRootInWorkingDirectory();
        final String gitObjectType = getRevisionType(request.getCommit());
        if (!("commit".equalsIgnoreCase(gitObjectType) || "tag".equalsIgnoreCase(gitObjectType))) {
            throw new GitException("Invalid object for merge " + request.getCommit() + ".");
        }
        return nativeGit.createMergeCommand().setCommit(request.getCommit()).setCommitter(getLocalCommitter()).execute();
    }

    @Override
    public RebaseResponse rebase(RebaseRequest request) throws GitException {
    	throw new GitException("Unsupported method");
    }

    @Override
    public void mv(MoveRequest request) throws GitException {
        ensureExistenceRepoRootInWorkingDirectory();
        nativeGit.createMoveCommand()
                 .setSource(request.getSource())
                 .setTarget(request.getTarget())
                 .execute();
    }

    @Override
    public PullResponse pull(PullRequest request) throws GitException, UnauthorizedException {
        ensureExistenceRepoRootInWorkingDirectory();
        String remoteUri = getRemoteUri(request.getRemote());

        PullCommand pullCommand = nativeGit.createPullCommand();
        pullCommand.setRemote(request.getRemote())
                   .setRefSpec(request.getRefSpec())
                   .setAuthor(getLocalCommitter())
                   .setRemoteUri(remoteUri)
                   .setTimeout(request.getTimeout());

        executeRemoteCommand(pullCommand);

        return pullCommand.getPullResponse();
    }

    @Override
    public PushResponse push(PushRequest request) throws GitException, UnauthorizedException {
        ensureExistenceRepoRootInWorkingDirectory();
        String remoteUri = getRemoteUri(request.getRemote());

        PushCommand pushCommand = nativeGit.createPushCommand();

        pushCommand.setRemote(request.getRemote())
                   .setForce(request.isForce())
                   .setRefSpec(request.getRefSpec())
                   .setRemoteUri(remoteUri)
                   .setTimeout(request.getTimeout());

        executeRemoteCommand(pushCommand);

        return pushCommand.getPushResponse();
    }

    @Override
    public void remoteAdd(RemoteAddRequest request) throws GitException {
        ensureExistenceRepoRootInWorkingDirectory();
        nativeGit.createRemoteAddCommand()
                 .setName(request.getName())
                 .setUrl(request.getUrl())
                 .setBranches(request.getBranches())
                 .execute();
    }

    @Override
    public void remoteDelete(String name) throws GitException {
        ensureExistenceRepoRootInWorkingDirectory();
        nativeGit.createRemoteDeleteCommand().setName(name).execute();
    }

    @Override
    public List<Remote> remoteList(RemoteListRequest request) throws GitException {
        ensureExistenceRepoRootInWorkingDirectory();
        RemoteListCommand remoteListCommand = nativeGit.createRemoteListCommand();
        return remoteListCommand.setRemoteName(request.getRemote()).execute();
    }

    @Override
    public void remoteUpdate(RemoteUpdateRequest request) throws GitException {
        ensureExistenceRepoRootInWorkingDirectory();
        nativeGit.createRemoteUpdateCommand()
                 .setRemoteName(request.getName())
                 .setAddUrl(request.getAddUrl())
                 .setBranchesToAdd(request.getBranches())
                 .setAddBranches(request.isAddBranches())
                 .setAddPushUrl(request.getAddPushUrl())
                 .setRemovePushUrl(request.getRemovePushUrl())
                 .setRemoveUrl(request.getRemoveUrl())
                 .execute();
    }

    @Override
    public void reset(ResetRequest request) throws GitException {
        ensureExistenceRepoRootInWorkingDirectory();
        nativeGit.createResetCommand()
                 .setMode(request.getType().getValue())
                 .setCommit(request.getCommit())
                 .setFilePattern(request.getFilePattern())
                 .execute();
    }

    @Override
    public void rm(RmRequest request) throws GitException {
        ensureExistenceRepoRootInWorkingDirectory();
        nativeGit.createRemoveCommand()
                 .setCached(request.isCached())
                 .setListOfItems(request.getItems())
                 .setRecursively(request.isRecursively())
                 .execute();
    }

    @Override
    public Status status(final StatusFormat format) throws GitException {
        ensureExistenceRepoRootInWorkingDirectory();
        return new NativeGitStatusImpl(getCurrentBranch(), nativeGit, format);
    }

    @Override
    public Tag tagCreate(TagCreateRequest request) throws GitException {
        ensureExistenceRepoRootInWorkingDirectory();
        return nativeGit.createTagCreateCommand().setName(request.getName())
                        .setCommitter(getLocalCommitter())
                        .setCommit(request.getCommit())
                        .setMessage(request.getMessage())
                        .setForce(request.isForce())
                        .execute();
    }

    @Override
    public void tagDelete(TagDeleteRequest request) throws GitException {
        ensureExistenceRepoRootInWorkingDirectory();
        nativeGit.createTagDeleteCommand().setName(request.getName()).execute();
    }

    @Override
    public List<Tag> tagList(TagListRequest request) throws GitException {
        ensureExistenceRepoRootInWorkingDirectory();
        return nativeGit.createTagListCommand().setPattern(request.getPattern()).execute();
    }

    @Override
    public List<GitUser> getCommiters() throws GitException {
        ensureExistenceRepoRootInWorkingDirectory();
        List<GitUser> users = new LinkedList<>();
        List<Revision> revList = nativeGit.createLogCommand().execute();
        for (Revision rev : revList) {
            users.add(rev.getCommitter());
        }
        return users;
    }

    @Override
    public Config getConfig() throws GitException {
        ensureExistenceRepoRootInWorkingDirectory();
        return nativeGit.createConfig();
    }

    @Override
    public void close() {
        //do not need to do anything
    }

    /**
     * Ensure existence repository root directory inside working directory and in our virtual file system
     *
     * @throws GitException
     *         if git root folder is not in working directory
     */
    void ensureExistenceRepoRootInWorkingDirectory() throws GitException {
        if (isInsideWorkTree()) {
            final EmptyGitCommand emptyGitCommand = nativeGit.createEmptyGitCommand();
            emptyGitCommand.setNextParameter("rev-parse").setNextParameter("--git-dir").execute();
            final String gitDir = emptyGitCommand.getText();
            //here we check that git repo inside our file system mount point
            if (!gitDir.startsWith(mountRoot.getAbsolutePath()) && !gitDir.equals(".git")) {
                throw new GitException("Project is not a git repository.");
            }
        } else {
            throw new GitException("Project is not a git repository.");
        }

    }


    /**
     * Ensure existence repository root directory inside working directory
     *
     * @throws GitException
     *         if git root folder is not in working directory
     */
     boolean isInsideWorkTree() throws GitException {
        final EmptyGitCommand emptyGitCommand = nativeGit.createEmptyGitCommand();
        // command "rev-parse --is-inside-work-tree" returns true/false
        try {
            emptyGitCommand.setNextParameter("rev-parse")
                           .setNextParameter("--is-inside-work-tree")
                           .execute();

            final String output = emptyGitCommand.getText();
            return Boolean.valueOf(output);
        } catch (GitException ge) {
            String msg = ge.getMessage();
            if (msg != null && notInGitRepoErrorPattern.matcher(msg).matches()) {
                return false;
            }
            throw ge;
        }
    }

    @Override
    public void setOutputLineConsumerFactory(LineConsumerFactory gitOutputPublisherFactory) {
        nativeGit.setOutputLineConsumerFactory(gitOutputPublisherFactory);
    }

    /**
     * Gets current branch name.
     *
     * @return name of current branch or <code>null</code> if current branch not exists
     * @throws GitException
     *         if any error occurs
     */
    private String getCurrentBranch() throws GitException {
        BranchListCommand command = nativeGit.createBranchListCommand();
        command.execute();
        String branchName = null;
        for (String outLine : command.getLines()) {
            if (outLine.indexOf('*') != -1) {
                branchName = outLine.substring(2);
            }
        }
        return branchName;
    }

    /**
     * Executes remote command.
     * <p/>
     * Note: <i>'need for authorization'</i> check based on command execution fail message, so this
     * check can fail when i.e. git version updated, for more information see {@link #isOperationNeedAuth(String)}
     *
     * @param command
     *         remote command which should be executed
     * @throws GitException
     *         when error occurs while {@code command} execution is going except of unauthorized error
     * @throws UnauthorizedException
     *         when it is not possible to execute {@code command} with existing credentials
     */
    private void executeRemoteCommand(RemoteOperationCommand<?> command) throws GitException, UnauthorizedException {
        try {
            command.execute();
        } catch (GitException gitEx) {
            if (!isOperationNeedAuth(gitEx.getMessage())) {
                throw gitEx;
            }
            throw new UnauthorizedException(gitEx.getMessage());
        }
    }

    /**
     * Check if error message from git output corresponding authenticate issue.
     */
    private boolean isOperationNeedAuth(String errorMessage) {
        return authErrorPattern.matcher(errorMessage).find();
    }

    /**
     * Gets branch ref by branch name.
     *
     * @param branchName
     *         existing git branch name
     * @return ref to the branch
     * @throws GitException
     *         when it is not possible to get branchName ref
     */
    private String getBranchRef(String branchName) throws GitException {
        EmptyGitCommand command = nativeGit.createEmptyGitCommand();
        command.setNextParameter("show-ref").setNextParameter(branchName).execute();
        final String output = command.getText();

        if (output.isEmpty()) {
            throw new GitException("Error getting reference of branch.");
        }

        return output.split(" ")[1];
    }


    /**
     * Gets type of git object.
     *
     * @param gitObject
     *         revision object e.g. commit, tree, blob, tag.
     * @return type of git object
     */
    private String getRevisionType(String gitObject) throws GitException {
        EmptyGitCommand command = nativeGit.createEmptyGitCommand()
                                           .setNextParameter("cat-file")
                                           .setNextParameter("-t")
                                           .setNextParameter(gitObject);
        command.execute();
        return command.getText();
    }

    private String parseBranchName(String name) {
        int branchNameIndex = 0;
        if (name.startsWith("refs/remotes/")) {
            branchNameIndex = name.indexOf("/", "refs/remotes/".length()) + 1;
        } else if (name.startsWith("refs/heads/")) {
            branchNameIndex = name.indexOf("/", "refs/heads".length()) + 1;
        }
        return name.substring(branchNameIndex);
    }

    private String parseRemoteName(String branchRef) {
        int remoteStartIndex = "refs/remotes/".length();
        int remoteEndIndex = branchRef.indexOf("/", remoteStartIndex);
        return branchRef.substring(remoteStartIndex, remoteEndIndex);
    }

    private GitUser getLocalCommitter() throws GitException {
        String credentialsProvider = "che";
        try {
            credentialsProvider = getConfig().get("codenvy.credentialsProvider");
        } catch (GitException e) {
            //ignore property not found.
        }

        return credentialsLoader.getUser(credentialsProvider);
    }


    private String getRemoteUri(String remoteName) throws GitException {
        List<Remote> remotes;
        try {
            remotes = nativeGit.createRemoteListCommand()
                    .setRemoteName(remoteName)
                    .execute();
        } catch (GitException ignored) {
            return remoteName;
        }

        if (remotes.isEmpty()) {
            throw new GitException("No remote repository specified.  " +
                    "Please, specify either a URL or a remote name from which new revisions should be fetched in request.");
        }

        return remotes.get(0).getUrl();
    }

}
