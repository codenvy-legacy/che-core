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

import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.ui.smartTree.event.SelectionChangedEvent.SelectionChangedHandler;

import java.util.Collections;
import java.util.List;

/**
 * Event fires after the selection changes.
 *
 * @author Vlad Zhukovskiy
 */
public class SelectionChangedEvent extends GwtEvent<SelectionChangedHandler> {

    public interface HasSelectionChangedHandlers {
        HandlerRegistration addSelectionChangedHandler(SelectionChangedHandler handler);
    }

    public interface SelectionChangedHandler extends EventHandler {
        void onSelectionChanged(SelectionChangedEvent event);
    }

    private static Type<SelectionChangedHandler> TYPE;

    public static Type<SelectionChangedHandler> getType() {
        if (TYPE == null) {
            TYPE = new Type<>();
        }
        return TYPE;
    }

    private List<Node> selection;

    public SelectionChangedEvent(List<Node> selection) {
        this.selection = Collections.unmodifiableList(selection);
    }

    @Override
    public Type<SelectionChangedHandler> getAssociatedType() {
        return TYPE;
    }

    public List<Node> getSelection() {
        return selection;
    }

    /** {@inheritDoc} */
    @Override
    protected void dispatch(SelectionChangedHandler handler) {
        handler.onSelectionChanged(this);
    }

}
