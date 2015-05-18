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
package org.eclipse.che.ide.websocket;

import com.google.gwt.user.client.Window;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Getting setting by native JS
 * $wnd.IDE.config.restContext
 *
 * @author Vitaly Parfonov
 */
public class WebSocketUrlProvider implements Provider<String> {

    @Inject


    @Override
    public String get() {
        boolean isSecureConnection = Window.Location.getProtocol().equals("https:");
        return (isSecureConnection ? "wss://" : "ws://") + Window.Location.getHost() + getRestContext() + "/ws/" + getWorkspaceId();
    }

    private static native String getRestContext() /*-{
        if ($wnd.IDE && $wnd.IDE.config) {
            return $wnd.IDE.config.restContext;
        } else {
            return null;
        }
    }-*/;


    /**
     * Returns workspace ID
     *
     * @return
     */
    private static native String getWorkspaceId() /*-{
        if ($wnd.IDE && $wnd.IDE.config) {
            return $wnd.IDE.config.workspaceId;
        } else {
            return null;
        }
    }-*/;
}
