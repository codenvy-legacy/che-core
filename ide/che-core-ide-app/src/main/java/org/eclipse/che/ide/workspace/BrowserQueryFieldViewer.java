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
package org.eclipse.che.ide.workspace;

import com.google.gwt.user.client.Window;
import com.google.inject.Singleton;

/**
 * The class contains business logic which allows get or set workspace name to query field in browser.
 *
 * @author Dmitry Shnurenko
 */
@Singleton
public class BrowserQueryFieldViewer {

    private static final int WORKSPACE_ORDER_IN_URL = 2;

    /**
     * Sets workspace name to query field in browser.
     *
     * @param workspaceName
     *         name which will be set
     */
    public native void setWorkspaceName(String workspaceName) /*-{
        try {
            var window = $wnd;
            var document = $doc;

            if (!window["_history_relocation_id"]) {
                window["_history_relocation_id"] = 0;
            }

            var isHostedVersion = window.location.pathname.indexOf("/ws/") > -1;

            var url = isHostedVersion ? "/ws/" + workspaceName : "/che/" + workspaceName;

            document.title = "Codenvy Developer Environment";
            window.history.pushState(window["_history_relocation_id"], document.title, url);
            window["_history_relocation_id"]++;
        } catch (e) {
            console.log(e.message);
        }
    }-*/;

    /**
     * Sets project name to query field in browser.
     *
     * @param projectName
     *         name which will be set
     */
    public native void setProjectName(String projectName) /*-{
        try {
            var window = $wnd;
            var document = $doc;

            if (!window["_history_relocation_id"]) {
                window["_history_relocation_id"] = 0;
            }

            var browserUrl = window.location.pathname;

            var lastIndex = browserUrl.lastIndexOf("/");

            var url = browserUrl.substring(lastIndex);

            document.title = "Codenvy Developer Environment";
            window.history.pushState(window["_history_relocation_id"], document.title, url + "/" + projectName);
            window["_history_relocation_id"]++;
        } catch (e) {
            console.log(e.message);
        }
    }-*/;

    /** Returns workspace name from browser query fiels */
    public String getWorkspaceName() {
        String browserUrl = Window.Location.getPath();

        String[] urlParts = browserUrl.split("/");

        return urlParts.length < 3 ? "" : urlParts[WORKSPACE_ORDER_IN_URL];
    }
}
