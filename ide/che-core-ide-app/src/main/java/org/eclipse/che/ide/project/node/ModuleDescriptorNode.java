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

import javax.validation.constraints.NotNull;
import org.eclipse.che.commons.annotation.Nullable;
import java.util.List;

/**
 * @author Vlad Zhukovskiy
 */
public class ModuleDescriptorNode extends ResourceBasedNode<ProjectDescriptor> implements HasStorablePath {

    private final ProjectDescriptorProcessor resourceProcessor;

    @Inject
    public ModuleDescriptorNode(@Assisted ProjectDescriptor projectDescriptor,
                                @Assisted NodeSettings nodeSettings,
                                @NotNull EventBus eventBus,
                                @NotNull NodeManager nodeManager,
                                @NotNull ProjectDescriptorProcessor resourceProcessor) {
        super(projectDescriptor, projectDescriptor, nodeSettings, eventBus, nodeManager);
        this.resourceProcessor = resourceProcessor;
    }

    @NotNull
    @Override
    protected Promise<List<Node>> getChildrenImpl() {
        return nodeManager.getChildren(getData(), getSettings());
    }

    @Override
    public void updatePresentation(@NotNull NodePresentation presentation) {
        presentation.setPresentableText(getData().getName());
        presentation.setPresentableIcon(nodeManager.getNodesResources().moduleRoot());
    }

    @NotNull
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

    @NotNull
    @Override
    public String getStorablePath() {
        return getData().getPath();
    }

    @Override
    public boolean supportGoInto() {
        return true;
    }
}
