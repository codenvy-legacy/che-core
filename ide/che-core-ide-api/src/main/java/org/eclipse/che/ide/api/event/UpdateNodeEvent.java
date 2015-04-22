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

/**
 * @author Andrienko Alexander
 */
public class UpdateNodeEvent extends GwtEvent<UpdateNodeEventHandler> {

    public static Type<UpdateNodeEventHandler> TYPE = new Type<>();

    @Override
    public Type<UpdateNodeEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(UpdateNodeEventHandler handler) {
        handler.onNodeUpdated();
    }
}
