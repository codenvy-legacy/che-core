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

import com.google.gwt.user.client.Window;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.api.notification.Notification;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The graphic container for {@link NotificationMessage}. Provides showing notification message on display.
 *
 * @author <a href="mailto:aplotnikov@codenvy.com">Andrey Plotnikov</a>
 */
@Singleton
public class NotificationMessageStack implements NotificationMessage.ActionDelegate {
    /** Required for delegating some functions in view. */
    public interface ActionDelegate {
        /**
         * Performs some actions in response to a user's opening a notification.
         *
         * @param notification
         *         notification that is tried opening
         */
        void onOpenMessageClicked(@Nonnull Notification notification);

        /**
         * Performs some actions in response to a user's closing a notification.
         *
         * @param notification
         *         notification that is tried closing
         */
        void onCloseMessageClicked(@Nonnull Notification notification);
    }

    public static final int POPUP_COUNT = 3;
    private Resources                              resources;
    private ActionDelegate                         delegate;
    private Map<Notification, NotificationMessage> notificationMessage;
    private List<NotificationMessage>              messages;

    /**
     * Create message stack.
     *
     * @param resources
     */
    @Inject
    public NotificationMessageStack(Resources resources) {
        this.resources = resources;
        this.notificationMessage = new HashMap<>();
        this.messages = new ArrayList<>();
    }

    /** Sets the delegate for receiving events from this view. */
    public void setDelegate(@Nonnull ActionDelegate delegate) {
        this.delegate = delegate;
    }

    /**
     * Add notification to message stack.
     *
     * @param notification
     *         notification that need to add
     */
    public void addNotification(@Nonnull Notification notification) {
        NotificationMessage message = new NotificationMessage(resources, notification, this);
        notificationMessage.put(notification, message);
        messages.add(message);
        showMessage();
    }

    /** Show notification message. */
    private void showMessage() {
        int left = Window.getClientWidth() - NotificationMessage.WIDTH - 30;
        for (int i = 0, top = 53; i < POPUP_COUNT && i < messages.size(); i++, top += NotificationMessage.HEIGHT + 20) {
            NotificationMessage popup = messages.get(i);
            if (popup.isShowing()) {
                popup.setPopupPosition(left, top);
            } else {
                popup.show(left, top);
            }
        }
    }

    /**
     * Remove notification from message stack.
     *
     * @param notification
     *         notification that need to remove
     */
    public void removeNotification(@Nonnull Notification notification) {
        NotificationMessage message = notificationMessage.remove(notification);
        message.hide();
    }

    /** {@inheritDoc} */
    @Override
    public void onOpenMessageClicked(@Nonnull Notification notification) {
        delegate.onOpenMessageClicked(notification);
    }

    /** {@inheritDoc} */
    @Override
    public void onCloseMessageClicked(@Nonnull Notification notification) {
        NotificationMessage message = notificationMessage.get(notification);
        message.hide();
        delegate.onCloseMessageClicked(notification);
    }

    /** {@inheritDoc} */
    @Override
    public void onClosingDialog(@Nonnull NotificationMessage message) {
        messages.remove(message);
        showMessage();
    }

    public void clear() {
        for (NotificationMessage notification : notificationMessage.values()) {
            notification.hide();
        }
        notificationMessage.clear();
        messages.clear();
    }
}