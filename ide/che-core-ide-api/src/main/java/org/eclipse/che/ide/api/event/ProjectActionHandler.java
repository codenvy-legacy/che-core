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
 * A handler for handling {@link ProjectActionEvent}.
 *
 * @author Nikolay Zamosenchuk
 */
public interface ProjectActionHandler extends EventHandler {
    /**
     * Called on project open.
     *
     * @param event
     *         the fired {@link ProjectActionEvent}
     */
    void onProjectOpened(ProjectActionEvent event);

    /**
     * Called on project close before any closing activities.
     *
     * @param event
     *         the fired {@link ProjectActionEvent}
     */
    void onProjectClosing(ProjectActionEvent event);

    /**
     * Called on project close after performing all closing activities.
     *
     * @param event
     *         the fired {@link ProjectActionEvent}
     */
    void onProjectClosed(ProjectActionEvent event);
}
