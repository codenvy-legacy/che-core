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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.project.tree.generic.FileNode;
import org.eclipse.che.ide.api.project.tree.generic.StorableNode;
import org.eclipse.che.ide.api.selection.Selection;
import org.eclipse.che.ide.api.selection.SelectionAgent;
import org.eclipse.che.ide.download.DownloadContainer;
import org.eclipse.che.ide.rest.RestContext;

/**
 * Download selected item action.
 * If selected item is a project or a folder then item will be downloaded as a zip archive.
 *
 * @author Roman Nikitenko
 */
@Singleton
public class DownloadItemAction extends Action {

    private final String BASE_URL;

    private final SelectionAgent       selectionAgent;
    private final AnalyticsEventLogger eventLogger;
    private       DownloadContainer    downloadContainer;

    @Inject
    public DownloadItemAction(@RestContext String restContext,
                              @Named("workspaceId") String workspaceId,
                              CoreLocalizationConstant locale,
                              SelectionAgent selectionAgent,
                              AnalyticsEventLogger eventLogger,
                              Resources resources,
                              DownloadContainer downloadContainer) {
        super(locale.downloadItemName(), locale.downloadItemDescription(), null);
        this.selectionAgent = selectionAgent;
        this.eventLogger = eventLogger;
        this.downloadContainer = downloadContainer;

        BASE_URL = restContext + "/project/" + workspaceId + "/export/";
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent e) {
        eventLogger.log(this);

        Selection<?> selection = selectionAgent.getSelection();
        if (selection == null) {
            return;
        }

        final StorableNode selectedNode = (StorableNode)selection.getHeadElement();
        if (selectedNode == null) {
            return;
        }

        String url = getUrl(selectedNode);
        downloadContainer.setUrl(url);
    }

    /** {@inheritDoc} */
    @Override
    public void update(ActionEvent event) {
        event.getPresentation().setVisible(true);
        boolean enabled = false;
        Selection<?> selection = selectionAgent.getSelection();
        if (selection != null) {
            enabled = selection.getHeadElement() != null;
        }
        event.getPresentation().setEnabled(enabled);
    }

    private String getUrl(StorableNode selectedNode) {
        String path = normalizePath(selectedNode.getPath());

        if (selectedNode instanceof FileNode) {
            return BASE_URL + "file/" + path;
        }
        return BASE_URL + path;
    }

    private String normalizePath(String path) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }
}
