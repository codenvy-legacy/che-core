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
package org.eclipse.che.ide.ui.smartTree.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;

import org.eclipse.che.ide.ui.smartTree.event.FocusEvent.FocusHandler;

/**
 * Fires after a widget is focused. Unlike the GWT {@link com.google.gwt.event.dom.client.FocusEvent},
 * this event is NOT a dom event to allow components flexibility in when the focus event is fired.
 *
 * @author Vlad Zhukovskiy
 */
public class FocusEvent extends GwtEvent<FocusHandler> {

    public interface FocusHandler extends EventHandler {
        void onFocus(FocusEvent event);
    }

    public interface HasFocusHandlers {
        HandlerRegistration addFocusHandler(FocusHandler handler);
    }

    private static Type<FocusHandler> TYPE;

    public static Type<FocusHandler> getType() {
        if (TYPE == null) {
            TYPE = new Type<>();
        }
        return TYPE;
    }

    @Override
    public Type<FocusHandler> getAssociatedType() {
        return TYPE;
    }

    /** {@inheritDoc} */
    @Override
    protected void dispatch(FocusHandler handler) {
        handler.onFocus(this);
    }
}
