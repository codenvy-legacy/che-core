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
package org.eclipse.che.ide.upload.file;

import com.google.gwt.user.client.ui.FormPanel;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.ide.api.event.FileContentUpdateEvent;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.part.explorer.project.NewProjectExplorerPresenter;
import org.eclipse.che.ide.project.node.ResourceBasedNode;
import org.eclipse.che.ide.rest.RestContext;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The purpose of this class is upload file
 *
 * @author Roman Nikitenko.
 */
public class UploadFilePresenter implements UploadFileView.ActionDelegate {

    private UploadFileView              view;
    private String                      restContext;
    private String                      workspaceId;
    private EventBus                    eventBus;
    private NotificationManager         notificationManager;
    private NewProjectExplorerPresenter projectExplorer;

    @Inject
    public UploadFilePresenter(UploadFileView view,
                               @RestContext String restContext,
                               @Named("workspaceId") String workspaceId,
                               EventBus eventBus,
                               NotificationManager notificationManager,
                               NewProjectExplorerPresenter projectExplorer) {

        this.restContext = restContext;
        this.workspaceId = workspaceId;
        this.eventBus = eventBus;
        this.view = view;
        this.projectExplorer = projectExplorer;
        this.view.setDelegate(this);
        this.view.setEnabledUploadButton(false);
        this.notificationManager = notificationManager;
    }

    /** Show dialog. */
    public void showDialog() {
        view.showDialog();
    }

    /** {@inheritDoc} */
    @Override
    public void onCancelClicked() {
        view.closeDialog();
    }

    /** {@inheritDoc} */
    @Override
    public void onSubmitComplete(String result) {
        projectExplorer.reloadChildren(getResourceBasedNode());
        if (result != null && !result.isEmpty()) {
            view.closeDialog();
            notificationManager.showError(parseMessage(result));
            return;
        }

        if (view.isOverwriteFileSelected()) {
            String path = ((HasStorablePath)getResourceBasedNode()).getStorablePath() + "/" + view.getFileName();
            eventBus.fireEvent(new FileContentUpdateEvent(path));
        }
        view.closeDialog();
    }

    /** {@inheritDoc} */
    @Override
    public void onUploadClicked() {
        view.setEncoding(FormPanel.ENCODING_MULTIPART);
        view.setAction(restContext + "/project/" + workspaceId + "/uploadfile" + ((HasStorablePath)getResourceBasedNode()).getStorablePath());
        view.submit();
    }

    /** {@inheritDoc} */
    @Override
    public void onFileNameChanged() {
        String fileName = view.getFileName();
        boolean enabled = !fileName.isEmpty();
        view.setEnabledUploadButton(enabled);
    }

    protected ResourceBasedNode<?> getResourceBasedNode() {
        List<?> selection = projectExplorer.getSelection().getAllElements();
        //we should be sure that user selected single element to work with it
        if (selection != null && selection.isEmpty() || selection.size() > 1) {
            return null;
        }

        Object o = selection.get(0);

        if (o instanceof ResourceBasedNode<?>) {
            ResourceBasedNode<?> node = (ResourceBasedNode<?>)o;
            //it may be file node, so we should take parent node
            if (node.isLeaf() && isResourceAndStorableNode(node.getParent())) {
                return (ResourceBasedNode<?>)node.getParent();
            }

            return isResourceAndStorableNode(node) ? node : null;
        }

        return null;
    }

    protected boolean isResourceAndStorableNode(@Nullable Node node) {
        return node != null && node instanceof ResourceBasedNode<?> && node instanceof HasStorablePath;
    }

    private String parseMessage(String message) {
        int startIndex = 0;
        int endIndex = -1;

        if (message.contains("<pre>message:")) {
            startIndex = message.indexOf("<pre>message:") + "<pre>message:".length();
        } else if (message.contains("<pre>")) {
            startIndex = message.indexOf("<pre>") + "<pre>".length();
        }

        if (message.contains("</pre>")) {
            endIndex = message.indexOf("</pre>");
        }
        return (endIndex != -1) ? message.substring(startIndex, endIndex) : message.substring(startIndex);
    }
}
