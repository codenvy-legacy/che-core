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
package org.eclipse.che.security.oauth;

import com.google.gwt.user.client.Window;

/**
 * @author Vladislav Zhukovskii
 */
public class JsOAuthWindow {
    private String        authUrl;
    private String        errUrl;
    private OAuthStatus   authStatus;
    private int           popupHeight;
    private int           popupWidth;
    private int           clientHeight;
    private int           clientWidth;
    private OAuthCallback callback;

    public JsOAuthWindow(String authUrl, String errUrl, int popupHeight, int popupWidth, OAuthCallback callback) {
        this.authUrl = authUrl;
        this.errUrl = errUrl;
        this.popupHeight = popupHeight;
        this.popupWidth = popupWidth;
        this.clientHeight = Window.getClientHeight();
        this.clientWidth = Window.getClientWidth();
        this.callback = callback;
    }

    public void setAuthenticationStatus(int value) {
        authStatus = OAuthStatus.fromValue(value);
        if (callback != null) {
            this.callback.onAuthenticated(authStatus);
        }
    }

    public OAuthStatus getAuthenticationStatus() {
        return authStatus;
    }

    public void loginWithOAuth() {
        loginWithOAuth(authUrl, errUrl, popupHeight, popupWidth, clientHeight, clientWidth);
    }

    private native void loginWithOAuth(String authUrl, String errUrl, int popupHeight, int popupWidth, int clientHeight,
                                       int clientWidth) /*-{
        var instance = this;

        function Popup(authUrl, errorPageUrl, popupWindowWidth, popupWindowHeight) {
            this.authUrl = authUrl;
            this.errorPageUrl = errorPageUrl;
            this.popupWindowWidth = popupWindowWidth;
            this.popupWindowHeight = popupWindowHeight;

            var popup_close_handler = function () {
                if (!popupWindow || popupWindow.closed) {
                    instance.@org.eclipse.che.security.oauth.JsOAuthWindow::setAuthenticationStatus(I)(1);
                    popupWindow = null;
                    if (popupCloseHandlerIntervalId) {
                        window.clearInterval(popupCloseHandlerIntervalId);
                    }
                } else {
                    var href;
                    try {
                        href = popupWindow.location.href;
                    } catch (error) {
                    }

                    if (href) {
                        var path = popupWindow.location.pathname;
                        if (path == ("/ws/" + $wnd.IDE.config.workspaceName) || path == "/dashboard/") {
                            instance.@org.eclipse.che.security.oauth.JsOAuthWindow::setAuthenticationStatus(I)(3);
                            popupWindow.close();
                            popupWindow = null;
                            if (popupCloseHandlerIntervalId) {
                                window.clearInterval(popupCloseHandlerIntervalId);
                            }
                        } else if (path.match("j_security_check$")) {
                            instance.@org.eclipse.che.security.oauth.JsOAuthWindow::setAuthenticationStatus(I)(2);
                            if (!errorFlag) {
                                errorFlag = true;
                                popupWindow.location.replace(errorPageUrl);
                            }

                        }
                    }
                }
            }

            this.open_window = function () {
                var x = Math.max(0, Math.round(clientWidth / 2)
                    - Math.round(this.popupWindowWidth / 2));
                var y = Math.max(0, Math.round(clientHeight / 2)
                    - Math.round(this.popupWindowHeight / 2));
                popupWindow = window.open(this.authUrl, 'popup', 'width='
                    + this.popupWindowWidth + ',height='
                    + this.popupWindowHeight + ',left=' + x + ',top=' + y);
                popupCloseHandlerIntervalId = window.setInterval(
                    popup_close_handler, 80);
                errorFlag = false;
            }
        }

        var popup = new Popup(authUrl, errUrl, popupWidth, popupHeight);
        popup.open_window();
    }-*/;

}
