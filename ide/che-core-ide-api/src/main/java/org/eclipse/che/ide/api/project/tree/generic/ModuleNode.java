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
package org.eclipse.che.ide.api.project.tree.generic;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.icon.IconRegistry;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;

/**
 * Node that represents a module.
 *
 * @author Vladyslav Zhukovskyi
 */
public class ModuleNode extends ProjectNode {

    private final AppContext appContext;

    @Inject
    public ModuleNode(@Assisted TreeNode<?> parent,
                      @Assisted ProjectDescriptor data,
                      @Assisted GenericTreeStructure treeStructure, EventBus eventBus,
                      ProjectServiceClient projectServiceClient,
                      DtoUnmarshallerFactory dtoUnmarshallerFactory,
                      IconRegistry iconRegistry,
                      AppContext appContext) {
        super(parent, data, treeStructure, eventBus, projectServiceClient, dtoUnmarshallerFactory);

        this.appContext = appContext;
        setDisplayIcon(iconRegistry.getIcon("maven.module").getSVGImage());
    }

    /** {@inheritDoc} */
    @Override
    public void delete(final DeleteCallback callback) {
        final CurrentProject currentProject = appContext.getCurrentProject();
        if (currentProject == null) {
            throw new IllegalStateException("No opened project.");
        }

        currentProject.getCurrentTree().getNodeByPath(getPath(), new AsyncCallback<TreeNode<?>>() {
            @Override
            public void onFailure(Throwable caught) {
                callback.onFailure(caught);
            }

            @Override
            public void onSuccess(TreeNode<?> result) {
                String parentPath = result.getParent().getProject().getPath();
                String moduleRelativePath = getPath().substring(parentPath.length() + 1);
                removeModule(parentPath, moduleRelativePath, callback);
            }
        });

    }

    private void removeModule(String parentPath, String moduleRelativePath,  DeleteCallback callback) {
        projectServiceClient.deleteModule(parentPath, moduleRelativePath, new AsyncRequestCallback<Void>() {
            @Override
            protected void onSuccess(Void result) {
                ModuleNode.super.delete(callback);
            }

            @Override
            protected void onFailure(Throwable exception) {
                callback.onFailure(exception);
            }
        });
    }
}
