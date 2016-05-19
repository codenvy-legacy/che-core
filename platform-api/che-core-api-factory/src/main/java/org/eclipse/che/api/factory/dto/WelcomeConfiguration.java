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
package org.eclipse.che.api.factory.dto;

import org.eclipse.che.dto.shared.DTO;

/**
 * Welcome message configuration. Contains title, link for icon url, and link for content page e.g. HTML or something
 * else.
 * This configuration will be processed when user apply factory link. And shows in right side of IDE
 */
@Deprecated
@DTO
public interface WelcomeConfiguration {

    // Greeting title
    @Deprecated
    String getTitle();
    @Deprecated
    void setTitle(String title);

    WelcomeConfiguration withTitle(String title);

    // URL to greeting icon
    @Deprecated
    String getIconurl();
    @Deprecated
    void setIconurl(String iconurl);
    @Deprecated
    WelcomeConfiguration withIconurl(String iconurl);

    // URL to greeting page
    @Deprecated
    String getContenturl();
    @Deprecated
    void setContenturl(String contenturl);
    @Deprecated
    WelcomeConfiguration withContenturl(String contenturl);

    // Notification
    @Deprecated
    String getNotification();
    @Deprecated
    void setNotification(String notification);
    @Deprecated
    WelcomeConfiguration withNotification(String notification);

}
