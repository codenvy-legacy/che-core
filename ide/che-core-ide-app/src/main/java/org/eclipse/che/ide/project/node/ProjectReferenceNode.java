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
import org.eclipse.che.api.project.shared.dto.ProjectReference;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.api.event.OpenProjectEvent;
import org.eclipse.che.ide.api.project.node.HasAction;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.api.project.node.resource.DeleteProcessor;
import org.eclipse.che.ide.api.project.node.resource.RenameProcessor;
import org.eclipse.che.ide.api.project.node.settings.NodeSettings;
import org.eclipse.che.ide.project.node.resource.ProjectReferenceProcessor;
import org.eclipse.che.ide.ui.smartTree.presentation.NodePresentation;
import org.eclipse.che.ide.util.Pair;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;

/**
 * @author Vlad Zhukovskiy
 */
public class ProjectReferenceNode extends ResourceBasedNode<ProjectReference> implements HasAction, HasStorablePath {

    private final ProjectReferenceProcessor resourceProcessor;

    @Inject
    public ProjectReferenceNode(@Assisted ProjectReference projectReference,
                                @Assisted ProjectDescriptor projectDescriptor,
                                @Assisted NodeSettings nodeSettings,
                                @NotNull EventBus eventBus,
                                @NotNull NodeManager nodeManager,
                                @NotNull ProjectReferenceProcessor resourceProcessor) {
        super(projectReference, projectDescriptor, nodeSettings, eventBus, nodeManager);
        this.resourceProcessor = resourceProcessor;
    }

    @NotNull
    @Override
    protected Promise<List<Node>> getChildrenImpl() {
        return Promises.resolve(Collections.<Node>emptyList());
    }

    @Override
    public void updatePresentation(@NotNull NodePresentation presentation) {
        presentation.setPresentableText(getData().getName());
        presentation.setPresentableIcon(isValid(getData()) ? nodeManager.getNodesResources().projectRoot()
                                                           : nodeManager.getNodesResources().invalidProjectRoot());
        if ("private".equals(getData().getVisibility())) {
            presentation.setInfoText("private");
            presentation.setInfoTextWrapper(Pair.of("[", "]"));
        }
    }

    @NotNull
    @Override
    public String getName() {
        return getData().getName();
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Nullable
    @Override
    public DeleteProcessor<ProjectReference> getDeleteProcessor() {
        return resourceProcessor;
    }

    @Nullable
    @Override
    public RenameProcessor<ProjectReference> getRenameProcessor() {
        return resourceProcessor;
    }

    @Override
    public void actionPerformed() {
        eventBus.fireEvent(new OpenProjectEvent(getData().getName()));
    }

    @NotNull
    @Override
    public String getStorablePath() {
        return getData().getPath();
    }

    private boolean isValid(ProjectReference reference) {
        return reference.getProblems().isEmpty();
    }
}
