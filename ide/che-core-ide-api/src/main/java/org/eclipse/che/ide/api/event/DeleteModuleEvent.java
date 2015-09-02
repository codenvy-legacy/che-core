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
package org.eclipse.che.ide.api.event;

import com.google.gwt.event.shared.GwtEvent;

import org.eclipse.che.ide.api.project.tree.generic.ProjectNode;

import javax.validation.constraints.NotNull;

/**
 * The event should be fired when we delete module from project
 * This event was created because we can't use ItemEvent for ProjectNode.
 * We have treeNode hierarchy where {@link org.eclipse.che.ide.api.project.tree.generic.ProjectNode}
 * and {@link org.eclipse.che.ide.api.project.tree.generic.ItemNode} are different inheritors of parent
 * {@link org.eclipse.che.ide.api.project.tree.AbstractTreeNode}
 *
 * @author Alexander Andrienko
 */
public class DeleteModuleEvent extends GwtEvent<DeleteModuleEventHandler> {

    public static Type<DeleteModuleEventHandler> TYPE = new Type<>();
    private ProjectNode module;

    public DeleteModuleEvent(@NotNull ProjectNode module) {
        this.module = module;
    }

    @Override
    public Type<DeleteModuleEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(DeleteModuleEventHandler handler) {
        handler.onModuleDeleted(this);
    }

    /**
     * Returns deleted module node
     * @return deleted module node
     */
    @NotNull
    public ProjectNode getModule() {
        return module;
    }
}
