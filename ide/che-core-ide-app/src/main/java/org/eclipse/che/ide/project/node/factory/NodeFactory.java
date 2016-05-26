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
package org.eclipse.che.ide.project.node.factory;

import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.project.shared.dto.ProjectReference;
import org.eclipse.che.ide.api.project.node.settings.NodeSettings;
import org.eclipse.che.ide.project.node.FileReferenceNode;
import org.eclipse.che.ide.project.node.FolderReferenceNode;
import org.eclipse.che.ide.project.node.ModuleDescriptorNode;
import org.eclipse.che.ide.project.node.ProjectDescriptorNode;
import org.eclipse.che.ide.project.node.ProjectReferenceNode;

import javax.validation.constraints.NotNull;

/**
 * Factory that helps to create nodes.
 *
 * @author Vlad Zhukovskiy
 */
public interface NodeFactory {
    /**
     * Creates project node that represent opened project.
     *
     * @param projectDescriptor
     *         instance of {@link ProjectDescriptor} related to this node
     * @param nodeSettings
     *         node view settings
     * @return instance of {@link ProjectDescriptorNode}
     */
    ProjectDescriptorNode newProjectDescriptorNode(@NotNull ProjectDescriptor projectDescriptor,
                                                   @NotNull NodeSettings nodeSettings);

    /**
     * Creates project node that represent project in project list.
     *
     * @param projectReference
     *         instance of {@link ProjectReference} related to this node
     * @param projectDescriptor
     *         instance of {@link ProjectDescriptor} related to this node
     * @param nodeSettings
     *         node view settings
     * @return instance of {@link ProjectReferenceNode}
     */
    ProjectReferenceNode newProjectReferenceNode(@NotNull ProjectReference projectReference,
                                                 @NotNull ProjectDescriptor projectDescriptor,
                                                 @NotNull NodeSettings nodeSettings);

    /**
     * Creates module node that represent project module.
     *
     * @param projectDescriptor
     *         instance of {@link ProjectDescriptor} related to this node
     * @param nodeSettings
     *         node view settings
     * @return instance of {@link ModuleDescriptorNode}
     */
    ModuleDescriptorNode newModuleNode(@NotNull ProjectDescriptor projectDescriptor,
                                       @NotNull NodeSettings nodeSettings);

    /**
     * Creates folder referenced node.
     *
     * @param itemReference
     *         instance of {@link ItemReference} related to this node
     * @param projectDescriptor
     *         instance of {@link ProjectDescriptor} related to this node
     * @param nodeSettings
     *         node view settings
     * @return instance of {@link FolderReferenceNode}
     */
    FolderReferenceNode newFolderReferenceNode(@NotNull ItemReference itemReference,
                                               @NotNull ProjectDescriptor projectDescriptor,
                                               @NotNull NodeSettings nodeSettings);

    /**
     * Creates file referenced node.
     *
     * @param itemReference
     *         instance of {@link ItemReference} related to this node
     * @param projectDescriptor
     *         instance of {@link ProjectDescriptor} related to this node
     * @param nodeSettings
     *         node view settings
     * @return instance of {@link FileReferenceNode}
     */
    FileReferenceNode newFileReferenceNode(@NotNull ItemReference itemReference,
                                           @NotNull ProjectDescriptor projectDescriptor,
                                           @NotNull NodeSettings nodeSettings);
}
