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
import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.Notification;
import static org.eclipse.che.ide.api.notification.Notification.Type.ERROR;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.api.project.tree.generic.StorableNode;
import org.eclipse.che.ide.api.selection.Selection;
import org.eclipse.che.ide.api.selection.SelectionAgent;
import org.eclipse.che.ide.commons.exception.ServerException;
import org.eclipse.che.ide.part.projectexplorer.ProjectExplorerPartPresenter;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.ui.dialogs.CancelCallback;
import org.eclipse.che.ide.ui.dialogs.ConfirmCallback;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.ui.dialogs.InputCallback;
import org.eclipse.che.ide.ui.dialogs.choice.ChoiceDialog;

import java.util.List;

/**
 * Action to copy or move resources.
 *
 * @author Vitaliy Guliy
 */
@Singleton
public class PasteAction extends Action {

    private final AnalyticsEventLogger         eventLogger;
    private       SelectionAgent               selectionAgent;
    private       AppContext                   appContext;
    private       DialogFactory                dialogFactory;
    private       ProjectServiceClient         projectServiceClient;
    private       NotificationManager          notificationManager;
    private       ProjectExplorerPartPresenter projectExplorerPartPresenter;
    private       RenameItemAction             renameItemAction;

    /** List of items to do. */
    private       List<StorableNode>   items;

    /** Move items, don't copy. */
    private       boolean              moveItems = false;

    /** The path checked last time */
    private       String               checkedPath;

    /** Last checking result */
    private       boolean              checkResult;

    /** Index of current processing resource */
    private int itemIndex;

    /** Destination directory to paste. */
    private StorableNode destination;


    @Inject
    public PasteAction(Resources resources, AnalyticsEventLogger eventLogger, SelectionAgent selectionAgent,
                       CoreLocalizationConstant localization, AppContext appContext, DialogFactory dialogFactory,
                       ProjectServiceClient projectServiceClient, NotificationManager notificationManager,
                       ProjectExplorerPartPresenter projectExplorerPartPresenter, RenameItemAction renameItemAction) {
        super(localization.pasteItemsActionText(), localization.pasteItemsActionDescription(), null, resources.paste());
        this.selectionAgent = selectionAgent;
        this.eventLogger = eventLogger;
        this.appContext = appContext;
        this.dialogFactory = dialogFactory;
        this.projectServiceClient = projectServiceClient;
        this.notificationManager = notificationManager;
        this.projectExplorerPartPresenter = projectExplorerPartPresenter;
        this.renameItemAction = renameItemAction;
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

        e.getPresentation().setEnabled(canPaste());
    }

    /**
     * Sets list of items for copying.
     *
     * @param items items to copy
     */
    public void copyItems(List<StorableNode> items) {
        this.items = items;
        moveItems = false;

        checkedPath = null;
    }

    /**
     * Sets list of items for moving.
     *
     * @param items items to move
     */
    public void moveItems(List<StorableNode> items) {
        this.items = items;
        moveItems = true;

        checkedPath = null;
    }

    /**
     * Determines whether pasting can be performed.
     *
     * @return <b>true</b> if pasting can be performed, otherwise returns <b>false</b>
     */
    private boolean canPaste() {
        /** List of items must not be empty */
        if (items == null || items.isEmpty()) {
            return false;
        }

        /** Test current selection */
        Selection<?> selection = selectionAgent.getSelection();

        /** The selection must not be empty */
        if (selection == null) {
            return false;
        }

        /** Only one resource must be selected */
        if (!selection.isSingleSelection()) {
            return false;
        }

        /** Selected resource must be storable */
        if (!(selection.getHeadElement() instanceof StorableNode)) {
            return false;
        }

        /** Test selected node */
        StorableNode selectedNode = (StorableNode)selection.getHeadElement();
        if (selectedNode.getPath().equals(checkedPath)) {
            return checkResult;
        }
        checkedPath = selectedNode.getPath();

        for (StorableNode item : items) {

            if (item.canContainsFolder()) {
                // item is folder

                /** Unable to copy or move folder itself */
                if (item.getPath().equals(selectedNode.getPath())) {
                    checkResult = false;
                    return false;
                }

                /** Unable to copy or move folder to its children */
                if (selectedNode.getPath().startsWith(item.getPath())) {
                    checkResult = false;
                    return false;
                }

                /** Unable to move folder to its parent */
                if (moveItems) {
                    String folderDirectory = item.getPath().substring(0, item.getPath().lastIndexOf("/"));
                    if (selectedNode.canContainsFolder()) {
                        // when selected a folder

                        if (selectedNode.getPath().equals(folderDirectory)) {
                            checkResult = false;
                            return false;
                        }
                    } else {
                        // when selected a file

                        String fileDirectory = selectedNode.getPath().substring(0, selectedNode.getPath().lastIndexOf("/"));
                        if (fileDirectory.equals(fileDirectory)) {
                            checkResult = false;
                            return false;
                        }
                    }
                }

            } else {
                // item is file

                if (selectedNode.canContainsFolder()) {
                    // when selected a folder

                    /** Unable to move file in the same directory */
                    if (moveItems) {
                        String folderPath = selectedNode.getPath();
                        String fileDirectory = item.getPath().substring(0, item.getPath().lastIndexOf("/"));

                        if (moveItems && folderPath.equals(fileDirectory)) {
                            checkResult = false;
                            return false;
                        }
                    }
                } else {
                    // when selected a file

                    /** Unable to move file in the same directory */
                    if (moveItems) {
                        String selectedFileDirectory = selectedNode.getPath().substring(0, selectedNode.getPath().lastIndexOf("/"));
                        String fileDirectory = item.getPath().substring(0, item.getPath().lastIndexOf("/"));

                        if (selectedFileDirectory.equals(fileDirectory)) {
                            checkResult = false;
                            return false;
                        }
                    }
                }

            }
        }

        checkResult = true;
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent e) {
        eventLogger.log(this);

        if (!canPaste()) {
            return;
        }

        /** Fetch destination directory from selection */
        destination = (StorableNode)selectionAgent.getSelection().getHeadElement();
        /** Get parent directory if destination is file */
        if (!destination.canContainsFolder()) {
            destination = (StorableNode)destination.getParent();
        }

        /** Reset item index */
        itemIndex = -1;

        if (moveItems) {
            move();
        } else {
            copy();
        }
    }

    /**
     * Copies next item to destination.
     */
    private void copy() {
        /** Switch to next item and check item list */
        itemIndex++;
        if (itemIndex == items.size()) {
            /** Copying finished */
            return;
        }

        /** Get item to copy */
        final StorableNode item = items.get(itemIndex);

        try {
            /** Copy the item */
            projectServiceClient.copy(item.getPath(), destination.getPath(), null, copyCallback);
        } catch (Exception error) {
            /** Handle error and stop copying */
            notificationManager.showNotification(new Notification(error.getMessage(), ERROR));
            dialogFactory.createMessageDialog("ERROR", error.getMessage(), null).show();
        }
    }

    /**
     * Asks the user for a decision when copying existent resource.
     */
    private void resolveCopyConflict(String cause) {
        ChoiceDialog dialog = dialogFactory.createChoiceDialog("Conflict", cause, "Rename", "Skip", "Overwrite",
                new ConfirmCallback() {
                    @Override
                    public void accepted() {
                        /** Copy with new name */
                        copyWithNewName();
                    }
                }, new ConfirmCallback() {
                    @Override
                    public void accepted() {
                        /** Skip resource and copy next */
                        copy();
                    }
                }, new ConfirmCallback() {
                    @Override
                    public void accepted() {
                        /** Copy with overwriting existent resource */
                        copyWithOverwriting();
                    }
                }
        );
        dialog.show();
    }

    /**
     * Asks the user for new item name and retries copying.
     */
    private void copyWithNewName() {
        /** Get item to copy */
        final StorableNode item = items.get(itemIndex);

        /** Ask user for new resource name. */
        renameItemAction.askForNewName(item, new InputCallback() {
            @Override
            public void accepted(String value) {
                try {
                    /** Copy the item, giving new name */
                    projectServiceClient.copy(item.getPath(), destination.getPath(), value, copyCallback);
                } catch (Exception error) {
                    /** Handle error and stop copying */
                    notificationManager.showNotification(new Notification(error.getMessage(), ERROR));
                    dialogFactory.createMessageDialog("ERROR", error.getMessage(), null).show();
                }
            }
        }, new CancelCallback() {
            @Override
            public void cancelled() {
                /** Stop copying and do nothing */
            }
        });
    }

    /**
     * Copies with overwriting.
     */
    private void copyWithOverwriting() {
    }

    /**
     * Callback for copy operation.
     */
    private final AsyncRequestCallback<Void> copyCallback = new AsyncRequestCallback<Void>() {
        @Override
        protected void onSuccess(Void result) {
            /** Item copied, refresh project explorer */
            projectExplorerPartPresenter.refreshNode(destination, new AsyncCallback<TreeNode<?>>() {
                @Override
                public void onFailure(Throwable caught) {
                    /** Ignore errors and continue copying */
                    dialogFactory.createMessageDialog("ERROR", caught.getMessage(), null);
                    copy();
                }

                @Override
                public void onSuccess(TreeNode<?> result) {
                    /** Refreshing complete, copy next item */
                    copy();
                }
            });
        }

        @Override
        protected void onFailure(Throwable exception) {
            /** Check for conflict */
            if (exception instanceof ServerException && ((ServerException)exception).getHTTPStatus() == 409) {
                /** Resolve conflicting situation */
                resolveCopyConflict(exception.getMessage());
                return;
            }

            /** Handle error and stop copying */
            notificationManager.showNotification(new Notification(exception.getMessage(), ERROR));
            dialogFactory.createMessageDialog("ERROR", exception.getMessage(), null).show();
        }
    };

    /**
     * Moves items to destination directory.
     */
    private void move() {
    }

    /**
     * Logs text to browser console.
     *
     * @param text text to log
     */
    public final native void log(String text) /*-{
        console.log(text);
    }-*/;

}
