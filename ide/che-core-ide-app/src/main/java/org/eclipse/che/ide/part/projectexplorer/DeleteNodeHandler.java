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
package org.eclipse.che.ide.part.projectexplorer;

import org.eclipse.che.api.runner.dto.ApplicationProcessDescriptor;
import org.eclipse.che.api.runner.gwt.client.RunnerServiceClient;

import org.eclipse.che.ide.CoreLocalizationConstant;

import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.project.node.FileReferenceNode;
import org.eclipse.che.ide.project.node.FolderReferenceNode;
import org.eclipse.che.ide.project.node.ModuleDescriptorNode;
import org.eclipse.che.ide.project.node.ProjectDescriptorNode;
import org.eclipse.che.ide.project.node.ProjectReferenceNode;
import org.eclipse.che.ide.project.node.ResourceBasedNode;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;
import org.eclipse.che.ide.ui.dialogs.ConfirmCallback;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.ui.dialogs.confirm.ConfirmDialog;
import org.eclipse.che.ide.ui.dialogs.message.MessageDialog;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import static org.eclipse.che.api.runner.ApplicationStatus.NEW;
import static org.eclipse.che.api.runner.ApplicationStatus.RUNNING;
import static org.eclipse.che.ide.api.notification.Notification.Type.ERROR;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Used for deleting a {@link Node}.
 *
 * @author Ann Shumilova
 * @author Artem Zatsarynnyy
 */
@Singleton
public class DeleteNodeHandler {
    private NotificationManager      notificationManager;
    private CoreLocalizationConstant localization;
    private RunnerServiceClient      runnerServiceClient;
    private DtoUnmarshallerFactory   dtoUnmarshallerFactory;
    private DialogFactory            dialogFactory;

    @Inject
    public DeleteNodeHandler(NotificationManager notificationManager,
                             CoreLocalizationConstant localization,
                             RunnerServiceClient runnerServiceClient,
                             DtoUnmarshallerFactory dtoUnmarshallerFactory,
                             DialogFactory dialogFactory) {
        this.notificationManager = notificationManager;
        this.localization = localization;
        this.runnerServiceClient = runnerServiceClient;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.dialogFactory = dialogFactory;
    }

    /**
     * Delete the specified node.
     *
     * @param nodeToDelete
     *         node to be deleted
     */
    public void delete(final ResourceBasedNode<?> nodeToDelete) {
        if (nodeToDelete instanceof ProjectDescriptorNode) {
            deleteProjectNode((ProjectDescriptorNode)nodeToDelete);
        } else {
            askForDeletingNode(nodeToDelete);
        }
    }

    private void deleteProjectNode(final ProjectDescriptorNode projectNodeToDelete) {
        checkRunningProcessesForProject(projectNodeToDelete, new AsyncCallback<Boolean>() {
            @Override
            public void onSuccess(final Boolean hasRunningProcesses) {
                if (hasRunningProcesses) {
                    dialogFactory.createMessageDialog("", localization.stopProcessesBeforeDeletingProject(), null).show();
                } else {
                    askForDeletingNode(projectNodeToDelete);
                }
            }

            @Override
            public void onFailure(final Throwable caught) {
                askForDeletingNode(projectNodeToDelete);
            }
        });
    }

    /**
     * Ask the user to confirm the delete operation.
     *
     * @param nodeToDelete
     */
    private void askForDeletingNode(final ResourceBasedNode<?> nodeToDelete) {
        dialogFactory.createConfirmDialog(getDialogTitle(nodeToDelete), getDialogQuestion(nodeToDelete), new ConfirmCallback() {
            @Override
            public void accepted() {
                nodeToDelete.delete();
            }
        }, null).show();
    }

    /**
     * Ask the user to confirm the (multiple) delete operation.
     *
     * @param nodesToDelete node list for deletion
     */
    private void askForDeletingNodes(final List<ResourceBasedNode<?>> nodesToDelete) {
        final ConfirmDialog dialog = dialogFactory.createConfirmDialog(localization.deleteMultipleDialogTitle(),
                                          getDialogWidget(nodesToDelete),
                                          new ConfirmCallback() {
                                                @Override
                                                public void accepted() {
                                                    for (final ResourceBasedNode nodeToDelete : nodesToDelete) {
                                                        nodeToDelete.delete();
                                                    }
                                                }
                                            }, null);
                                            dialog.show();
                                        }

    private IsWidget getDialogWidget(final List<ResourceBasedNode<?>> nodesToDelete) {
        return new ConfirmMultipleDeleteWidget(nodesToDelete, this.localization);
    }

    /**
     * Check whether there are running processes for the resource that will be deleted.
     *
     * @param projectNode
     * @param callback callback returns true if project has any running processes and false - otherwise
     */
    private void checkRunningProcessesForProject(ResourceBasedNode<?> projectNode, final AsyncCallback<Boolean> callback) {
        Unmarshallable<List<ApplicationProcessDescriptor>> unmarshaller =
                dtoUnmarshallerFactory.newListUnmarshaller(ApplicationProcessDescriptor.class);
        runnerServiceClient.getRunningProcesses(((ProjectDescriptorNode)projectNode).getStorablePath(),//todo
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

    /**
     * Return the title of the deletion dialog due the resource type.
     *
     * @param node
     * @return {@link String} title
     */
    private String getDialogTitle(Node node) {
        if (node instanceof FileReferenceNode) {
            return localization.deleteFileDialogTitle();
        } else if (node instanceof FolderReferenceNode) {
            return localization.deleteFolderDialogTitle();
        } else if (node instanceof ProjectReferenceNode) {
            return localization.deleteProjectDialogTitle();
        } else if (node instanceof ModuleDescriptorNode) {
            return localization.deleteModuleDialogTitle();
        }
        return localization.deleteNodeDialogTitle();
    }

    /**
     * Return the content of the deletion dialog due the resource type.
     *
     * @param node
     * @return {@link String} content
     */
    private String getDialogQuestion(Node node) {
        final String name = node.getName();
        if (node instanceof FileReferenceNode) {
            return localization.deleteFileDialogQuestion(name);
        } else if (node instanceof FolderReferenceNode) {
            return localization.deleteFolderDialogQuestion(name);
        } else if (node instanceof ProjectDescriptorNode) {
            return localization.deleteProjectDialogQuestion(name);
        } else if (node instanceof ModuleDescriptorNode) {
            return localization.deleteModuleDialogQuestion(name);
        }
        return localization.deleteNodeDialogQuestion(name);
    }

    public void deleteNodes(final List<ResourceBasedNode<?>> nodes) {
        if (nodes != null && !nodes.isEmpty()) {
            if (nodes.size() == 1) {
                delete(nodes.get(0));
            } else {
                final List<ResourceBasedNode<?>> projects = extractProjectNodes(nodes);
                if (projects.isEmpty()) {
                    askForDeletingNodes(nodes);
                } else if (projects.size() < nodes.size()) {
                    // mixed project and non-project nodes
                    final MessageDialog dialog = dialogFactory.createMessageDialog(localization.mixedProjectDeleteTitle(),
                                                      localization.mixedProjectDeleteMessage(),
                                                      null);
                    dialog.show();
                } else {
                    // only projects
                    deleteProjectNodes(projects);
                }
            }
        }
    }

    private void deleteProjectNodes(final List<ResourceBasedNode<?>> nodes) {
        final Queue<ResourceBasedNode<?>> nodeStack = new LinkedList<>(nodes);
        checkRunningForAllProjects(nodeStack, new AsyncCallback<Boolean>() {
            @Override
            public void onSuccess(final Boolean result) {
                if (result) {
                    dialogFactory.createMessageDialog("", localization.stopProcessesBeforeDeletingProject(), null).show();
                } else {
                    askForDeletingNodes(nodes);
                }
            }
            @Override
            public void onFailure(final Throwable caught) {
                notificationManager.showNotification(new Notification(caught.getMessage(), ERROR));
            }
        });
    }

    private void checkRunningForAllProjects(final Queue<ResourceBasedNode<?>> nodes, final AsyncCallback<Boolean> callback) {
        if (!nodes.isEmpty()) {
            final ResourceBasedNode<?> projectNode = nodes.remove();
            checkRunningProcessesForProject(projectNode, new AsyncCallback<Boolean>() {
                @Override
                public void onSuccess(final Boolean result) {
                    if (result == null) {
                        callback.onFailure(new Exception("Could not check 'running' state for project " + projectNode.getName()));
                    } else {
                        if (result) {
                            callback.onSuccess(true);
                        } else {
                            checkRunningForAllProjects(nodes, callback);
                        }
                    }
                }

                @Override
                public void onFailure(final Throwable caught) {
                    callback.onFailure(caught);
                }
            });
        } else {
            callback.onSuccess(false);
        }
    }

    /**
     * Search all the nodes that are project nodes inside the given nodes.
     *
     * @param nodes the nodes
     * @return the project nodes
     */
    private List<ResourceBasedNode<?>> extractProjectNodes(final List<ResourceBasedNode<?>> nodes) {
        final List<ResourceBasedNode<?>> result = new ArrayList<>();
        for (ResourceBasedNode node : nodes) {
            if (node instanceof ProjectDescriptorNode) {
                result.add(node);
            }
        }
        return result;
    }
}
