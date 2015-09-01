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

import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.project.node.settings.NodeSettings;
import org.eclipse.che.ide.project.node.resource.ItemReferenceProcessor;
import org.eclipse.che.ide.ui.smartTree.presentation.NodePresentation;

import javax.annotation.Nonnull;

/**
 * @author Vlad Zhukovskiy
 */
public class FolderReferenceNode extends ItemReferenceBasedNode {
    @Inject
    public FolderReferenceNode(@Assisted ItemReference itemReference,
                               @Assisted ProjectDescriptor projectDescriptor,
                               @Assisted NodeSettings nodeSettings,
                               @Nonnull EventBus eventBus,
                               @Nonnull NodeManager nodeManager,
                               @Nonnull ItemReferenceProcessor resourceProcessor) {
        super(itemReference, projectDescriptor, nodeSettings, eventBus, nodeManager, resourceProcessor);
    }

    @Override
    public void updatePresentation(@Nonnull NodePresentation presentation) {
        presentation.setPresentableText(getData().getName());
        presentation.setPresentableIcon(nodeManager.getNodesResources().simpleRoot());
    }

    @Override
    public boolean supportGoInto() {
        return true;
    }
}
