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
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.selection.Selection;
import org.eclipse.che.ide.download.DownloadContainer;
import org.eclipse.che.ide.part.explorer.project.NewProjectExplorerPresenter;
import org.eclipse.che.ide.project.node.ProjectReferenceNode;
import org.eclipse.che.ide.rest.RestContext;

import java.util.List;

/**
 * Download project as zip action
 *
 * @author Roman Nikitenko
 */
@Singleton
public class DownloadProjectAsZipAction extends Action {

    private final String BASE_URL;

    private final AnalyticsEventLogger        eventLogger;
    private final AppContext                  appContext;
    private final NewProjectExplorerPresenter projectExplorer;
    private       DownloadContainer           downloadContainer;

    @Inject
    public DownloadProjectAsZipAction(@RestContext String restContext,
                                      @Named("workspaceId") String workspaceId,
                                      AppContext appContext,
                                      CoreLocalizationConstant locale,
                                      NewProjectExplorerPresenter projectExplorer,
                                      AnalyticsEventLogger eventLogger,
                                      Resources resources,
                                      DownloadContainer downloadContainer) {
        super(locale.downloadProjectAsZipName(), locale.downloadProjectAsZipDescription(), null);
        this.appContext = appContext;
        this.eventLogger = eventLogger;
        this.projectExplorer = projectExplorer;
        this.downloadContainer = downloadContainer;

        BASE_URL = restContext + "/project/" + workspaceId + "/export/";
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent e) {
        eventLogger.log(this);

        String url = BASE_URL + getPath();
        downloadContainer.setUrl(url);
    }

    /** {@inheritDoc} */
    @Override
    public void update(ActionEvent event) {
        Selection<?> selection = projectExplorer.getSelection();
        boolean enabled = appContext.getCurrentProject() != null || selection != null ||
                          (!selection.isEmpty() && selection.getHeadElement() instanceof ProjectReferenceNode);

        event.getPresentation().setVisible(true);
        event.getPresentation().setEnabled(enabled);
    }

    private String getPath() {
        String path = "";
        HasStorablePath selectedNode = null;

        List<?> selection = projectExplorer.getSelection().getAllElements();
        CurrentProject currentProject = appContext.getCurrentProject();

        if (!selection.isEmpty() && selection.get(0) instanceof HasStorablePath) {
            selectedNode = (HasStorablePath)selection.get(0);
        }

        if (selectedNode != null && selectedNode instanceof ProjectReferenceNode) {
            path = selectedNode.getStorablePath();
        } else if (currentProject != null) {
            path = currentProject.getProjectDescription().getPath();
        }

        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }
}
