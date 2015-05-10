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
package org.eclipse.che.ide.util;

import org.eclipse.che.api.workspace.shared.dto.WorkspaceDescriptor;

/**
 * A smattering of useful methods.
 *
 * @author Dmytro Nochevnov
 * @author Vitaliy Guliy
 */
@Deprecated
public class Config {

    private static WorkspaceDescriptor _workspace;

    /**
     * Returns the base context of the IDE.
     * Is used to give IDE an ability to build valid URL when using history.
     * Valid IDE url looks like
     * "/ide-context/workspace-name/project-name"
     * and can be got by code below
     * Config.getContext() + "/" + Config.getWorkspaceName() + "/" + Config.getProjectName()
     *
     * @return
     */
    public static native String getContext() /*-{
        if ($wnd.IDE && $wnd.IDE.config) {
            return $wnd.IDE.config.context;
        } else {
            return null;
        }
    }-*/;


    public static native String getRestContext() /*-{
        if ($wnd.IDE && $wnd.IDE.config) {
            return $wnd.IDE.config.restContext;
        } else {
            return null;
        }
    }-*/;


    /**
     * Returns workspace name
     *
     * @return
     */
    public static native String getWorkspaceName() /*-{
        if ($wnd.IDE && $wnd.IDE.config) {
            return $wnd.IDE.config.workspaceName;
        } else {
            return null;
        }
    }-*/;


    /**
     * Returns workspace ID
     *
     * @return
     */
    public static native String getWorkspaceId() /*-{
        if ($wnd.IDE && $wnd.IDE.config) {
            return $wnd.IDE.config.workspaceId;
        } else {
            return null;
        }
    }-*/;


    /**
     * Returns project name
     *
     * @return
     */
    public static native String getProjectName() /*-{
        if ($wnd.IDE && $wnd.IDE.config) {
            return $wnd.IDE.config.projectName;
        } else {
            return null;
        }
    }-*/;


    public static native String getStartupParams() /*-{
        if ($wnd.IDE && $wnd.IDE.config && $wnd.IDE.config.startupParams) {
            // remove leading question marks
            while ($wnd.IDE.config.startupParams.indexOf("?") == 0) {
                $wnd.IDE.config.startupParams = $wnd.IDE.config.startupParams.substring(1);
            }

            return $wnd.IDE.config.startupParams;
        } else {
            return null;
        }
    }-*/;


    public static native String getStartupParam(String name) /*-{
        if ($wnd.IDE && $wnd.IDE.config && $wnd.IDE.config.startupParams) {
            // remove leading question marks
            while ($wnd.IDE.config.startupParams.indexOf("?") == 0) {
                $wnd.IDE.config.startupParams = $wnd.IDE.config.startupParams.substring(1);
            }

            var pairs = $wnd.IDE.config.startupParams.split("&");
            for (var i = 0; i < pairs.length; i++) {
                var pair = pairs[i].split('=');
                if (pair.length == 2 && decodeURIComponent(pair[0]) == name) {
                    return decodeURIComponent(pair[1]);
                }
            }
        }
        return null;
    }-*/;

    /**
     * The method defines current mode is sdk or not.
     *
     * @return <code>true</code> current mode is sdk, <code>false</code> current mode isn't sdk
     */
    public static native boolean isSdkProject()/*-{
        var isSdk = $wnd.sdk;

        if (!isSdk) {
            return false;
        }

        return isSdk == 1;
    }-*/;

    /**
     * Set current Workspace.
     *
     * @param workspace
     *         the Workspace to set
     */
    public static void setCurrentWorkspace(WorkspaceDescriptor workspace) {
        _workspace = workspace;
    }

    /**
     * Get current workspace information.
     *
     * @return workspace
     */
    public static WorkspaceDescriptor getCurrentWorkspace() {
        return _workspace;
    }
}
