/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - API 
 *   SAP           - initial implementation
 *******************************************************************************/
package org.eclipse.che.git.impl.jgit;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.core.util.LineConsumerFactory;
import org.eclipse.che.api.git.Config;
import org.eclipse.che.api.git.CredentialsLoader;
import org.eclipse.che.api.git.DiffPage;
import org.eclipse.che.api.git.GitConnection;
import org.eclipse.che.api.git.GitException;
import org.eclipse.che.api.git.LogPage;
import org.eclipse.che.api.git.UserCredential;
import org.eclipse.che.api.git.shared.AddRequest;
import org.eclipse.che.api.git.shared.Branch;
import org.eclipse.che.api.git.shared.BranchCreateRequest;
import org.eclipse.che.api.git.shared.BranchDeleteRequest;
import org.eclipse.che.api.git.shared.BranchListRequest;
import org.eclipse.che.api.git.shared.CheckoutRequest;
import org.eclipse.che.api.git.shared.CloneRequest;
import org.eclipse.che.api.git.shared.CommitRequest;
import org.eclipse.che.api.git.shared.DiffRequest;
import org.eclipse.che.api.git.shared.FetchRequest;
import org.eclipse.che.api.git.shared.GitRequest;
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
import org.eclipse.che.api.git.shared.PushRequest;
import org.eclipse.che.api.git.shared.PushResponse;
import org.eclipse.che.api.git.shared.RebaseRequest;
import org.eclipse.che.api.git.shared.RebaseResponse;
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
import org.eclipse.che.git.impl.jgit.ssh.SshUtil;
import org.eclipse.che.git.impl.jgit.shared.JGitRebaseResponse;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.RebaseCommand;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.TagCommand;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.DetachedHeadException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.RefUpdate.Result;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a>
 * @version $Id: JGitConnection.java 22817 2011-03-22 09:17:52Z andrew00x $
 */
public class JGitConnection implements GitConnection {
    private static final String REBASE_OPERATION_SKIP = "SKIP";
    private static final String REBASE_OPERATION_CONTINUE = "CONTINUE";
    private static final String REBASE_OPERATION_ABORT = "ABORT";
    private static final String ADD_ALL_OPTION = "all";
    // The JGit repository
    private final Repository repository;
    // The git user
    private final GitUser user;
    // Configuration object
    JGitConfigImpl _config;
    // Git stuff
    Git _git;
    private static final Logger LOG = LoggerFactory.getLogger(JGitConnection.class);

    private final CredentialsLoader credentialsLoader;

    /**
     * @param repository
     *            the JGit repository
     * @param user
     *            the user
     */
    @Inject
    JGitConnection(Repository repository, GitUser user, CredentialsLoader credentialsLoader) {
        this.repository = repository;
        this.user = user;
        this.credentialsLoader = credentialsLoader;
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#add(org.exoplatform.ide.git.shared.AddRequest) */
    @Override
    public void add(AddRequest request) throws GitException {
        add(request, request.isUpdate());

        // "all" option, when update is false, should run git add with both
        // update true and update false
        if ((!request.isUpdate()) && request.getAttributes() != null
                && request.getAttributes().containsKey(ADD_ALL_OPTION)) {
            add(request, true);
        }
    }

    /*
     * Perform the "add" according to the add request. isUpdate is always used
     * as the value for the "update" parameter instead of the value in the
     * AddRequest.
     */
    private void add(AddRequest request, boolean isUpdate) throws GitException {
        AddCommand addCommand = getGit().add().setUpdate(isUpdate);

        List<String> filepattern = request.getFilepattern();
        if (filepattern == null) {
            filepattern = AddRequest.DEFAULT_PATTERN;
        }
        for (String filepatternItem : filepattern) {
            addCommand.addFilepattern(filepatternItem);
        }

        try {
            addCommand.call();
        } catch (GitAPIException e) {
            throw new GitException(e.getMessage(), e);
        }
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#checkout(org.exoplatform.ide.git.shared.CheckoutRequest) */
    @Override
    public void checkout(CheckoutRequest request) throws GitException {
        CheckoutCommand checkoutCommand = getGit().checkout();

        // checkout files?
        List<String> files = request.getFiles();
        if (files != null && !files.isEmpty()) {
            for (int i = 0; i < files.size(); i++) {
                checkoutCommand.addPath(files.get(i));
            }
            try {
                checkoutCommand.call();

                // remove untracked
                Status status = this.status(StatusFormat.LONG);
                List<String> untracked = status.getUntracked();
                for (int i = 0; i < untracked.size(); i++) {
                    if (files.contains(untracked.get(i))) {
                        // remove file
                        File f = new File(repository.getWorkTree(), untracked.get(i));
                        f.delete();
                    }

                }

            } catch (GitAPIException e) {
                throw new GitException(e.getMessage(), e);
            }
        } else {
            // checkout branch
            String startPoint = request.getStartPoint();
            String cleanName = cleanRemoteName(request.getName());
            if (startPoint != null) {
                checkoutCommand.setStartPoint(startPoint);
            }
            checkoutCommand.setCreateBranch(request.isCreateNew());
            checkoutCommand.setName(cleanName);
            checkoutCommand.setUpstreamMode(SetupUpstreamMode.SET_UPSTREAM);
            try {
                checkoutCommand.call();
            } catch (GitAPIException e) {
                if (e.getMessage().endsWith("already exists")) {
                    throw new GitException(Messages.getString("ERROR_BRANCH_NAME_EXISTS", cleanName));
                }
                throw new GitException(e.getMessage(), e.getCause());
            }
        }
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#branchCreate(org.exoplatform.ide.git.shared.BranchCreateRequest) */
    @Override
    public Branch branchCreate(BranchCreateRequest request) throws GitException {
        CreateBranchCommand createBranchCommand = getGit().branchCreate().setName(request.getName());
        String start = request.getStartPoint();
        if (start != null) {
            createBranchCommand.setStartPoint(start);
        }
        try {
            Ref brRef = createBranchCommand.call();
            String refName = brRef.getName();
            String displayName = Repository.shortenRefName(refName);
            Branch branch = createDto(Branch.class);
            return branch.withName(refName).withActive(false).withDisplayName(displayName).withRemote(false);
        } catch (GitAPIException e) {
            throw new GitException(e);
        }
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#branchDelete(org.exoplatform.ide.git.shared.BranchDeleteRequest) */
    @Override
    public void branchDelete(BranchDeleteRequest request) throws GitException {
        try {
            getGit().branchDelete().setBranchNames(request.getName()).setForce(request.isForce()).call();
        } catch (GitAPIException e) {
            throw new GitException(e);
        }
    }

    /**
     * @see org.exoplatform.ide.git.server.GitConnection#branchRename(String
     *      oldName, String newName)
     */
    @Override
    public void branchRename(String oldName, String newName) throws GitException {
        try {
            getGit().branchRename().setOldName(oldName).setNewName(newName).call();
        } catch (GitAPIException e) {
            throw new GitException(e);
        }
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#branchList(org.exoplatform.ide.git.shared.BranchListRequest) */
    @Override
    public List<Branch> branchList(BranchListRequest request) throws GitException {
        String listMode = request.getListMode();
        if (listMode != null
                && !(listMode.equals(BranchListRequest.LIST_ALL) || listMode.equals(BranchListRequest.LIST_REMOTE))) {
            throw new IllegalArgumentException("Unsupported list mode '" + listMode + "'. Must be either 'a' or 'r'. ");
        }

        ListBranchCommand listBranchCommand = getGit().branchList();
        if (listMode != null) {
            if (listMode.equals(BranchListRequest.LIST_ALL)) {
                listBranchCommand.setListMode(ListMode.ALL);
            } else if (listMode.equals(BranchListRequest.LIST_REMOTE)) {
                listBranchCommand.setListMode(ListMode.REMOTE);
            }
        }
        List<Ref> refs;
        try {
            refs = listBranchCommand.call();
        } catch (GitAPIException err) {
            throw new GitException(err);
        }
        String current = null;
        try {
            Ref headRef = repository.getRef(Constants.HEAD);
            if (headRef != null && !(Constants.HEAD.equals(headRef.getLeaf().getName()))) {
                current = headRef.getLeaf().getName();
            }
        } catch (IOException e) {
            throw new GitException(e.getMessage(), e);
        }

        List<Branch> branches = new ArrayList<Branch>();
        if (current == null) {
            branches.add(createDto(Branch.class).withName("(no branch)").withActive(true).withDisplayName("(no name)")
                    .withRemote(false));
        }

        for (Ref brRef : refs) {
            String refName = brRef.getName();
            Branch branch = createDto(Branch.class).withName(refName).withActive(refName.equals(current))
                    .withDisplayName(Repository.shortenRefName(refName))
                    .withRemote(brRef.getName().startsWith("refs/remotes"));
            branches.add(branch);
        }
        return branches;
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#clone(org.exoplatform.ide.git.shared.CloneRequest) */
    public void clone(CloneRequest request) throws GitException {
        String remoteUri = null;
        try {
            if (request.getRemoteName() == null) {
                request.setRemoteName(Constants.DEFAULT_REMOTE_NAME);
            }
            if (request.getWorkingDir() == null) {
                request.setWorkingDir(repository.getWorkTree().getCanonicalPath());
            }
            CloneCommand cloneCom = Git.cloneRepository();
            cloneCom.setRemote(request.getRemoteName());
            remoteUri = request.getRemoteUri();
            cloneCom.setURI(remoteUri);
            cloneCom.setDirectory(new File(request.getWorkingDir()));
            if (request.getBranchesToFetch() != null) {
                cloneCom.setBranchesToClone(new ArrayList<String>(request.getBranchesToFetch()));
            } else {
                cloneCom.setCloneAllBranches(true);
            }

            setRemoteCommandCredentials(request, remoteUri, cloneCom);

            Repository repo = cloneCom.call().getRepository();
            StoredConfig config = repo.getConfig();
            GitUser gitUser = getUser();
            if (gitUser != null) {
                config.setString(ConfigConstants.CONFIG_USER_SECTION, null, ConfigConstants.CONFIG_KEY_NAME,
                        gitUser.getName());
                config.setString(ConfigConstants.CONFIG_USER_SECTION, null, ConfigConstants.CONFIG_KEY_EMAIL,
                        gitUser.getEmail());
            }

            config.save();

        } catch (GitAPIException | IOException e) {
            throw new GitException(e);
        } 
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#commit(org.exoplatform.ide.git.shared.CommitRequest) */
    @Override
    public Revision commit(CommitRequest request) throws GitException {
        try {
            if (!repository.getRepositoryState().canCommit()) {
                Revision rev = createDto(Revision.class);
                rev.setMessage("Commit is not possible because repository state is '"
                        + repository.getRepositoryState().getDescription() + "'");
                return rev;
            }

            if (request.isAmend() && !repository.getRepositoryState().canAmend()) {
                Revision rev = createDto(Revision.class);
                rev.setMessage("Amend is not possible because repository state is '"
                        + repository.getRepositoryState().getDescription() + "'");
                return rev;
            }

            // TODO had to set message line NativeGitConnect because JGit's
            // previous implementation uses
            // stat.createString
            Status stat = status(StatusFormat.LONG);
            if (stat.getAdded().isEmpty() && stat.getChanged().isEmpty() && stat.getRemoved().isEmpty()) {
                if (request.isAll()) {
                    if (stat.getMissing().isEmpty() && stat.getModified().isEmpty()) {
                        return createDto(Revision.class).withMessage(request.getMessage());
                    }
                } else {
                    if (stat.getMissing().isEmpty() && stat.getModified().isEmpty()) {
                        return createDto(Revision.class).withMessage(request.getMessage());
                    } else {
                        return createDto(Revision.class).withMessage(request.getMessage());
                    }
                }
            }

            CommitCommand commitCommand = getGit().commit();

            // Always take the committer name and email from the current user
            // (in case it changed between
            // the time the repository was cloned and this commit request)
            // TODO once we support user configuration of the name and email,
            // this should be reverted back
            // to read from the configuration
            // String configName = repository.getConfig().getString(
            // ConfigConstants.CONFIG_USER_SECTION, null,
            // ConfigConstants.CONFIG_KEY_NAME);
            // String configEmail = repository.getConfig().getString(
            // ConfigConstants.CONFIG_USER_SECTION, null,
            // ConfigConstants.CONFIG_KEY_EMAIL);
            //
            // String gitName = getUser().getName();
            // String gitEmail = getUser().getEmail();
            //
            // String comitterName = configName != null ? configName : gitName;
            // String comitterEmail = configEmail != null ? configEmail :
            // gitEmail;

            String comitterName = getUser().getName();
            String comitterEmail = getUser().getEmail();
            // End TODO

            commitCommand.setCommitter(comitterName, comitterEmail);
            commitCommand.setAuthor(comitterName, comitterEmail);
            commitCommand.setMessage(request.getMessage());
            commitCommand.setAll(request.isAll());
            commitCommand.setAmend(request.isAmend());

            RevCommit result = commitCommand.call();

            GitUser gitUser = createDto(GitUser.class).withName(comitterName).withEmail(comitterEmail);
            Revision revision = createDto(Revision.class).withBranch(getCurrentBranch())
                    .withId(result.getId().getName()).withMessage(result.getFullMessage())
                    .withCommitTime((long) result.getCommitTime() * 1000).withCommitter(gitUser);
            return revision;
        } catch (GitAPIException e) {
            throw new GitException(e);
        }
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#diff(org.exoplatform.ide.git.shared.DiffRequest) */
    @Override
    public DiffPage diff(DiffRequest request) throws GitException {
        return new JGitDiffPage(request, repository);
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#fetch(org.exoplatform.ide.git.shared.FetchRequest) */
    @Override
    public void fetch(FetchRequest request) throws GitException {
        String remote = request.getRemote();

        try {
            List<RefSpec> fetchRefSpecs = null;
            List<String> refSpec = request.getRefSpec();
            if (refSpec != null && refSpec.size() > 0) {
                fetchRefSpecs = new ArrayList<RefSpec>(refSpec.size());
                for (String refSpecItem : refSpec) {
                    RefSpec fetchRefSpec = (refSpecItem.indexOf(':') < 0) //
                            ? new RefSpec(Constants.R_HEADS + refSpecItem + ":") //
                            : new RefSpec(refSpecItem);
                    fetchRefSpecs.add(fetchRefSpec);
                }
            } else {
                // fetchRefSpecs = Arrays.asList(new RefSpec(Constants.HEAD));
                fetchRefSpecs = Collections.emptyList();
            }

            FetchCommand fetchCommand = getGit().fetch();

            // If this an unknown remote with no refspecs given, put HEAD
            // (otherwise JGit fails)
            if (remote != null && (refSpec == null || refSpec.isEmpty())) {
                boolean found = false;
                List<Remote> configRemotes = remoteList(createDto(RemoteListRequest.class));
                for (Remote configRemote : configRemotes) {
                    if (remote.equals(configRemote.getName())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    fetchRefSpecs = Arrays.asList(new RefSpec(Constants.HEAD + ":" + Constants.FETCH_HEAD));
                }
            }

            setRemoteCommandCredentials(request, remote, fetchCommand);

            if (remote != null) {
                fetchCommand.setRemote(remote);
            }
            if (fetchRefSpecs != null) {
                fetchCommand.setRefSpecs(fetchRefSpecs);
            }
            int timeout = request.getTimeout();
            if (timeout > 0) {
                fetchCommand.setTimeout(timeout);
            }
            fetchCommand.setRemoveDeletedRefs(request.isRemoveDeletedRefs());

            fetchCommand.call();
        } catch (GitAPIException e) {
            throw new GitException(e);
        } 
    }

    private void setRemoteCommandCredentials(GitRequest request, String remote, TransportCommand<?, ?> command)
            throws GitException {
        if (SshUtil.isSSH(remote)) {
            throw new GitException("Unsupported Operation - SSH");
        } else {
            // workaround for clone use-case remove when normal credential
            // storage is implemented
            boolean useRequestCredentials = false;
            String user = request.getAttributes().get("userName");
            String password = request.getAttributes().get("password");
            if (user != null && password != null) {
                useRequestCredentials = true;
            }
            try {
                if (useRequestCredentials) {
                    GitBasicAuthenticationCredentialsProvider.setCurrentCredentials(user, password);
                }
                UserCredential credentials = credentialsLoader.getUserCredential(remote);
                if (credentials != null) {
                    command.setCredentialsProvider(new UsernamePasswordCredentialsProvider(credentials.getUserName(),
                            credentials.getPassword()));
                }
            } finally {
                if (useRequestCredentials) {
                    GitBasicAuthenticationCredentialsProvider.clearCredentials();
                }
            }
        }
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#init(org.exoplatform.ide.git.shared.InitRequest) */
    @Override
    public void init(InitRequest request) throws GitException {
        File workDir = repository.getWorkTree();
        if (!workDir.exists()) {
            throw new GitException(Messages.getString("ERROR_INIT_FOLDER_MISSING", workDir));
        }

        boolean bare = request.isBare();

        try {
            repository.create(bare);

            if (!bare) {
                try {
                    Git git = getGit();
                    git.add().addFilepattern(".").call();
                    git.commit().setMessage("init").call();
                } catch (GitAPIException e) {
                    throw new GitException(e);
                }
            }
            GitUser gitUser = getUser();
            if (gitUser != null) {
                StoredConfig config = repository.getConfig();
                config.setString(ConfigConstants.CONFIG_USER_SECTION, null, ConfigConstants.CONFIG_KEY_NAME,
                        gitUser.getName());
                config.setString(ConfigConstants.CONFIG_USER_SECTION, null, ConfigConstants.CONFIG_KEY_EMAIL,
                        gitUser.getEmail());
                config.save();
            }
        } catch (IOException e) {
            throw new GitException(e.getMessage(), e);
        }
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#log(org.exoplatform.ide.git.shared.LogRequest) */
    @Override
    public LogPage log(LogRequest request) throws GitException {
        LogCommand logCommand = getGit().log();
        try {
            setRevisionRange(logCommand, request);

            Iterator<RevCommit> revIterator = logCommand.call().iterator();
            List<Revision> commits = new ArrayList<Revision>();

            while (revIterator.hasNext()) {
                RevCommit commit = revIterator.next();
                PersonIdent committerIdentity = commit.getCommitterIdent();
                GitUser gitUser = createDto(GitUser.class).withName(committerIdentity.getName())
                        .withEmail(committerIdentity.getEmailAddress());
                Revision revision = createDto(Revision.class).withId(commit.getId().getName())
                        .withMessage(commit.getFullMessage()).withCommitTime((long) commit.getCommitTime() * 1000)
                        .withCommitter(gitUser);
                commits.add(revision);
            }
            return new LogPage(commits);
        } catch (GitAPIException | IOException e) {
            throw new GitException(e);
        }
    }

    private void setRevisionRange(LogCommand logCommand, LogRequest request) throws IOException {
        // Revision Range
        if (request != null) {
            String revisionRangeSince = request.getRevisionRangeSince();
            String revisionRangeUntil = request.getRevisionRangeUntil();
            if (revisionRangeSince != null && revisionRangeUntil != null) {
                ObjectId since = repository.resolve(revisionRangeSince);
                ObjectId until = repository.resolve(revisionRangeUntil);
                logCommand.addRange(since, until);
            }
        }
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#log(org.exoplatform.ide.git.shared.LogRequest) */
    @Override
    public List<GitUser> getCommiters() throws GitException {
        List<GitUser> gitUsers = new ArrayList<GitUser>();
        try {
            LogCommand logCommand = getGit().log();
            Iterator<RevCommit> revIterator = logCommand.call().iterator();

            while (revIterator.hasNext()) {
                RevCommit commit = revIterator.next();
                PersonIdent committerIdentity = commit.getCommitterIdent();
                GitUser gitUser = createDto(GitUser.class).withName(committerIdentity.getName())
                        .withEmail(committerIdentity.getEmailAddress());
                if (!gitUsers.contains(gitUser)) {
                    gitUsers.add(gitUser);
                }
            }
        } catch (GitAPIException e) {
            throw new GitException(e);
        }

        return gitUsers;
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#merge(org.exoplatform.ide.git.shared.MergeRequest) */
    @Override
    public MergeResult merge(MergeRequest request) throws GitException {
        try {
            Ref ref = repository.getRef(request.getCommit());
            if (ref == null) {
                throw new IllegalArgumentException("Invalid reference to commit for merge " + request.getCommit());
            }
            // Shorten local branch names by removing '/refs/heads/' from the
            // beginning
            String name = ref.getName();
            if (name.startsWith(Constants.R_HEADS)) {
                name = name.substring(Constants.R_HEADS.length());
            }
            org.eclipse.jgit.api.MergeResult jgitMergeResult = getGit().merge().include(name, ref.getObjectId()).call();
            return new JGitMergeResult(jgitMergeResult);
        } catch (CheckoutConflictException e) {
            org.eclipse.jgit.api.MergeResult jgitMergeResult = new org.eclipse.jgit.api.MergeResult(
                    e.getConflictingPaths());
            return new JGitMergeResult(jgitMergeResult);
        } catch (IOException | GitAPIException e) {
            throw new GitException(e.getMessage(), e);
        }
    }

    @Override
    public RebaseResponse rebase(RebaseRequest request) throws GitException {
        RebaseResult result;
        try {
            // Create a jgit command
            RebaseCommand rebaseCommand = getGit().rebase();

            setRebaseOperation(rebaseCommand, request);

            String branch = request.getBranch();
            if (branch != null && !branch.isEmpty()) {
                rebaseCommand.setUpstream(branch);
            }
            result = rebaseCommand.call();
        } catch (GitAPIException e) {
            throw new GitException(e.getMessage(), e);
        }
        return new JGitRebaseResponse(result);
    }

    private void setRebaseOperation(RebaseCommand rebaseCommand, RebaseRequest request) {
        RebaseCommand.Operation op = RebaseCommand.Operation.BEGIN;

        // If other operation other than 'BEGIN' was specified, set it
        if (request.getOperation() != null) {
            switch (request.getOperation()) {
            case REBASE_OPERATION_ABORT:
                op = RebaseCommand.Operation.ABORT;
                break;
            case REBASE_OPERATION_CONTINUE:
                op = RebaseCommand.Operation.CONTINUE;
                break;
            case REBASE_OPERATION_SKIP:
                op = RebaseCommand.Operation.SKIP;
                break;
            default:
                op = RebaseCommand.Operation.BEGIN;
                break;
            }
        }
        rebaseCommand.setOperation(op);
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#mv(org.exoplatform.ide.git.shared.MoveRequest) */
    @Override
    public void mv(MoveRequest request) throws GitException {
        throw new RuntimeException("Not implemented yet. ");
    }

    /**
     * @return
     * @see org.exoplatform.ide.git.server.GitConnection#pull(org.exoplatform.ide.git.shared.PullRequest)
     */
    @Override
    public PullResponse pull(PullRequest request) throws GitException {
        String remote = request.getRemote();
        try {
            if (repository.getRepositoryState().equals(RepositoryState.MERGING)) {
                throw new GitException(Messages.getString("ERROR_PULL_MERGING"));
            }
            String fullBranch = repository.getFullBranch();
            if (!fullBranch.startsWith(Constants.R_HEADS)) {
                throw new DetachedHeadException(Messages.getString("ERROR_PULL_HEAD_DETACHED"));
            }

            String branch = fullBranch.substring(Constants.R_HEADS.length());

            StoredConfig config = repository.getConfig();
            if (remote == null) {
                remote = config.getString(ConfigConstants.CONFIG_BRANCH_SECTION, branch,
                        ConfigConstants.CONFIG_KEY_REMOTE);
                if (remote == null) {
                    remote = Constants.DEFAULT_REMOTE_NAME;
                }
            }

            String remoteBranch = null;
            RefSpec fetchRefSpecs = null;
            String refSpec = request.getRefSpec();
            if (refSpec != null) {
                fetchRefSpecs = (refSpec.indexOf(':') < 0) //
                        ? new RefSpec(Constants.R_HEADS + refSpec + ":" + fullBranch) //
                        : new RefSpec(refSpec);
                remoteBranch = fetchRefSpecs.getSource();
            } else {
                remoteBranch = config.getString(ConfigConstants.CONFIG_BRANCH_SECTION, branch,
                        ConfigConstants.CONFIG_KEY_MERGE);
            }

            if (remoteBranch == null) {
                String key = ConfigConstants.CONFIG_BRANCH_SECTION + "." + branch + "."
                        + ConfigConstants.CONFIG_KEY_MERGE;
                throw new GitException(Messages.getString("ERROR_PULL_REMOTE_BRANCH_MISSING", key));
            }

            FetchCommand fetchCommand = getGit().fetch();
            fetchCommand.setRemote(remote);
            if (fetchRefSpecs != null) {
                fetchCommand.setRefSpecs(fetchRefSpecs);
            }
            int timeout = request.getTimeout();
            if (timeout > 0) {
                fetchCommand.setTimeout(timeout);
            }

            setRemoteCommandCredentials(request, remote, fetchCommand);

            FetchResult fetchResult = fetchCommand.call();

            Ref remoteBranchRef = fetchResult.getAdvertisedRef(remoteBranch);
            if (remoteBranchRef == null) {
                remoteBranchRef = fetchResult.getAdvertisedRef(Constants.R_HEADS + remoteBranch);
            }
            if (remoteBranchRef == null) {
                throw new GitException(Messages.getString("ERROR_PULL_REF_MISSING", remoteBranch));
            }
            org.eclipse.jgit.api.MergeResult res = getGit().merge().include(remoteBranchRef).call();
            if (res.getMergeStatus().equals(org.eclipse.jgit.api.MergeResult.MergeStatus.ALREADY_UP_TO_DATE)) {
                throw new GitException(res.getMergeStatus().toString());
            }

            if (res.getConflicts() != null) {
                StringBuilder message = new StringBuilder(Messages.getString("ERROR_PULL_MERGE_CONFLICT_IN_FILES"));
                message.append("\n");
                Map<String, int[][]> allConflicts = res.getConflicts();
                for (String path : allConflicts.keySet()) {
                    message.append(path + "\n");
                }
                message.append(Messages.getString("ERROR_PULL_AUTO_MERGE_FAILED"));
                throw new GitException(message.toString());
            }
        } catch (CheckoutConflictException e) {
            StringBuilder message = new StringBuilder(Messages.getString("ERROR_CHECKOUT_CONFLICT"));
            message.append("\n");
            for (String path : e.getConflictingPaths()) {
                message.append(path + "\n");
            }
            message.append(Messages.getString("ERROR_PULL_COMMIT_BEFORE_MERGE"));
            throw new GitException(message.toString());
        } catch (IOException | GitAPIException e) {
            throw new GitException(e.getMessage(), e);
        } 
        return DtoFactory.getInstance().createDto(PullResponse.class);
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#push(org.exoplatform.ide.git.shared.PushRequest) */
    @Override
    public PushResponse push(PushRequest request) throws GitException {
        StringBuilder message = new StringBuilder();
        String remote = request.getRemote();

        try {
            PushCommand pushCommand = getGit().push();

            if (request.getRemote() != null) {
                pushCommand.setRemote(remote);
            }
            List<String> refSpec = request.getRefSpec();
            if (refSpec != null && refSpec.size() > 0) {
                List<RefSpec> refSpecInst = new ArrayList<RefSpec>(refSpec.size());
                for (String refSpecItem : refSpec) {
                    refSpecInst.add(new RefSpec(refSpecItem));
                }
                pushCommand.setRefSpecs(refSpecInst);
            }

            pushCommand.setForce(request.isForce());

            int timeout = request.getTimeout();
            if (timeout > 0) {
                pushCommand.setTimeout(timeout);
            }

            setRemoteCommandCredentials(request, remote, pushCommand);

            Iterable<PushResult> list = pushCommand.call();
            for (PushResult pushResult : list) {
                Collection<RemoteRefUpdate> refUpdates = pushResult.getRemoteUpdates();
                for (RemoteRefUpdate remoteRefUpdate : refUpdates) {
                    if (!remoteRefUpdate.getStatus().equals(org.eclipse.jgit.transport.RemoteRefUpdate.Status.OK)) {

                        if (remoteRefUpdate.getStatus()
                                .equals(org.eclipse.jgit.transport.RemoteRefUpdate.Status.UP_TO_DATE)) {
                            message.append(Messages.getString("INFO_PUSH_ATTEMPT_IGNORED_UP_TO_DATE"));
                        } else {
                            String refspec = getCurrentBranch() + " -> " + request.getRefSpec().get(0).split(":")[1];
                            message.append(
                                    Messages.getString("ERROR_PUSH_ATTEMPT_FAILED_1", refspec, request.getRemote()));
                            message.append("\n");
                            message.append(Messages.getString("ERROR_PUSH_ATTEMPT_FAILED_2",
                                    remoteRefUpdate.getStatus(), remoteRefUpdate.getMessage()));
                            message.append("\n");
                        }
                        throw new GitException(message.toString());
                    }
                }
            }
        } catch (GitAPIException e) {
            throw new GitException(e.getMessage(), e);
        } 
        return DtoFactory.getInstance().createDto(PushResponse.class);
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#remoteAdd(org.exoplatform.ide.git.shared.RemoteAddRequest) */
    @Override
    public void remoteAdd(RemoteAddRequest request) throws GitException {
        String remoteName = request.getName();
        if (remoteName == null || remoteName.length() == 0) {
            throw new IllegalArgumentException(Messages.getString("ERROR_ADD_REMOTE_NAME_MISSING"));
        }

        StoredConfig config = repository.getConfig();
        Set<String> remoteNames = config.getSubsections("remote");
        if (remoteNames.contains(remoteName)) {
            throw new IllegalArgumentException(Messages.getString("ERROR_REMOTE_NAME_ALREADY_EXISTS", remoteName));
        }

        String url = request.getUrl();
        if (url == null || url.length() == 0) {
            throw new IllegalArgumentException(Messages.getString("ERROR_REMOTE_URL_MISSING"));
        }

        RemoteConfig remoteConfig;
        try {
            remoteConfig = new RemoteConfig(config, remoteName);
        } catch (URISyntaxException e) {
            // Not happen since it is newly created remote.
            throw new GitException(e.getMessage(), e);
        }

        try {
            remoteConfig.addURI(new URIish(url));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Remote url " + url + " is invalid. ");
        }

        List<String> branches = request.getBranches();
        if (branches != null) {
            for (String branch : branches) {
                remoteConfig.addFetchRefSpec( //
                        new RefSpec(Constants.R_HEADS + branch + ":" + Constants.R_REMOTES + remoteName + "/" + branch)
                                .setForceUpdate(true));
            }
        } else {
            remoteConfig.addFetchRefSpec(
                    new RefSpec(Constants.R_HEADS + "*" + ":" + Constants.R_REMOTES + remoteName + "/*")
                            .setForceUpdate(true));
        }

        remoteConfig.update(config);

        try {
            config.save();
        } catch (IOException e) {
            throw new GitException(e.getMessage(), e);
        }
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#remoteDelete(java.lang.String) */
    @Override
    public void remoteDelete(String name) throws GitException {
        StoredConfig config = repository.getConfig();
        Set<String> remoteNames = config.getSubsections(ConfigConstants.CONFIG_KEY_REMOTE);
        if (!remoteNames.contains(name)) {
            throw new GitException(Messages.getString("ERROR_DELETE_REMOTE_NAME_MISSING"));
        }

        config.unsetSection(ConfigConstants.CONFIG_REMOTE_SECTION, name);
        Set<String> branches = config.getSubsections(ConfigConstants.CONFIG_BRANCH_SECTION);

        for (String branch : branches) {
            String r = config.getString(ConfigConstants.CONFIG_BRANCH_SECTION, branch,
                    ConfigConstants.CONFIG_KEY_REMOTE);
            if (name.equals(r)) {
                config.unset(ConfigConstants.CONFIG_BRANCH_SECTION, branch, ConfigConstants.CONFIG_KEY_REMOTE);
                config.unset(ConfigConstants.CONFIG_BRANCH_SECTION, branch, ConfigConstants.CONFIG_KEY_MERGE);
                List<Branch> remoteBranches = branchList(createDto(BranchListRequest.class).withListMode("r"));
                for (Branch remoteBranch : remoteBranches) {
                    if (remoteBranch.getDisplayName().startsWith(name)) {
                        branchDelete(
                                createDto(BranchDeleteRequest.class).withName(remoteBranch.getName()).withForce(true));
                    }
                }
            }
        }

        try {
            config.save();
        } catch (IOException e) {
            throw new GitException(e.getMessage(), e);
        }
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#remoteList(org.exoplatform.ide.git.shared.RemoteListRequest) */
    @Override
    public List<Remote> remoteList(RemoteListRequest request) throws GitException {
        StoredConfig config = repository.getConfig();
        Set<String> remoteNames = new HashSet<String>(config.getSubsections(ConfigConstants.CONFIG_KEY_REMOTE));
        String remote = request.getRemote();

        if (remote != null && remoteNames.contains(remote)) {
            remoteNames.clear();
            remoteNames.add(remote);
        }

        List<Remote> result = new ArrayList<Remote>(remoteNames.size());
        for (String rn : remoteNames) {
            try {
                List<URIish> uris = new RemoteConfig(config, rn).getURIs();
                result.add(
                        createDto(Remote.class).withName(rn).withUrl(uris.isEmpty() ? null : uris.get(0).toString()));
            } catch (URISyntaxException e) {
                throw new GitException(e.getMessage(), e);
            }
        }
        return result;
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#remoteUpdate(org.exoplatform.ide.git.shared.RemoteUpdateRequest) */
    @Override
    public void remoteUpdate(RemoteUpdateRequest request) throws GitException {
        String remoteName = request.getName();
        if (remoteName == null || remoteName.length() == 0) {
            throw new IllegalArgumentException(Messages.getString("ERROR_UPDATE_REMOTE_NAME_MISSING"));
        }

        StoredConfig config = repository.getConfig();
        Set<String> remoteNames = config.getSubsections(ConfigConstants.CONFIG_KEY_REMOTE);
        if (!remoteNames.contains(remoteName)) {
            throw new IllegalArgumentException("Remote " + remoteName + " not found. ");
        }

        RemoteConfig remoteConfig;
        try {
            remoteConfig = new RemoteConfig(config, remoteName);
        } catch (URISyntaxException e) {
            throw new GitException(e.getMessage(), e);
        }

        List<String> tmp;

        tmp = request.getBranches();
        if (tmp != null && tmp.size() > 0) {
            if (!request.isAddBranches()) {
                remoteConfig.setFetchRefSpecs(new ArrayList<RefSpec>());
                remoteConfig.setPushRefSpecs(new ArrayList<RefSpec>());
            } else {
                // Replace wildcard refspec if any.
                remoteConfig.removeFetchRefSpec(
                        new RefSpec(Constants.R_HEADS + "*" + ":" + Constants.R_REMOTES + remoteName + "/*")
                                .setForceUpdate(true));
                remoteConfig.removeFetchRefSpec(
                        new RefSpec(Constants.R_HEADS + "*" + ":" + Constants.R_REMOTES + remoteName + "/*"));
            }

            // Add new refspec.
            for (String branch : tmp) {
                remoteConfig.addFetchRefSpec(
                        new RefSpec(Constants.R_HEADS + branch + ":" + Constants.R_REMOTES + remoteName + "/" + branch)
                                .setForceUpdate(true));
            }
        }

        // Remove URLs first.
        tmp = request.getRemoveUrl();
        if (tmp != null) {
            for (String url : tmp) {
                try {
                    remoteConfig.removeURI(new URIish(url));
                } catch (URISyntaxException e) {
                    LOG.debug(Messages.getString("ERROR_REMOVING_INVALID_URL"));
                }
            }
        }

        // Add new URLs.
        tmp = request.getAddUrl();
        if (tmp != null) {
            for (String url : tmp) {
                try {
                    remoteConfig.addURI(new URIish(url));
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException("Remote url " + url + " is invalid. ");
                }
            }
        }

        // Remove URLs for pushing.
        tmp = request.getRemovePushUrl();
        if (tmp != null) {
            for (String url : tmp) {
                try {
                    remoteConfig.removePushURI(new URIish(url));
                } catch (URISyntaxException e) {
                    LOG.debug(Messages.getString("ERROR_REMOVING_INVALID_URL"));
                }
            }
        }

        // Add URLs for pushing.
        tmp = request.getAddPushUrl();
        if (tmp != null) {
            for (String url : tmp) {
                try {
                    remoteConfig.addPushURI(new URIish(url));
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException("Remote push url " + url + " is invalid. ");
                }
            }
        }

        remoteConfig.update(config);

        try {
            config.save();
        } catch (IOException e) {
            throw new GitException(e.getMessage(), e);
        }
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#reset(org.exoplatform.ide.git.shared.ResetRequest) */
    @Override
    public void reset(ResetRequest request) throws GitException {
        try {

            ResetCommand req = getGit().reset();
            req.setRef(request.getCommit());
            for (int i = 0; i < request.getFilePattern().size(); i++) {
                req.addPath(request.getFilePattern().get(i));
            }

            if (request.getType() != null) {
                if (request.getType().equals(ResetRequest.ResetType.HARD)) {
                    req.setMode(ResetCommand.ResetType.HARD);
                } else if (request.getType().equals(ResetRequest.ResetType.KEEP)) {
                    req.setMode(ResetCommand.ResetType.KEEP);
                } else if (request.getType().equals(ResetRequest.ResetType.MERGE)) {
                    req.setMode(ResetCommand.ResetType.MERGE);
                } else if (request.getType().equals(ResetRequest.ResetType.MIXED)) {
                    req.setMode(ResetCommand.ResetType.MIXED);
                } else if (request.getType().equals(ResetRequest.ResetType.SOFT)) {
                    req.setMode(ResetCommand.ResetType.SOFT);
                }
            }

            req.call();
        } catch (GitAPIException e) {
            throw new GitException(e);
        }
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#rm(org.exoplatform.ide.git.shared.RmRequest) */
    @Override
    public void rm(RmRequest request) throws GitException {
        List<String> files = request.getItems();
        RmCommand rmCommand = getGit().rm();

        rmCommand.setCached(request.isCached());

        if (files != null) {
            for (String file : files) {
                rmCommand.addFilepattern(file);
            }
        }
        try {
            rmCommand.call();
        } catch (GitAPIException e) {
            throw new GitException(e);
        }
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#status(org.exoplatform.ide.git.shared.StatusRequest) */
    @Override
    public Status status(StatusFormat format) throws GitException {
        if (!RepositoryCache.FileKey.isGitRepository(getGit().getRepository().getDirectory(), FS.DETECTED)) {
            throw new GitException(Messages.getString("ERROR_STATUS_NOT_GIT_REPO"));
        }

        org.eclipse.jgit.api.Status gitStatus;
        try {
            gitStatus = getGit().status().call();
        } catch (NoWorkTreeException | GitAPIException e) {
            throw new GitException(e.getMessage(), e);
        }

        List<String> untrackedFolders = new ArrayList<String>(gitStatus.getUntrackedFolders());
        List<String> untrackedFiles = new ArrayList<String>(gitStatus.getUntracked());

        // The Che result
        String currentBranch = getCurrentBranch();
        Status cheStatus = createDto(org.eclipse.che.api.git.shared.Status.class);
        cheStatus.setRepositoryState(repository.getRepositoryState().name());
        cheStatus.setAdded(new ArrayList<String>(gitStatus.getAdded()));
        cheStatus.setBranchName(currentBranch);
        cheStatus.setChanged(new ArrayList<String>(gitStatus.getChanged()));
        cheStatus.setClean(gitStatus.isClean());
        cheStatus.setConflicting(new ArrayList<String>(gitStatus.getConflicting()));
        cheStatus.setFormat(format);
        cheStatus.setMissing(new ArrayList<String>(gitStatus.getMissing()));
        cheStatus.setModified(new ArrayList<String>(gitStatus.getModified()));
        cheStatus.setRemoved(new ArrayList<String>(gitStatus.getRemoved()));
        cheStatus.setUntracked(untrackedFiles);
        cheStatus.setUntrackedFolders(untrackedFolders);
        return cheStatus;
    }

    static <T extends Comparable<T>> List<T> asSortedList(Set<T> set) {
        List<T> list = new ArrayList<T>(set);
        Collections.sort(list);
        return list;
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#tagCreate(org.exoplatform.ide.git.shared.TagCreateRequest) */
    @Override
    public Tag tagCreate(TagCreateRequest request) throws GitException {
        String commit = request.getCommit();
        if (commit == null) {
            commit = Constants.HEAD;
        }

        try {
            RevWalk revWalk = new RevWalk(repository);
            RevObject revObject;
            try {
                revObject = revWalk.parseAny(repository.resolve(commit));
            } finally {
                revWalk.close();
            }

            TagCommand tagCommand = getGit().tag().setName(request.getName()).setObjectId(revObject)
                    .setMessage(request.getMessage()).setForceUpdate(request.isForce());

            GitUser tagger = getUser();
            if (tagger != null) {
                tagCommand.setTagger(new PersonIdent(tagger.getName(), tagger.getEmail()));
            }

            Ref revTagRef = tagCommand.call();
            RevTag revTag = revWalk.parseTag(revTagRef.getLeaf().getObjectId());
            return createDto(Tag.class).withName(revTag.getTagName());
        } catch (IOException | GitAPIException e) {
            throw new GitException(e.getMessage(), e);
        }
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#tagDelete(org.exoplatform.ide.git.shared.TagDeleteRequest) */
    @Override
    public void tagDelete(TagDeleteRequest request) throws GitException {
        try {
            String tagName = request.getName();
            Ref tagRef = repository.getRef(tagName);
            if (tagRef == null) {
                throw new IllegalArgumentException("Tag " + tagName + " not found. ");
            }

            RefUpdate updateRef = repository.updateRef(tagRef.getName());
            updateRef.setRefLogMessage("tag deleted", false);
            updateRef.setForceUpdate(true);
            Result deleteResult;
            deleteResult = updateRef.delete();
            if (deleteResult != Result.FORCED && deleteResult != Result.FAST_FORWARD) {
                throw new GitException(Messages.getString("ERROR_TAG_DELETE", tagName, deleteResult));
            }
        } catch (IOException e) {
            throw new GitException(e.getMessage(), e);
        }
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#tagList(org.exoplatform.ide.git.shared.TagListRequest) */
    @Override
    public List<Tag> tagList(TagListRequest request) throws GitException {
        String patternStr = request.getPattern();
        Pattern pattern = null;
        if (patternStr != null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < patternStr.length(); i++) {
                char c = patternStr.charAt(i);
                if (c == '*' || c == '?') {
                    sb.append('.');
                } else if (c == '.' || c == '(' || c == ')' || c == '[' || c == ']' || c == '^' || c == '$'
                        || c == '|') {
                    sb.append('\\');
                }
                sb.append(c);
            }
            pattern = Pattern.compile(sb.toString());
        }

        Set<String> tagNames = repository.getTags().keySet();
        List<Tag> tags = new ArrayList<Tag>(tagNames.size());

        for (String tagName : tagNames) {
            if (pattern == null || pattern.matcher(tagName).matches()) {
                tags.add(createDto(Tag.class).withName(tagName));
            }
        }
        return tags;
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#getUser() */
    public GitUser getUser() {
        return user;
    }

    /** @see org.exoplatform.ide.git.server.GitConnection#close() */
    @Override
    public void close() {
        repository.close();
    }

    public Repository getRepository() {
        return repository;
    }

    public String getCurrentBranch() throws GitException {
        try {
            Ref headRef;
            headRef = repository.getRef(Constants.HEAD);
            return Repository.shortenRefName(headRef.getLeaf().getName());
        } catch (IOException e) {
            throw new GitException(e.getMessage(), e);
        }
    }

    /**
     * Method for cleaning name of remote branch to be checked out. I.e. it
     * takes something like "origin/testBranch" and returns "testBranch". This
     * is needed for view-compatibility with console Git client.
     * 
     * @param branchName
     *            is a name of branch to be cleaned
     * @return branchName without remote repository name
     * @throws GitException
     */
    private String cleanRemoteName(String branchName) throws GitException {
        String returnName = branchName;
        List<Remote> remotes = this.remoteList(createDto(RemoteListRequest.class));
        for (Remote remote : remotes) {
            if (branchName.startsWith(remote.getName())) {
                returnName = branchName.replaceFirst(remote.getName() + "/", "");
            }
        }
        return returnName;
    }

    @Override
    public File getWorkingDir() {
        return repository.getWorkTree();
    }

    @Override
    public List<RemoteReference> lsRemote(LsRemoteRequest request) throws UnauthorizedException, GitException {
        LsRemoteCommand jgitCommand = getGit().lsRemote();
        jgitCommand.setRemote(request.getRemoteUrl());
        // TODO handle cases of isUseAuthorization()
        Collection<Ref> refs;
        try {
            refs = jgitCommand.call();
        } catch (GitAPIException e) {
            throw new GitException(e.getMessage(), e);
        }
        // Translate the JGit result
        List<RemoteReference> remoteRefs = new ArrayList<RemoteReference>(refs.size());
        for (Ref ref : refs) {
            String commitId = ref.getObjectId().name();
            String name = ref.getName();
            RemoteReference remoteRef = createDto(RemoteReference.class).withCommitId(commitId).withReferenceName(name);
            remoteRefs.add(remoteRef);
        }
        return remoteRefs;
    }

    @Override
    public Config getConfig() throws GitException {
        if (_config != null) {
            return _config;
        }
        return _config = new JGitConfigImpl(repository);
    }

    @Override
    public void setOutputLineConsumerFactory(LineConsumerFactory outputPublisherFactory) {
        // XXX nothing to do, not outputs are produced by JGit
    }

    private Git getGit() {
        if (_git != null) {
            return _git;
        }
        return _git = new Git(repository);
    }

    private static <T> T createDto(Class<T> clazz) {
        return DtoFactory.getInstance().createDto(clazz);
    }

    @Override
    public List<String> listFiles(LsFilesRequest request) throws GitException {
        return null;
    }

}
