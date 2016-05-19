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
package org.eclipse.che.ide.jseditor.client.events;

import com.google.gwt.event.shared.GwtEvent;
/**
 * Events that correspond to external completion requests.
 */
public class CompletionRequestEvent extends GwtEvent<CompletionRequestHandler> {

    /** The type instance for this event. */
    public static final Type<CompletionRequestHandler> TYPE = new Type<>();

    @Override
    public Type<CompletionRequestHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(final CompletionRequestHandler handler) {
        handler.onCompletionRequest(this);
    }
}
