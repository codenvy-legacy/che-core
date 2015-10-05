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
package org.eclipse.che.api.workspace.server;

/**
 * Constants for Workspace API
 *
 * @author Eugene Voevodin
 */
public final class Constants {

    public static final String LINK_REL_CREATE_WORKSPACE             = "create workspace";
    public static final String LINK_REL_GET_CURRENT_USER_WORKSPACES  = "current user workspaces";
    public static final String LINK_REL_GET_WORKSPACES_BY_ACCOUNT    = "all workspaces of given account";
    public static final String LINK_REL_GET_CONCRETE_USER_WORKSPACES = "concrete user workspaces";
    public static final String LINK_REL_GET_WORKSPACE_BY_ID          = "workspace by id";
    public static final String LINK_REL_GET_WORKSPACE_BY_NAME        = "workspace by name";
    public static final String LINK_REL_UPDATE_WORKSPACE_BY_ID       = "update by id";
    public static final String LINK_REL_GET_CURRENT_USER_MEMBERSHIP  = "get membership in given workspace";
    public static final String LINK_REL_GET_WORKSPACE_MEMBERS        = "get members";
    public static final String LINK_REL_ADD_WORKSPACE_MEMBER         = "add member";
    public static final String LINK_REL_REMOVE_WORKSPACE_MEMBER      = "remove member";
    public static final String LINK_REL_REMOVE_WORKSPACE             = "remove workspace";
    public static final String LINK_REL_CREATE_TEMP_WORKSPACE        = "create temp workspace";
    public static final String LINK_REL_ADD_ATTRIBUTE                = "add attribute";
    public static final String LINK_REL_REMOVE_ATTRIBUTE             = "remove attribute";
    public static final int    ID_LENGTH                             = 16;
    public static final String RESOURCES_USAGE_LIMIT_PROPERTY        = "codenvy:resources_usage_limit";
    public static final String LINK_REL_START_WORKSPACE              = "start workspace";
    public static final String LINK_REL_GET_RUNTIMEWORKSPACE         = "get runtime workspace";
    public static final String STOP_WORKSPACE                        = "stop workspace";
    public static final String GET_USERS_WORKSPACE                   = "get users workspace";
    public static final String GET_ALL_USER_WORKSPACES               = "get all user workspaces";

    private Constants() {
    }
}