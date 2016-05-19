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
package org.eclipse.che.ide.api.event;

import com.google.gwt.event.shared.EventHandler;

/**
 * A handler for handling {@link PersistProjectTreeStateEvent}.
 *
 * @author Roman Nikitenko
 */
public interface PersistProjectTreeStateHandler extends EventHandler {
    /**
     * Called when the project state should be persisted.
     *
     * @param event
     *         the fired {@link PersistProjectTreeStateEvent}
     */
    void onPersist(PersistProjectTreeStateEvent event);
}
