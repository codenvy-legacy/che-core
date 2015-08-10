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

import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.api.event.FileEvent;
import org.eclipse.che.ide.api.project.node.HasProjectDescriptor;
import org.eclipse.che.ide.api.project.node.settings.NodeSettings;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.api.project.node.HasAction;
import org.eclipse.che.ide.project.node.resource.ItemReferenceProcessor;
import org.eclipse.che.ide.ui.smartTree.presentation.NodePresentation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Vlad Zhukovskiy
 */
public class FileReferenceNode extends ItemReferenceBasedNode implements VirtualFile, HasAction {

    public static final String GET_CONTENT_REL = "get content";

    @Inject
    public FileReferenceNode(@Assisted ItemReference itemReference,
                             @Assisted ProjectDescriptor projectDescriptor,
                             @Assisted NodeSettings nodeSettings,
                             @Nonnull EventBus eventBus,
                             @Nonnull ResourceNodeManager resourceNodeManager,
                             @Nonnull ItemReferenceProcessor resourceProcessor) {
        super(itemReference, projectDescriptor, nodeSettings, eventBus, resourceNodeManager, resourceProcessor);
    }

    @Override
    public void updatePresentation(@Nonnull NodePresentation presentation) {
        presentation.setPresentableText(getData().getName());
        presentation.setPresentableIcon(resourceNodeManager.getNodesResources().file());
    }

    @Nonnull
    @Override
    public String getPath() {
        return getData().getPath();
    }

    @Override
    public String getDisplayName() {
        return getData().getName();
    }

    @Nullable
    @Override
    public String getMediaType() {
        return getData().getMediaType();
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Nullable
    @Override
    public HasProjectDescriptor getProject() {
        return this;
    }

    @Override
    public String getContentUrl() {
        Link link = getData().getLink(GET_CONTENT_REL);

        return link == null ? null : link.getHref();
    }

    @Override
    public Promise<Void> updateContent(String content) {
        return resourceNodeManager.updateContent(this, content);
    }

    @Override
    public void actionPerformed() {
        eventBus.fireEvent(new FileEvent(this, FileEvent.FileOperation.OPEN));
    }

    @Override
    public Promise<String> getContent() {
        return resourceNodeManager.getContent(this);
    }
}
