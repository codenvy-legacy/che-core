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

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;

import org.eclipse.che.ide.api.project.node.Node;

/**
 * Event fires when need to update node presentation.
 *
 * @author Vlad Zhukovskiy
 */
public class UpdateNodePresentationEvent extends GwtEvent<UpdateNodePresentationEvent.UpdateNodePresentationHandler> {

    public interface UpdateNodePresentationHandler extends EventHandler {
        void onUpdateNodePresentation(UpdateNodePresentationEvent event);
    }

    public interface HasExpandItemHandlers {
        HandlerRegistration addExpandHandler(UpdateNodePresentationHandler handler);
    }

    private static Type<UpdateNodePresentationHandler> TYPE;

    public static Type<UpdateNodePresentationHandler> getType() {
        if (TYPE == null) {
            TYPE = new Type<>();
        }
        return TYPE;
    }

    private Node node;

    public UpdateNodePresentationEvent(Node node) {
        this.node = node;
    }

    @Override
    public Type<UpdateNodePresentationHandler> getAssociatedType() {
        return TYPE;
    }

    public Node getNode() {
        return node;
    }

    /** {@inheritDoc} */
    @Override
    protected void dispatch(UpdateNodePresentationHandler handler) {
        handler.onUpdateNodePresentation(this);
    }

}
