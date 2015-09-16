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
package org.eclipse.che.ide.ui.smartTree.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.ui.smartTree.event.GoIntoStateEvent.GoIntoStateHandler ;

/**
 * Fires after a widget is blurred. Unlike the GWT {@link com.google.gwt.event.dom.client.BlurEvent},
 * this event is NOT a dom event to allow components flexibility in when the focus event is fired.
 *
 * @author Vlad Zhukovskiy
 */
public class GoIntoStateEvent extends GwtEvent<GoIntoStateHandler> {

    public interface GoIntoStateHandler extends EventHandler {
        void onGoIntoStateChanged(GoIntoStateEvent event);
    }

    public interface HasGoIntoStateHandlers extends HasHandlers {
        HandlerRegistration addGoIntoHandler(GoIntoStateHandler handler);
    }

    private static Type<GoIntoStateHandler> TYPE;

    public static Type<GoIntoStateHandler> getType() {
        if (TYPE == null) {
            TYPE = new Type<>();
        }
        return TYPE;
    }

    public enum State {
        ACTIVATED, DEACTIVATED
    }

    private State state;
    private Node node;

    public GoIntoStateEvent(State state, Node node) {
        this.state = state;
        this.node = node;
    }

    public State getState() {
        return state;
    }

    public Node getNode() {
        return node;
    }

    @Override
    public Type<GoIntoStateHandler> getAssociatedType() {
        return TYPE;
    }

    /** {@inheritDoc} */
    @Override
    protected void dispatch(GoIntoStateHandler handler) {
        handler.onGoIntoStateChanged(this);
    }

}
