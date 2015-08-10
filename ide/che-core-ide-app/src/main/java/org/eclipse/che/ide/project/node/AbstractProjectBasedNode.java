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

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.project.node.HasProjectDescriptor;
import org.eclipse.che.ide.api.project.node.AbstractTreeNode;
import org.eclipse.che.ide.api.project.node.HasDataObject;
import org.eclipse.che.ide.api.project.node.settings.HasSettings;
import org.eclipse.che.ide.api.project.node.settings.NodeSettings;
import org.eclipse.che.ide.ui.smartTree.presentation.HasPresentation;
import org.eclipse.che.ide.ui.smartTree.presentation.NodePresentation;

import javax.annotation.Nonnull;

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

    public AbstractProjectBasedNode(@Nonnull DataObject dataObject,
                                    @Nonnull ProjectDescriptor projectDescriptor,
                                    @Nonnull NodeSettings nodeSettings) {
        this.dataObject = dataObject;
        this.projectDescriptor = projectDescriptor;
        this.nodeSettings = nodeSettings;
        this.nodePresentation = new NodePresentation();
    }

    @Nonnull
    @Override
    public ProjectDescriptor getProjectDescriptor() {
        return projectDescriptor;
    }

    @Override
    public void setProjectDescriptor(@Nonnull ProjectDescriptor projectDescriptor) {
        this.projectDescriptor = projectDescriptor;
    }

    @Override
    public NodeSettings getSettings() {
        return nodeSettings;
    }

    @Nonnull
    @Override
    public DataObject getData() {
        return dataObject;
    }

    @Override
    public void setData(@Nonnull DataObject data) {
        this.dataObject = data;
    }

    @Override
    public final NodePresentation getPresentation() {
        return nodePresentation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractProjectBasedNode that = (AbstractProjectBasedNode)o;

        if (!dataObject.equals(that.dataObject)) return false;
        if (!projectDescriptor.equals(that.projectDescriptor)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = dataObject.hashCode();
        result = 31 * result + projectDescriptor.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "AbstractProjectBasedNode{" +
               "dataObject=" + dataObject +
               ", projectDescriptor=" + projectDescriptor +
               '}';
    }
}
