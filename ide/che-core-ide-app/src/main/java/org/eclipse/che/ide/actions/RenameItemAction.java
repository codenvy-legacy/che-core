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
package org.eclipse.che.ide.actions;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.api.runner.dto.ApplicationProcessDescriptor;
import org.eclipse.che.api.runner.gwt.client.RunnerServiceClient;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.project.node.HasProjectDescriptor;
import org.eclipse.che.ide.api.project.node.resource.SupportRename;
import org.eclipse.che.ide.api.selection.Selection;
import org.eclipse.che.ide.part.explorer.project.NewProjectExplorerPresenter;
import org.eclipse.che.ide.project.node.FileReferenceNode;
import org.eclipse.che.ide.project.node.FolderReferenceNode;
import org.eclipse.che.ide.project.node.ProjectDescriptorNode;
import org.eclipse.che.ide.project.node.ProjectReferenceNode;
import org.eclipse.che.ide.project.node.ResourceBasedNode;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;
import org.eclipse.che.ide.ui.dialogs.CancelCallback;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.ui.dialogs.InputCallback;
import org.eclipse.che.ide.ui.dialogs.input.InputDialog;
import org.eclipse.che.ide.ui.dialogs.input.InputValidator;
import org.eclipse.che.ide.util.NameUtils;

import javax.annotation.Nullable;

import java.util.List;

import static org.eclipse.che.api.runner.ApplicationStatus.NEW;
import static org.eclipse.che.api.runner.ApplicationStatus.RUNNING;

/**
 * Action for renaming an item which is selected in 'Project Explorer'.
 *
 * @author Artem Zatsarynnyy
 */
@Singleton
public class RenameItemAction extends Action {
    private final AnalyticsEventLogger        eventLogger;
    private final CoreLocalizationConstant    localization;
    private final RunnerServiceClient         runnerServiceClient;
    private final DtoUnmarshallerFactory      dtoUnmarshallerFactory;
    private final DialogFactory               dialogFactory;
    private final AppContext                  appContext;
    private final NewProjectExplorerPresenter projectExplorer;
    private final InputValidator              fileNameValidator;
    private final InputValidator              folderNameValidator;
    private final InputValidator              projectNameValidator;

    @Inject
    public RenameItemAction(Resources resources,
                            AnalyticsEventLogger eventLogger,
                            NewProjectExplorerPresenter projectExplorer,
                            CoreLocalizationConstant localization,
                            RunnerServiceClient runnerServiceClient,
                            DtoUnmarshallerFactory dtoUnmarshallerFactory,
                            DialogFactory dialogFactory,
                            AppContext appContext) {
        super(localization.renameItemActionText(), localization.renameItemActionDescription(), null, resources.rename());
        this.projectExplorer = projectExplorer;
        this.eventLogger = eventLogger;
        this.localization = localization;
        this.runnerServiceClient = runnerServiceClient;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.dialogFactory = dialogFactory;
        this.appContext = appContext;
        this.fileNameValidator = new FileNameValidator();
        this.folderNameValidator = new FolderNameValidator();
        this.projectNameValidator = new ProjectNameValidator();
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent e) {
        eventLogger.log(this);

        Selection<?> selection = projectExplorer.getSelection();
        if (selection.isEmpty()) {
            return;
        }

        final ResourceBasedNode<?> selectedNode = (ResourceBasedNode<?>)selection.getHeadElement();

        if (selectedNode == null) {
            return;
        }

//        ResourceBasedNode<?> selectedParent = selectedNode.getParent();

        if (selectedNode instanceof ProjectDescriptorNode) {
            dialogFactory.createMessageDialog("", localization.closeProjectBeforeRenaming(), null).show();
        } else {
            checkRunningProcessesForProject(selectedNode, new AsyncCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean hasRunningProcesses) {
                    if (hasRunningProcesses) {
                        dialogFactory.createMessageDialog("", localization.stopProcessesBeforeRenamingProject(), null).show();
                    } else {
                        renameNode(selectedNode);
                    }
                }

                @Override
                public void onFailure(Throwable caught) {
                    renameNode(selectedNode);
                }
            });
        }
    }

    /** {@inheritDoc} */
    @Override
    public void update(ActionEvent e) {
        if ((appContext.getCurrentProject() == null && !appContext.getCurrentUser().isUserPermanent()) ||
            (appContext.getCurrentProject() != null && appContext.getCurrentProject().isReadOnly())) {
            e.getPresentation().setVisible(true);
            e.getPresentation().setEnabled(false);
            return;
        }

        Selection<?> selection = projectExplorer.getSelection();
        e.getPresentation().setEnabled(!selection.isEmpty() && selection.getHeadElement() instanceof SupportRename<?>);
    }

    /**
     * Asks the user for new name and renames the node.
     *
     * @param node node to rename
     */
    private void renameNode(final ResourceBasedNode<?> node) {
        final InputCallback inputCallback = new InputCallback() {
            @Override
            public void accepted(final String value) {
                node.rename(value);
            }
        };

        askForNewName(node, inputCallback, null);
    }

    /**
     * Asks the user for new node name.
     *
     * @param node
     * @param inputCallback
     * @param cancelCallback
     */
    public void askForNewName(final ResourceBasedNode<?> node, final InputCallback inputCallback, final CancelCallback cancelCallback) {
        final int selectionLength = node.getName().indexOf('.') >= 0
                ? node.getName().lastIndexOf('.')
                : node.getName().length();

        InputDialog inputDialog = dialogFactory.createInputDialog(getDialogTitle(node),
                localization.renameDialogNewNameLabel(),
                node.getName(), 0, selectionLength, inputCallback, null);
        if (node instanceof FileReferenceNode) {
            inputDialog.withValidator(fileNameValidator);
        } else if (node instanceof FolderReferenceNode) {
            inputDialog.withValidator(folderNameValidator);
        } else if (node instanceof ProjectDescriptorNode || node instanceof ProjectReferenceNode) {
            inputDialog.withValidator(projectNameValidator);
        }
        inputDialog.show();
    }

    /**
     * Check whether project has any running processes.
     *
     * @param projectNode
     *         project to check
     * @param callback
     *         callback returns true if project has any running processes and false - otherwise
     */
    private void checkRunningProcessesForProject(HasProjectDescriptor projectNode, final AsyncCallback<Boolean> callback) {
        Unmarshallable<List<ApplicationProcessDescriptor>> unmarshaller =
                dtoUnmarshallerFactory.newListUnmarshaller(ApplicationProcessDescriptor.class);
        runnerServiceClient.getRunningProcesses(projectNode.getPath(),
                                                new AsyncRequestCallback<List<ApplicationProcessDescriptor>>(unmarshaller) {
                                                    @Override
                                                    protected void onSuccess(List<ApplicationProcessDescriptor> result) {
                                                        boolean hasRunningProcesses = false;
                                                        for (ApplicationProcessDescriptor descriptor : result) {
                                                            if (descriptor.getStatus() == NEW || descriptor.getStatus() == RUNNING) {
                                                                hasRunningProcesses = true;
                                                                break;
                                                            }
                                                        }
                                                        callback.onSuccess(hasRunningProcesses);
                                                    }

                                                    @Override
                                                    protected void onFailure(Throwable exception) {
                                                        callback.onFailure(exception);
                                                    }
                                                });
    }

    private String getDialogTitle(ResourceBasedNode<?> node) {
        if (node instanceof FileReferenceNode) {
            return localization.renameFileDialogTitle();
        } else if (node instanceof FolderReferenceNode) {
            return localization.renameFolderDialogTitle();
        } else if (node instanceof ProjectDescriptorNode || node instanceof ProjectReferenceNode) {
            return localization.renameProjectDialogTitle();
        }
        return localization.renameNodeDialogTitle();
    }

    private class FileNameValidator implements InputValidator {
        @Nullable
        @Override
        public Violation validate(String value) {
            if (!NameUtils.checkFileName(value)) {
                return new Violation() {
                    @Override
                    public String getMessage() {
                        return localization.invalidName();
                    }

                    @Nullable
                    @Override
                    public String getCorrectedValue() {
                        return null;
                    }
                };
            }
            return null;
        }
    }

    private class FolderNameValidator implements InputValidator {
        @Nullable
        @Override
        public Violation validate(String value) {
            if (!NameUtils.checkFolderName(value)) {
                return new Violation() {
                    @Override
                    public String getMessage() {
                        return localization.invalidName();
                    }

                    @Nullable
                    @Override
                    public String getCorrectedValue() {
                        return null;
                    }
                };
            }
            return null;
        }
    }

    private class ProjectNameValidator implements InputValidator {
        @Nullable
        @Override
        public Violation validate(String value) {
            final String correctValue = value.contains(" ") ? value.replaceAll(" ", "-") : null;
            final String errormessage = !NameUtils.checkFileName(value) ? localization.invalidName() : null;
            if (correctValue != null || errormessage != null) {
                return new Violation() {
                    @Override
                    public String getMessage() {
                        return errormessage;
                    }

                    @Nullable
                    @Override
                    public String getCorrectedValue() {
                        return correctValue;
                    }
                };
            }
            return null;
        }
    }
}
