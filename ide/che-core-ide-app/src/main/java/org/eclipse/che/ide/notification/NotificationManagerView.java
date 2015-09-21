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
package org.eclipse.che.ide.notification;

import org.eclipse.che.ide.api.mvp.View;
import org.eclipse.che.ide.api.parts.base.BaseActionDelegate;

/**
 * The view of {@link NotificationManagerImpl}.
 *
 * @author Andrey Plotnikov
 */
public interface NotificationManagerView extends View<NotificationManagerView.ActionDelegate> {
    /** Required for delegating some functions in view. */
    interface ActionDelegate extends BaseActionDelegate {
    }

    /**
     * Status of a notification manager. The manager has 3 statuses: manager has unread messages, manager has at least one message in
     * progress and manager has no new messages
     */
    enum Status {
        IN_PROGRESS, EMPTY, HAS_UNREAD
    }

    void setContainer(NotificationContainer container);

    void setVisible(boolean visible);

    /**
     * Set title of event log part.
     *
     * @param title
     *         title that need to be set
     */
    void setTitle(String title);

    /**
     * Scrolls the view to the bottom.
     */
    void scrollBottom();

}
