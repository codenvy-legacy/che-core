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

import com.google.gwt.user.client.ui.IsWidget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.api.project.tree.generic.FileNode;
import org.eclipse.che.ide.api.project.tree.generic.FolderNode;
import org.eclipse.che.ide.api.project.tree.generic.ProjectNode;
import org.eclipse.che.ide.api.project.tree.generic.StorableNode;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.ui.dialogs.ConfirmCallback;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.ui.dialogs.confirm.ConfirmDialog;
import org.eclipse.che.ide.ui.dialogs.message.MessageDialog;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.che.ide.api.notification.Notification.Type.ERROR;
import static org.eclipse.che.ide.api.project.tree.TreeNode.DeleteCallback;

/**
 * Used for deleting a {@link StorableNode}.
 *
 * @author Ann Shumilova
 * @author Artem Zatsarynnyy
 */
@Singleton
public class DeleteNodeHandler {
    private NotificationManager      notificationManager;
    private CoreLocalizationConstant localization;
    private DialogFactory            dialogFactory;

    @Inject
    public DeleteNodeHandler(NotificationManager notificationManager,
                             CoreLocalizationConstant localization,
                             DtoUnmarshallerFactory dtoUnmarshallerFactory,
                             DialogFactory dialogFactory) {
        this.notificationManager = notificationManager;
        this.localization = localization;
        this.dialogFactory = dialogFactory;
    }

    /**
     * Delete the specified node.
     *
     * @param nodeToDelete
     *         node to be deleted
     */
    public void delete(final StorableNode nodeToDelete) {
        dialogFactory.createConfirmDialog(getDialogTitle(nodeToDelete),
                                          getDialogQuestion(nodeToDelete),
                                          localization.delete(),
                                          localization.cancel(),
                                          new ConfirmCallback() {
                                              @Override
                                              public void accepted() {
                                                  nodeToDelete.delete(new DeleteCallback() {
                                                      @Override
                                                      public void onDeleted() {
                                                      }

                                                      @Override
                                                      public void onFailure(Throwable caught) {
                                                          notificationManager
                                                                  .showNotification(new Notification(caught.getMessage(), ERROR));
                                                      }
                                                  });

                                              }
                                          }, null).show();
    }

    /**
     * Ask the user to confirm the (multiple) delete operation.
     *
     * @param nodesToDelete
     *         node list for deletion
     */
    private void askForDeletingNodes(final List<StorableNode> nodesToDelete) {
        final ConfirmDialog dialog = dialogFactory.createConfirmDialog(localization.deleteMultipleDialogTitle(),
                                                                       getDialogWidget(nodesToDelete), localization.delete(),
                                                                       localization.cancel(),
                                                                       new ConfirmCallback() {
                                                                           @Override
                                                                           public void accepted() {
                                                                               for (final StorableNode nodeToDelete : nodesToDelete) {
                                                                                   nodeToDelete.delete(new DeleteCallback() {
                                                                                       @Override
                                                                                       public void onDeleted() {
                                                                                       }

                                                                                       @Override
                                                                                       public void onFailure(final Throwable caught) {
                                                                                           notificationManager.showNotification(
                                                                                                   new Notification(caught.getMessage(),
                                                                                                                    ERROR));
                                                                                       }
                                                                                   });
                                                                               }
                                                                           }
                                                                       }, null);
        dialog.show();
    }

    private IsWidget getDialogWidget(final List<StorableNode> nodesToDelete) {
        return new ConfirmMultipleDeleteWidget(nodesToDelete, this.localization);
    }

    /**
     * Return the title of the deletion dialog due the resource type.
     *
     * @param node
     * @return {@link String} title
     */
    private String getDialogTitle(StorableNode node) {
        if (node instanceof FileNode) {
            return localization.deleteFileDialogTitle();
        } else if (node instanceof FolderNode) {
            return localization.deleteFolderDialogTitle();
        } else if (node instanceof ProjectNode) {
            return localization.deleteProjectDialogTitle();
        }
        return localization.deleteNodeDialogTitle();
    }

    /**
     * Return the content of the deletion dialog due the resource type.
     *
     * @param node
     * @return {@link String} content
     */
    private String getDialogQuestion(StorableNode node) {
        final String name = node.getName();
        if (node instanceof FileNode) {
            return localization.deleteFileDialogQuestion(name);
        } else if (node instanceof FolderNode) {
            return localization.deleteFolderDialogQuestion(name);
        } else if (node instanceof ProjectNode) {
            TreeNode parent = node.getParent().getParent();
            return (parent == null) ? localization.deleteProjectDialogQuestion(name) : localization.deleteModuleDialogQuestion(name);
        }
        return localization.deleteNodeDialogQuestion(name);
    }

    /**
     * Checks and deletes nodes
     *
     * @param nodes
     *         list of nodes
     */
    public void deleteNodes(final List<StorableNode> nodes) {
        if (nodes != null && !nodes.isEmpty()) {
            if (nodes.size() == 1) {
                delete(nodes.get(0));
            } else {
                final List<StorableNode> projects = extractProjectNodes(nodes);
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
                    askForDeletingNodes(nodes);
                }
            }
        }
    }

    /**
     * Search all the nodes that are project nodes inside the given nodes.
     *
     * @param nodes
     *         the nodes
     * @return the project nodes
     */
    private List<StorableNode> extractProjectNodes(final List<StorableNode> nodes) {
        final List<StorableNode> result = new ArrayList<>();
        for (StorableNode node : nodes) {
            if (node instanceof ProjectNode) {
                result.add(node);
            }
        }
        return result;
    }

}
