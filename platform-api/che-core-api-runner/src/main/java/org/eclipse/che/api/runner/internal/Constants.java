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
package org.eclipse.che.api.runner.internal;

/** @author andrew00x */
public class Constants {
    // rels for known runner links
    public static final String LINK_REL_REGISTER_RUNNER_SERVER   = "register runner server";
    public static final String LINK_REL_UNREGISTER_RUNNER_SERVER = "unregister runner server";
    public static final String LINK_REL_REGISTERED_RUNNER_SERVER = "registered runner server";
    public static final String LINK_REL_RUNNER_TASKS             = "runner tasks";
    public static final String LINK_REL_AVAILABLE_RUNNERS        = "available runners";
    public static final String LINK_REL_SERVER_STATE             = "server state";
    public static final String LINK_REL_RUNNER_STATE             = "runner state";
    public static final String LINK_REL_RUNNER_ENVIRONMENTS      = "runner environments";
    public static final String LINK_REL_RUN                      = "run";
    public static final String LINK_REL_GET_STATUS               = "get status";
    public static final String LINK_REL_VIEW_LOG                 = "view logs";
    public static final String LINK_REL_WEB_URL                  = "web url";
    public static final String LINK_REL_SHELL_URL                = "shell url";
    public static final String LINK_REL_STOP                     = "stop";
    public static final String LINK_REL_RUNNER_RECIPE            = "runner recipe";
    public static final String LINK_REL_GET_RECIPE               = "get recipe";
    public static final String LINK_REL_GET_CURRENT_RECIPE       = "get current recipe";

    // config properties
    /**
     * Default size of memory in megabytes available for one workspace if workspace doesn't has own setting, see {@link
     * #RUNNER_MAX_MEMORY_SIZE}
     */
    public static final String RUNNER_WS_MAX_MEMORY_SIZE          = "runner.workspace.max_memsize";
    /** URLs of slave runners that should be registered in RunQueue (all runners are registered to 'community' infra). */
    public static final String RUNNER_SLAVE_RUNNER_URLS           = "runner.slave_runner_urls";
    /** URLs of slave runners for 'paid' infra. */
    public static final String RUNNER_SLAVE_RUNNER_URLS_PAID      = "runner.slave_runner_urls.paid";
    /** URLs of slave runners for 'always_on' infra. */
    public static final String RUNNER_SLAVE_RUNNER_URLS_ALWAYS_ON = "runner.slave_runner_urls.always_on";
    /**
     * Directory for deploy applications. All implementation of {@link Runner} create sub-directories in
     * this directory for deploying applications.
     */
    public static final String DEPLOY_DIRECTORY                   = "runner.deploy_directory";
    /** After this time all information about not running application may be removed. */
    public static final String APP_CLEANUP_TIME                   = "runner.cleanup_time";
    /** Default size of memory for application in megabytes. Value that is provided by this property may be overridden by user settings. */
    public static final String APP_DEFAULT_MEM_SIZE               = "runner.default_app_mem_size";
    /**
     * Max waiting time in seconds of application for the start. If process is not started after this time, it will be removed from the
     * queue.
     */
    public static final String WAITING_TIME                       = "runner.waiting_time";
    /**
     * Default lifetime in seconds of an application if workspace doesn't have own setting, see {@link #RUNNER_LIFETIME}. After this time
     * application may be terminated.
     */
    public static final String APP_LIFETIME                       = "runner.app_lifetime";
    /** Name of configuration parameter that sets amount of memory (in megabytes) for running applications. */
    public static final String TOTAL_APPS_MEM_SIZE                = "runner.total_apps_mem_size_mb";

    public static final String RUNNER_ASSIGNED_TO_WORKSPACE = "runner.assigned_to_workspace";
    public static final String RUNNER_ASSIGNED_TO_PROJECT   = "runner.assigned_to_project";

    // attributes of workspace which are interested for runner
    public static final String RUNNER_MAX_MEMORY_SIZE = "codenvy:runner_ram";
    public static final String RUNNER_LIFETIME        = "codenvy:runner_lifetime";
    public static final String RUNNER_INFRA           = "codenvy:runner_infra";

    private Constants() {
    }
}
