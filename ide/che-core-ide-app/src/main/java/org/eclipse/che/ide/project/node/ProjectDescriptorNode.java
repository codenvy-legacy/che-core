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
package org.eclipse.che.ide.project.node;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.api.project.node.resource.DeleteProcessor;
import org.eclipse.che.ide.api.project.node.resource.RenameProcessor;
import org.eclipse.che.ide.api.project.node.settings.NodeSettings;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.project.node.resource.ProjectDescriptorProcessor;
import org.eclipse.che.ide.ui.smartTree.presentation.NodePresentation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author Vlad Zhukovskiy
 */
public class ProjectDescriptorNode extends ResourceBasedNode<ProjectDescriptor> implements HasStorablePath {

    private final ProjectDescriptorProcessor resourceProcessor;

    @Inject
    public ProjectDescriptorNode(@Assisted ProjectDescriptor projectDescriptor,
                                 @Assisted NodeSettings nodeSettings,
                                 @Nonnull EventBus eventBus,
                                 @Nonnull ResourceNodeManager resourceNodeManager,
                                 @Nonnull ProjectDescriptorProcessor resourceProcessor) {
        super(projectDescriptor, projectDescriptor, nodeSettings, eventBus, resourceNodeManager);
        this.resourceProcessor = resourceProcessor;
    }

    @Nonnull
    @Override
    protected Promise<List<Node>> getChildrenImpl() {
        return resourceNodeManager.getChildren(getData(), getSettings());
    }

    @Override
    public void updatePresentation(@Nonnull NodePresentation presentation) {
        presentation.setPresentableText(getData().getName());
        presentation.setPresentableIcon(resourceNodeManager.getNodesResources().projectRoot());
    }

    @Nonnull
    @Override
    public String getName() {
        return getData().getName();
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Nullable
    @Override
    public DeleteProcessor<ProjectDescriptor> getDeleteProcessor() {
        return resourceProcessor;
    }

    @Nullable
    @Override
    public RenameProcessor<ProjectDescriptor> getRenameProcessor() {
        return resourceProcessor;
    }

    @Nonnull
    @Override
    public String getStorablePath() {
        return getData().getPath();
    }

    @Override
    public boolean supportGoInto() {
        return true;
    }
}
