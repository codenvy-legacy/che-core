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
package org.eclipse.che.ide;

import com.google.gwt.i18n.client.Messages;

/** @author Andrey Plotnikov */
public interface CoreLocalizationConstant extends Messages {
    @Key("createProjectFromTemplate.nameField")
    String createProjectFromTemplateName();

    @Key("createProjectFromTemplate.project.exists")
    String createProjectFromTemplateProjectExists(String projectName);

    @Key("extension.title")
    String extensionTitle();

    @Key("extension.category")
    String extensionCategory();

    @Key("navigateToFile.view.title")
    String navigateToFileViewTitle();

    @Key("navigateToFile.view.file.field.title")
    String navigateToFileViewFileFieldTitle();

    @Key("navigateToFile.view.file.field.prompt")
    String navigateToFileViewFileFieldPrompt();

    @Key("navigateToFile.searchIsCaseSensitive")
    String navigateToFileSearchIsCaseSensitive();

    @Key("appearance.title")
    String appearanceTitle();

    @Key("appearance.category")
    String appearanceCategory();

    /* DeleteItem */
    @Key("action.delete.text")
    String deleteItemActionText();

    @Key("action.delete.description")
    String deleteItemActionDescription();

    /* Cut */
    @Key("action.cut.text")
    String cutItemsActionText();

    @Key("action.cut.description")
    String cutItemsActionDescription();

    /* Copy */
    @Key("action.copy.text")
    String copyItemsActionText();

    @Key("action.copy.description")
    String copyItemsActionDescription();

    /* Paste */
    @Key("action.paste.text")
    String pasteItemsActionText();

    @Key("action.paste.description")
    String pasteItemsActionDescription();

    @Key("deleteDialogTitle")
    String deleteDialogTitle();

    @Key("deleteAddToIndexDialogTitle")
    String deleteAddToIndexDialogTitle();

    @Key("deleteAddToIndexDialogText")
    String deleteAddToIndexDialogText();

    @Key("deleteAddToIndexDialogNotification")
    String deleteAddToIndexDialogNotification();

    @Key("deleteAllFilesAndSubdirectories")
    String deleteAllFilesAndSubdirectories(String name);

    @Key("deleteFilesAndSubdirectoriesInTheSelectedDirectory")
    String deleteFilesAndSubdirectoriesInTheSelectedDirectory();

    @Key("mixedProjectDeleteMessage")
    String mixedProjectDeleteMessage();

    /* RenameItem */
    @Key("action.rename.text")
    String renameItemActionText();

    @Key("action.rename.description")
    String renameItemActionDescription();

    @Key("renameNodeDialogTitle")
    String renameNodeDialogTitle();

    @Key("renameFileDialogTitle")
    String renameFileDialogTitle();

    @Key("renameFolderDialogTitle")
    String renameFolderDialogTitle();

    @Key("renameProjectDialogTitle")
    String renameProjectDialogTitle();

    @Key("renameDialogNewNameLabel")
    String renameDialogNewNameLabel();

    @Key("createProjectFromTemplate.descriptionField")
    String createProjectFromTemplateDescription();

    @Key("createProjectFromTemplate.projectPrivacy")
    String createProjectFromTemplateProjectPrivacy();

    @Key("createProjectFromTemplate.public")
    String createProjectFromTemplatePublic();

    @Key("createProjectFromTemplate.private")
    String createProjectFromTemplatePrivate();

    @Key("projectWizard.defaultTitleText")
    String projectWizardDefaultTitleText();

    @Key("projectWizard.titleText")
    String projectWizardTitleText();

    @Key("projectWizard.createModule.titleText")
    String projectWizardCreateModuleTitleText();

    @Key("projectWizard.defaultSaveButtonText")
    String projectWizardDefaultSaveButtonText();

    @Key("projectWizard.saveButtonText")
    String projectWizardSaveButtonText();

    @Key("format.name")
    String formatName();

    @Key("format.description")
    String formatDescription();

    @Key("undo.name")
    String undoName();

    @Key("undo.description")
    String undoDescription();

    @Key("redo.name")
    String redoName();

    @Key("redo.description")
    String redoDescription();

    @Key("uploadFile.name")
    String uploadFileName();

    @Key("uploadFile.description")
    String uploadFileDescription();

    @Key("uploadFile.title")
    String uploadFileTitle();

    @Key("uploadFile.overwrite")
    String uploadFileOverwrite();

    @Key("uploadFolderFromZip.name")
    String uploadFolderFromZipName();

    @Key("uploadFolderFromZip.description")
    String uploadFolderFromZipDescription();

    @Key("downloadZip.project.name")
    String downloadProjectAsZipName();

    @Key("downloadZip.project.description")
    String downloadProjectAsZipDescription();

    @Key("download.item.name")
    String downloadItemName();

    @Key("download.item.description")
    String downloadItemDescription();

    @Key("uploadFolderFromZip.overwrite")
    String uploadFolderFromZipOverwrite();

    @Key("uploadFolderFromZip.skipFirstLevel")
    String uploadFolderFromZipSkipFirstLevel();

    @Key("cancelButton")
    String cancelButton();

    @Key("uploadButton")
    String uploadButton();

    @Key("openFileFieldTitle")
    String openFileFieldTitle();

    @Key("uploadFolderFromZip.openZipFieldTitle")
    String uploadFolderFromZipOpenZipFieldTitle();

    @Key("projectExplorer.button.title")
    String projectExplorerButtonTitle();

    @Key("projectExplorer.titleBar.text")
    String projectExplorerTitleBarText();

    @Key("importProject.message.success")
    String importProjectMessageSuccess(String projectName);

    @Key("importProject.message.failure")
    String importProjectMessageFailure(String projectName);

    @Key("importProject.message.startWithWhiteSpace")
    String importProjectMessageStartWithWhiteSpace();

    @Key("importProject.message.urlInvalid")
    String importProjectMessageUrlInvalid();

    @Key("importProject.message.unableGetSshKey")
    String importProjectMessageUnableGetSshKey();

    @Key("importProjectFromLocation.name")
    String importProjectFromLocationName();

    @Key("importProjectFromLocation.description")
    String importProjectFromLocationDescription();

    @Key("importLocalProject.name")
    String importLocalProjectName();

    @Key("importLocalProject.description")
    String importLocalProjectDescription();

    @Key("importLocalProject.openZipTitle")
    String importLocalProjectOpenZipTitle();

    @Key("importProject.importButton")
    String importProjectButton();

    @Key("importProject.importing")
    String importingProject(String projectName);

    @Key("importProject.uriFieldTitle")
    String importProjectUriFieldTitle();

    @Key("importProject.viewTitle")
    String importProjectViewTitle();

    @Key("importProject.importer.info")
    String importProjectImporterInfo();

    @Key("importProject.project.info")
    String importProjectInfo();

    @Key("importProject.name.prompt")
    String importProjectNamePrompt();

    @Key("importProject.description.prompt")
    String importProjectDescriptionPrompt();

    @Key("importProject.zipImporter.skipFirstLevel")
    String importProjectZipImporterSkipFirstLevel();

    @Key("import.project.error")
    String importProjectError();

    /* Actions */
    @Key("action.newFolder.title")
    String actionNewFolderTitle();

    @Key("action.newFolder.description")
    String actionNewFolderDescription();

    @Key("action.newFile.title")
    String actionNewFileTitle();

    @Key("action.newFile.description")
    String actionNewFileDescription();

    @Key("action.newFile.add.to.index.title")
    String actionNewFileAddToIndexTitle();

    @Key("action.newFile.add.to.index.text")
    String actionNewFileAddToIndexText(String file);

    @Key("action.newFile.add.to.index.notification")
    String actionNewFileAddToIndexNotification(String file);

    @Key("action.newXmlFile.title")
    String actionNewXmlFileTitle();

    @Key("action.newXmlFile.description")
    String actionNewXmlFileDescription();

    @Key("action.projectConfiguration.description")
    String actionProjectConfigurationDescription();

    @Key("action.projectConfiguration.title")
    String actionProjectConfigurationTitle();

    @Key("action.findAction.description")
    String actionFindActionDescription();

    @Key("action.findAction.title")
    String actionFindActionTitle();

    @Key("action.showHiddenFiles.title")
    String actionShowHiddenFilesTitle();

    @Key("action.showHiddenFiles.description")
    String actionShowHiddenFilesDescription();

    /* NewResource */
    @Key("newResource.title")
    String newResourceTitle(String title);

    @Key("newResource.label")
    String newResourceLabel(String title);

    @Key("newResource.invalidName")
    String invalidName();

    /* Messages */
    @Key("messages.changesMayBeLost")
    String changesMayBeLost();

    @Key("messages.allFilesSaved")
    String allFilesSaved();

    @Key("messages.someFilesCanNotBeSaved")
    String someFilesCanNotBeSaved();

    @Key("messages.saveChanges")
    String messagesSaveChanges(String name);

    @Key("messages.promptSaveChanges")
    String messagesPromptSaveChanges();

    @Key("messages.unableOpenResource")
    String unableOpenResource(String path);

    @Key("messages.canNotOpenFileWithoutParams")
    String canNotOpenFileWithoutParams();

    @Key("messages.fileToOpenIsNotSpecified")
    String fileToOpenIsNotSpecified();

    @Key("messages.canNotOpenNodeWithoutParams")
    String canNotOpenNodeWithoutParams();

    @Key("messages.nodeToOpenIsNotSpecified")
    String nodeToOpenIsNotSpecified();

    @Key("messages.noOpenedProject")
    String noOpenedProject();

    @Key("messages.startingOperation")
    String startingOperation(String operation);

    @Key("messages.startingMachine")
    String startingMachine(String machineName);

    /* Buttons */
    @Key("ok")
    String ok();

    @Key("cancel")
    String cancel();

    @Key("open")
    String open();

    @Key("next")
    String next();

    @Key("back")
    String back();

    @Key("close")
    String close();

    @Key("save")
    String save();

    @Key("apply")
    String apply();

    @Key("refresh")
    String refresh();

    @Key("delete")
    String delete();

    @Key("projectProblem.title")
    String projectProblemTitle();

    @Key("projectProblem.message")
    String projectProblemMessage();

    @Key("action.expandEditor.title")
    String actionExpandEditorTitle();

    @Key("askWindow.close.title")
    String askWindowCloseTitle();

    @Key("action.completions.title")
    String actionCompetitionsTitle();

    @Key("project.settings.title")
    String projectSettingsTitle();

    @Key("create.ws.title")
    String createWsTitle();

    @Key("create.ws.recipe.url")
    String createWsRecipeUrl();

    @Key("create.ws.find.by.tags")
    String createWsFindByTags();

    @Key("create.ws.name")
    String createWsName();

    @Key("create.ws.url.not.valid")
    String createWsUrlNotValid();

    @Key("create.ws.recipe.not.found")
    String createWsRecipeNotFound();

    @Key("create.ws.button")
    String createWsButton();

    @Key("create.ws.default.name")
    String createWsDefaultName();

    @Key("create.ws.name.is.not.correct")
    String createWsNameIsNotCorrect();

    @Key("create.ws.predefined.recipe")
    String createWsPredefinedRecipe();

    @Key("placeholder.input.recipe.url")
    String placeholderInputRecipeUrl();

    @Key("placeholder.choose.predefined")
    String placeholderChoosePredefined();

    @Key("placeholder.find.by.tags")
    String placeholderFindByTags();

    @Key("start.ws.button")
    String startWsButton();

    @Key("placeholder.select.ws.to.start")
    String placeholderSelectWsToStart();

    @Key("start.ws.title")
    String startWsTitle();

    @Key("start.ws.select.to.start")
    String startWsSelectToStart();

    @Key("stop.ws.title")
    String stopWsTitle();

    @Key("stop.ws.description")
    String stopWsDescription();

    @Key("started.ws")
    String startedWs(String wsName);

    @Key("create.snapshot.title")
    String createSnapshotTitle();

    @Key("create.snapshot.description")
    String createSnapshotDescription();

    @Key("create.snapshot.progress")
    String createSnapshotProgress();

    @Key("create.snapshot.success")
    String createSnapshotSuccess();

    @Key("ext.server.started")
    String extServerStarted();

    @Key("ext.server.stopped")
    String extServerStopped();

    @Key("workspace.start.failed")
    String workspaceStartFailed(String workspaceName);

    @Key("workspace.config.undefined")
    String workspaceConfigUndefined();

    @Key("start.ws.error.title")
    String startWsErrorTitle();

    @Key("start.ws.error.content")
    String startWsErrorContent(String workspaceName, String reason);

    @Key("create.ws.name.length.is.not.correct")
    String createWsNameLengthIsNotCorrect();

    @Key("create.ws.name.already.exist")
    String createWsNameAlreadyExist();

    @Key("get.ws.error.dialog.title")
    String getWsErrorDialogTitle();

    @Key("get.ws.error.dialog.content")
    String getWsErrorDialogContent(String reason);

    @Key("project.explorer.project.configuration.failed")
    String projectExplorerProjectConfigurationFailed();

    @Key("project.explorer.project.update.failed")
    String projectExplorerProjectUpdateFailed();

    @Key("project.explorer.projects.load.failed")
    String projectExplorerProjectsLoadFailed();

    @Key("project.explorer.detected.unconfigured.project")
    String projectExplorerDetectedUnconfiguredProject();

    @Key("project.explorer.extension.server.stopped")
    String projectExplorerExtensionServerStopped();

    @Key("project.explorer.part.tooltip")
    String projectExplorerPartTooltip();

    @Key("switch.to.left.editor.action.description")
    String switchToLeftEditorActionDescription();

    @Key("switch.to.left.editor.action")
    String switchToLeftEditorAction();

    @Key("switch.to.right.editor.action.description")
    String switchToRightEditorActionDescription();

    @Key("switch.to.right.editor.action")
    String switchToRightEditorAction();

    @Key("hot.keys.action.name")
    String hotKeysActionName();

    @Key("hot.keys.action.description")
    String hotKeysActionDescription();

    @Key("hot.keys.dialog.title")
    String hotKeysDialogTitle();

    @Key("hot.keys.table.action.description.title")
    String hotKeysTableActionDescriptionTitle();

    @Key("hot.keys.table.item.title")
    String hotKeysTableItemTitle();
}
