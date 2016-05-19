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

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.project.node.HasProjectDescriptor;
import org.eclipse.che.ide.api.project.node.AbstractTreeNode;
import org.eclipse.che.ide.api.project.node.HasDataObject;
import org.eclipse.che.ide.api.project.node.settings.HasSettings;
import org.eclipse.che.ide.api.project.node.settings.NodeSettings;
import org.eclipse.che.ide.ui.smartTree.presentation.HasPresentation;
import org.eclipse.che.ide.ui.smartTree.presentation.NodePresentation;

import javax.validation.constraints.NotNull;

/**
 * Base class for the project related nodes.
 *
 * @author Vlad Zhukovskiy
 */
public abstract class AbstractProjectBasedNode<DataObject> extends AbstractTreeNode implements HasDataObject<DataObject>,
                                                                                               HasPresentation,
                                                                                               HasProjectDescriptor,
                                                                                               HasSettings {
    private DataObject        dataObject;
    private ProjectDescriptor projectDescriptor;
    private NodeSettings      nodeSettings;
    private NodePresentation  nodePresentation;

    public AbstractProjectBasedNode(@NotNull DataObject dataObject,
                                    @NotNull ProjectDescriptor projectDescriptor,
                                    @NotNull NodeSettings nodeSettings) {
        this.dataObject = dataObject;
        this.projectDescriptor = projectDescriptor;
        this.nodeSettings = nodeSettings;
    }

    @NotNull
    @Override
    public ProjectDescriptor getProjectDescriptor() {
        return projectDescriptor;
    }

    @Override
    public void setProjectDescriptor(@NotNull ProjectDescriptor projectDescriptor) {
        this.projectDescriptor = projectDescriptor;
    }

    @Override
    public NodeSettings getSettings() {
        return nodeSettings;
    }

    @NotNull
    @Override
    public DataObject getData() {
        return dataObject;
    }

    @Override
    public void setData(@NotNull DataObject data) {
        this.dataObject = data;
    }

    @Override
    public final NodePresentation getPresentation(boolean update) {
        if (nodePresentation == null) {
            nodePresentation = new NodePresentation();
            updatePresentation(nodePresentation);
        }

        if (update) {
            updatePresentation(nodePresentation);
        }
        return nodePresentation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractProjectBasedNode)) return false;

        AbstractProjectBasedNode that = (AbstractProjectBasedNode)o;

        if (!dataObject.equals(that.dataObject)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return dataObject.hashCode();
    }
}
