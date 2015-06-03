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

import com.google.gwt.event.shared.EventHandler;

/**
 * A handler for handling {@link RestoreProjectTreeStateEvent}.
 *
 * @author Roman Nikitenko
 */
public interface RestoreProjectTreeStateHandler extends EventHandler {
    /**
     * Called when the project tree state should be restored.
     *
     * @param event
     *         the fired {@link RestoreProjectTreeStateEvent}
     */
    void onRestore(RestoreProjectTreeStateEvent event);
}
