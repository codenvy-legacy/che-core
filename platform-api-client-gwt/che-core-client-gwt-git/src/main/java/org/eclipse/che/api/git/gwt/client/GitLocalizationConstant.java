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
package org.eclipse.che.api.git.gwt.client;

import com.google.gwt.i18n.client.Messages;
import com.google.gwt.safehtml.shared.SafeHtml;

/**
 * @author <a href="mailto:evidolob@exoplatform.com">Evgen Vidolob</a>
 */
public interface GitLocalizationConstant extends Messages {
    // BUTTONS
    @Key("button.add")
    String buttonAdd();

    @Key("button.cancel")
    String buttonCancel();

    @Key("button.create")
    String buttonCreate();

    @Key("button.checkout")
    String buttonCheckout();

    @Key("button.delete")
    String buttonDelete();

    @Key("button.rename")
    String buttonRename();

    @Key("button.close")
    String buttonClose();

    @Key("button.commit")
    String buttonCommit();

    @Key("button.reset")
    String buttonReset();

    @Key("button.remove")
    String buttonRemove();

    @Key("button.fetch")
    String buttonFetch();

    @Key("button.ok")
    String buttonOk();

    @Key("button.push")
    String buttonPush();

    @Key("button.pull")
    String buttonPull();

    @Key("button.merge")
    String buttonMerge();

    // MESSAGES
    @Key("messages.unableGetSshKey")
    String messagesUnableGetSshKey();

    @Key("messages.warningTitle")
    String messagesWarningTitle();

    @Key("messages.index_empty")
    String indexIsEmpty();

    @Key("messages.add_success")
    String addSuccess();

    @Key("messages.nothingAddToIndex")
    String nothingAddToIndex();

    @Key("messages.add_failed")
    String addFailed();

    @Key("messages.branches_list_failed")
    String branchesListFailed();

    @Key("messages.local_branches_list_failed")
    String localBranchesListFailed();

    @Key("messages.remote_branches_list_failed")
    String remoteBranchesListFailed();

    @Key("messages.get_config_failed")
    String failedGettingConfig();

    @Key("messages.branch_create_failed")
    String branchCreateFailed();

    @Key("messages.branch_checkout_failed")
    String branchCheckoutFailed();

    @Key("messages.branch_delete_failed")
    String branchDeleteFailed();

    @Key("messages.branch_rename_failed")
    String branchRenameFailed();

    @Key("messages.commit_failed")
    String commitFailed();

    @Key("messages.diff.failed")
    String diffFailed();

    @Key("messages.log_failed")
    String logFailed();

    @Key("messages.init_success")
    String initSuccess();

    @Key("messages.initRepoQuestion")
    String messagesInitRepoQuestion(String name);

    @Key("messages.init_failed")
    String initFailed();

    @Key("messages.push_success")
    String pushSuccess(String remote);

    @Key("messages.push_fail")
    String pushFail();

    @Key("messages.pull_success")
    String pullSuccess(String remoteUrl);

    @Key("messages.pull_fail")
    String pullFail(String remoteUrl);

    @Key("messages.fetch_success")
    String fetchSuccess(String remoteUrl);

    @Key("messages.fetch_fail")
    String fetchFail(String remoteUrl);

    @Key("messages.remote_list_failed")
    String remoteListFailed();

    @Key("messages.remove_files_success")
    String removeFilesSuccessfull();

    @Key("messages.remove_files_failed")
    String removeFilesFailed();

    @Key("messages.remote_add_failed")
    String remoteAddFailed();

    @Key("messages.remote_delete_failed")
    String remoteDeleteFailed();

    @Key("messages.reset_files_failed")
    String resetFilesFailed();

    @Key("messages.reset_files_successfully")
    String resetFilesSuccessfully();

    @Key("messages.nothing_to_reset")
    String nothingToReset();

    @Key("messages.reset_successfully")
    String resetSuccessfully();

    @Key("messages.reset_fail")
    String resetFail();

    @Key("messages.status_failed")
    String statusFailed();

    @Key("messages.selected_remote_fail")
    String selectRemoteRepositoryFail();

    @Key("messages.delete_remote_repository.title")
    String deleteRemoteRepositoryTitle();

    @Key("messages.delete_remote_repository.question")
    String deleteRemoteRepositoryQuestion(String remote);

    @Key("messages.delete_repository.question")
    String deleteGitRepositoryQuestion(String repository);

    @Key("messages.delete_repository.title")
    String deleteGitRepositoryTitle();

    @Key("messages.delete_success")
    String deleteGitRepositorySuccess();

    @Key("messages.notAuthorized")
    String messagesNotAuthorized();

    @Key("importProject.messageStartWithWhiteSpace")
    String importProjectMessageStartWithWhiteSpace();

    @Key("importProject.messageNameRepoIncorrect")
    String importProjectMessageNameRepoIncorrect();

    @Key("importProject.messageProtocolIncorrect")
    String importProjectMessageProtocolIncorrect();

    @Key("importProject.messageHostIncorrect")
    String importProjectMessageHostIncorrect();

    // ----InitRequestHandler
    @Key("init.started")
    String initStarted(String repo);

    @Key("init.finished")
    String initFinished(String repo);

    // ----CloneRequestHandler-------------
    @Key("clone.started")
    String cloneStarted(String projectPath, String remoteName);

    @Key("clone.finished")
    String cloneFinished(String projectName, String remoteUri);

    // ----PushRequestHandler
    @Key("push.started")
    String pushStarted(String projectName, String localBranch, String remoteBranch);

    @Key("push.finished")
    String pushFinished(String projectName, String localBranch, String remoteBranch);

    // ----PullRequestHandler
    @Key("pull.started")
    String pullStarted(String projectName, String remoteBranch, String localBranch);

    @Key("pull.finished")
    String pullFinished(String projectName, String remoteBranch, String localBranch);

    // ----FetchRequestHandler
    @Key("fetch.started")
    String fetchStarted(String projectName, String remoteBranch, String localBranch);

    @Key("fetch.finished")
    String fetchFinished(String projectName, String remoteBranch, String localBranch);

    // ----AddRequestHandler
    @Key("add.started")
    String addStarted(String projectName);

    @Key("add.finished")
    String addFinished(String projectName);

    // ----CommitRequestHandler
    @Key("commit.started")
    String commitStarted(String projectName, String comment);

    @Key("commit.finished")
    String commitFinished(String projectName, String comment);

    // ----VIEWS------------------------------------------------------------------

    // GitImporterPage
    @Key("view.import.gitImporterPage.importerInfo")
    String gitImporterPageImporterInfo();

    @Key("view.import.gitImporterPage.projectUrl")
    String gitImporterPageProjectUrl();

    @Key("view.import.gitImporterPage.projectInfo")
    String gitImporterPageProjectInfo();

    @Key("view.import.gitImporterPage.projectName")
    String gitImporterPageProjectName();

    @Key("view.import.gitImporterPageProjectNamePrompt")
    String gitImporterPageProjectNamePrompt();

    @Key("view.import.gitImporterPage.projectDescription")
    String gitImporterPageProjectDescription();

    @Key("view.import.gitImporterPage.projectDescriptionPrompt")
    String gitImporterPageProjectDescriptionPrompt();

    @Key("view.import.gitImporterPage.projectPrivacy")
    String gitImporterPageProjectPrivacy();

    @Key("view.import.gitImporterPage.projectVisibilityPublic")
    String gitImporterPageprojectVisibilityPublic();

    @Key("view.import.gitImporterPage.projectVisibilityPrivate")
    String gitImporterPageprojectVisibilityPrivate();

    // Add
    @Key("view.add_to_index.all_changes")
    String addToIndexAllChanges();

    @Key("view.add_to_index.multiple")
    String addToIndexMultiple();

    @Key("view.add_to_index.folder")
    SafeHtml addToIndexFolder(String folder);

    @Key("view.add_to_index.file")
    SafeHtml addToIndexFile(String file);

    @Key("view.add_to_index.update_field_title")
    String addToIndexUpdateFieldTitle();

    @Key("view.add_to_index.title")
    String addToIndexTitle();

    // Branch
    @Key("view.branch.create_new")
    String branchCreateNew();

    @Key("view.branch.type_new")
    String branchTypeNew();

    @Key("view.branch.title_rename")
    String branchTitleRename();

    @Key("view.branch.type_rename")
    String branchTypeRename();

    @Key("view.branch.delete")
    String branchDelete();

    @Key("view.branch.delete_ask")
    String branchDeleteAsk(String name);

    @Key("view.branch.title")
    String branchTitle();

    // Commit
    @Key("view.commit.commit_message")
    String commitMessage(String revision, String time);

    @Key("view.commit.commit_user")
    String commitUser(String user);

    @Key("view.commit.title")
    String commitTitle();

    @Key("view.commit.message_field_title")
    String commitMessageFieldTitle();

    @Key("view.commit.all_field_title")
    String commitAllFieldTitle();

    @Key("view.commit.selection_field_title")
    String commitSelectionFieldTitle();

    @Key("view.commit.onlyselection_field_title")
    String commitOnlySelectionFieldTitle();

    @Key("view.commit.amend_field_title")
    String commitAmendFieldTitle();

    @Key("view.commit.grid.date")
    String commitGridDate();

    @Key("view.commit.grid.commiter")
    String commitGridCommiter();

    @Key("view.commit.grid.comment")
    String commitGridComment();

    @Key("view.push.title")
    String pushViewTitle();

    @Key("view.push.remote.field")
    String pushViewRemoteFieldTitle();

    @Key("view.push.local_branch.field")
    String pushViewLocalBranchFieldTitle();

    @Key("view.push.remote_branch.field")
    String pushViewRemoteBranchFieldTitle();

    // Reset
    @Key("view.reset.files.title")
    String resetFilesViewTitle();

    @Key("view.reset.commit.title")
    String resetCommitViewTitle();

    @Key("view.reset.soft.type.title")
    String resetSoftTypeTitle();

    @Key("view.reset.soft.type.description")
    String resetSoftTypeDescription();

    @Key("view.reset.mixed.type.title")
    String resetMixedTypeTitle();

    @Key("view.reset.mixed.type.description")
    String resetMixedTypeDescription();

    @Key("view.reset.hard.type.title")
    String resetHardTypeTitle();

    @Key("view.reset.hard.type.description")
    String resetHardTypeDescription();

    @Key("view.reset.keep.type.title")
    String resetKeepTypeTitle();

    @Key("view.reset.merge.type.title")
    String resetMergeTypeTitle();

    // Remove
    @Key("view.remove_from_index.all")
    String removeFromIndexAll();

    @Key("view.remove_from_index.only")
    String removeFromIndexOnly();

    @Key("view.remove_from_index.folder")
    SafeHtml removeFromIndexFolder(String folder);

    @Key("view.remove_from_index.file")
    SafeHtml removeFromIndexFile(String file);

    @Key("view.remove_from_index.title")
    String removeFromIndexTitle();

    // Create
    @Key("view.create.title")
    String createTitle();

    // Fetch
    @Key("view.fetch.title")
    String fetchTitle();

    @Key("view.fetch.remote.field.title")
    String fetchRemoteFieldTitle();

    @Key("view.fetch.remote.branches.title")
    String fetchRemoteBranchesTitle();

    @Key("view.fetch.local.branches.title")
    String fetchLocalBranchesTitle();

    @Key("view.fetch.remove.deleted.refs.title")
    String fetchRemoveDeletedRefsTitle();

    @Key("view.fetch.all.branches.field.title")
    String fetchAllBranchesTitle();

    // Remote
    @Key("view.remotes.title")
    String remotesViewTitle();

    @Key("view.remote.name.field")
    String remoteNameField();

    @Key("view.remote.location.field")
    String remoteLocationField();

    @Key("view.remote.grid.name.field")
    String remoteGridNameField();

    @Key("view.remote.grid.location.field")
    String remoteGridLocationField();

    // History
    @Key("view.history.diff.index.state")
    String historyDiffIndexState();

    @Key("view.history.diff.tree.state")
    String historyDiffTreeState();

    @Key("view.history.nothing.to.display")
    String historyNothingToDisplay();

    @Key("view.history.title")
    String historyTitle();

    @Key("view.history.project.changes.button.title")
    String historyProjectChangesButtonTitle();

    @Key("view.history.resource.changes.button.title")
    String historyResourceChangesButtonTitle();

    @Key("view.history.diff.with.index.button.title")
    String historyDiffWithIndexButtonTitle();

    @Key("view.history.diff.with.work.tree.button.title")
    String historyDiffWithWorkTreeButtonTitle();

    @Key("view.history.diff.with.prev.version.button.title")
    String historyDiffWithPrevVersionButtonTitle();

    @Key("view.history.revisionA.title")
    String historyViewRevisionATitle();

    @Key("view.history.revisionB.title")
    String historyViewRevisionBTitle();

    @Key("view.history.date.title")
    String historyViewDateTitle();

    @Key("view.history.refresh.button.title")
    String refreshRevisionListButtonTitle();

    // Pull
    @Key("view.pull.title")
    String pullTitle();

    @Key("view.pull.remote.branches.title")
    String pullRemoteBranches();

    @Key("view.pull.local.branches.title")
    String pullLocalBranches();

    // Merge
    @Key("view.merge.title")
    String mergeTitle();

    @Key("merged.commits")
    String mergedCommits(String commits);

    @Key("merged.new.head")
    String mergedNewHead(String newHead);

    @Key("merged.conflicts")
    String mergedConflicts(String conflicts);

    /* Controls */
    @Key("control.branches.title")
    String branchesControlTitle();

    @Key("control.branches.prompt")
    String branchesControlPrompt();

    @Key("control.commit.title")
    String commitControlTitle();

    @Key("control.commit.prompt")
    String commitControlPrompt();

    @Key("control.delete.title")
    String deleteControlTitle();

    @Key("control.delete.prompt")
    String deleteControlPrompt();

    @Key("control.fetch.title")
    String fetchControlTitle();

    @Key("control.fetch.prompt")
    String fetchControlPrompt();

    @Key("control.init.title")
    String initControlTitle();

    @Key("control.init.prompt")
    String initControlPrompt();

    @Key("control.merge.title")
    String mergeControlTitle();

    @Key("control.merge.prompt")
    String mergeControlPrompt();

    @Key("control.pull.title")
    String pullControlTitle();

    @Key("control.pull.prompt")
    String pullControlPrompt();

    @Key("control.push.title")
    String pushControlTitle();

    @Key("control.push.prompt")
    String pushControlPrompt();

    @Key("control.remotes.title")
    String remotesControlTitle();

    @Key("control.remotes.prompt")
    String remotesControlPrompt();

    @Key("control.resetFiles.title")
    String resetFilesControlTitle();

    @Key("control.resetFiles.prompt")
    String resetFilesControlPrompt();

    @Key("control.resetToCommit.title")
    String resetToCommitControlTitle();

    @Key("control.resetToCommit.prompt")
    String resetToCommitControlPrompt();

    @Key("control.history.title")
    String historyControlTitle();

    @Key("control.history.prompt")
    String historyControlPrompt();

    @Key("control.status.title")
    String statusControlTitle();

    @Key("control.status.prompt")
    String statusControlPrompt();

    @Key("control.ro.url.window.title")
    String projectReadOnlyGitUrlWindowTitle();

    @Key("control.ro.localUrl.title")
    String projectReadOnlyGitLocalUrlTitle();

    @Key("control.ro.remoteUrl.title")
    String projectReadOnlyGitRemoteUrlTitle();

    @Key("control.ro.remoteUrls.title")
    String projectReadOnlyGitRemoteUrlsTitle();

    @Key("control.ro.url.prompt")
    String projectReadOnlyGitUrlPrompt();

    /*
     * CloneRepositoryView
     */
    @Key("noIncorrectProjectNameMessage")
    String noIncorrectProjectNameMessage();

    @Key("noIncorrectProjectNameTitle")
    String noIncorrectProjectNameTitle();

    @Key("projectNameStartWith_Message")
    String projectNameStartWith_Message();

    /*
     * Project
     */
    @Key("project.name")
    String projectName();
}
