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
package org.eclipse.che.ide.project.event;

import com.google.common.annotations.Beta;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

import org.eclipse.che.ide.api.project.node.Node;

/**
 * Event fires when need to synchronize the state of project's displayed state.
 * E.g. reload existed rendered children.
 *
 * @author Vlad Zhukovskiy
 */
@Beta
public class SynchronizeProjectViewEvent extends GwtEvent<SynchronizeProjectViewEvent.SynchronizeProjectViewHandler> {
    public interface SynchronizeProjectViewHandler extends EventHandler {
        void onProjectViewSynchronizeEvent(SynchronizeProjectViewEvent event);
    }

    private static Type<SynchronizeProjectViewHandler> TYPE;

    public static Type<SynchronizeProjectViewHandler> getType() {
        if (TYPE == null) {
            TYPE = new Type<>();
        }
        return TYPE;
    }

    private Node  node;
    private Class byClass;

    public SynchronizeProjectViewEvent() {
    }

    public SynchronizeProjectViewEvent(Node node) {
        this.node = node;
    }

    public SynchronizeProjectViewEvent(Class byClass) {
        this.byClass = byClass;
    }

    public Node getNode() {
        return node;
    }

    public Class getByClass() {
        return byClass;
    }

    @Override
    public Type<SynchronizeProjectViewHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(SynchronizeProjectViewHandler handler) {
        handler.onProjectViewSynchronizeEvent(this);
    }
}
