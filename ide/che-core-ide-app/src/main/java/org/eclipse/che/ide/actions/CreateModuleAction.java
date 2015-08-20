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
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.api.project.server.handlers.CreateModuleHandler;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.ProjectAction;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.event.ModuleCreatedEvent;
import org.eclipse.che.ide.part.explorer.project.NewProjectExplorerPresenter;
import org.eclipse.che.ide.project.node.FolderReferenceNode;
import org.eclipse.che.ide.project.shared.NodesResources;
import org.eclipse.che.ide.projecttype.wizard.presenter.ProjectWizardPresenter;
import org.eclipse.che.ide.ui.smartTree.Tree;
import org.eclipse.che.ide.util.loging.Log;

import javax.annotation.Nullable;
import java.util.List;

/** @author Artem Zatsarynnyy */
@Singleton
public class CreateModuleAction extends ProjectAction implements ModuleCreatedEvent.ModuleCreatedHandler {

    private final ProjectWizardPresenter      wizard;
    private final AnalyticsEventLogger        eventLogger;
    private final NewProjectExplorerPresenter projectExplorer;

    @Inject
    public CreateModuleAction(NodesResources resources,
                              ProjectWizardPresenter wizard,
                              AnalyticsEventLogger eventLogger,
                              NewProjectExplorerPresenter projectExplorer,
                              EventBus eventBus) {
        super("Create Module...", "Create module from existing folder", resources.moduleRoot());
        this.wizard = wizard;
        this.eventLogger = eventLogger;
        this.projectExplorer = projectExplorer;
        eventBus.addHandler(ModuleCreatedEvent.getType(), this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        eventLogger.log(this);
        FolderReferenceNode selectedFolder = getResourceBasedNode();
        if (selectedFolder != null) {
            wizard.show(selectedFolder.getData());
        }
    }

    @Override
    protected void updateProjectAction(ActionEvent e) {
        e.getPresentation().setEnabledAndVisible(getResourceBasedNode() != null);
    }

    @Nullable
    protected FolderReferenceNode getResourceBasedNode() {
        List<?> selection = projectExplorer.getSelection().getAllElements();
        //we should be sure that user selected single element to work with it
        if (selection.isEmpty() || selection.size() > 1) {
            return null;
        }

        Object o = selection.get(0);

        if (o instanceof FolderReferenceNode) {
            return (FolderReferenceNode)o;
        }

        return null;
    }

    @Override
    public void onModuleCreated(ModuleCreatedEvent event) {
        Log.info(this.getClass(), "onModuleCreated():86: " + "get res node");
        FolderReferenceNode selectedFolder = getResourceBasedNode();
        if (selectedFolder != null && selectedFolder.getParent() != null) {
            Log.info(this.getClass(), "onModuleCreated():90: " + "lets try to reload children");
            projectExplorer.reloadChildren(selectedFolder.getParent());
        }
    }
}
