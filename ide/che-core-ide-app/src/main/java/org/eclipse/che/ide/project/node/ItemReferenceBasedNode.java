/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.project.node;

import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.api.project.node.resource.DeleteProcessor;
import org.eclipse.che.ide.api.project.node.resource.RenameProcessor;
import org.eclipse.che.ide.api.project.node.settings.NodeSettings;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.project.node.resource.ItemReferenceProcessor;

import javax.validation.constraints.NotNull;
import org.eclipse.che.commons.annotation.Nullable;
import java.util.List;

/**
 * @author Vlad Zhukovskiy
 */
public abstract class ItemReferenceBasedNode extends ResourceBasedNode<ItemReference> implements HasStorablePath {
    protected final ItemReferenceProcessor resourceProcessor;

    public ItemReferenceBasedNode(@NotNull ItemReference itemReference,
                                  @NotNull ProjectDescriptor projectDescriptor,
                                  @NotNull NodeSettings nodeSettings,
                                  @NotNull EventBus eventBus,
                                  @NotNull NodeManager nodeManager,
                                  @NotNull ItemReferenceProcessor resourceProcessor) {
        super(itemReference, projectDescriptor, nodeSettings, eventBus, nodeManager);
        this.resourceProcessor = resourceProcessor;
    }

    @Nullable
    @Override
    public DeleteProcessor<ItemReference> getDeleteProcessor() {
        return resourceProcessor;
    }

    @Nullable
    @Override
    public RenameProcessor<ItemReference> getRenameProcessor() {
        return resourceProcessor;
    }

    @NotNull
    @Override
    public String getName() {
        return getData().getName();
    }

    @Override
    public boolean isLeaf() {
        return "file".equals(getData().getType());
    }

    @NotNull
    @Override
    protected Promise<List<Node>> getChildrenImpl() {
        return nodeManager.getChildren(getData(), getProjectDescriptor(), getSettings());
    }

    @NotNull
    @Override
    public String getStorablePath() {
        return getData().getPath();
    }

}
