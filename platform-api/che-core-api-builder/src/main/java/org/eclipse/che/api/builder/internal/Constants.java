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
package org.eclipse.che.api.builder.internal;

/** @author andrew00x */
public class Constants {
    // rels for known builder links
    public static final String LINK_REL_REGISTER_BUILDER_SERVICE   = "register builder service";
    public static final String LINK_REL_UNREGISTER_BUILDER_SERVICE = "unregister builder service";
    public static final String LINK_REL_REGISTERED_BUILDER_SERVER  = "registered builder server";
    public static final String LINK_REL_QUEUE_STATE                = "queue state";

    public static final String LINK_REL_AVAILABLE_BUILDERS    = "available builders";
    public static final String LINK_REL_BUILDER_STATE         = "builder state";
    public static final String LINK_REL_SERVER_STATE          = "server state";
    public static final String LINK_REL_BUILD                 = "build";
    public static final String LINK_REL_DEPENDENCIES_ANALYSIS = "analyze dependencies";

    public static final String LINK_REL_GET_STATUS               = "get status";
    public static final String LINK_REL_VIEW_LOG                 = "view build log";
    public static final String LINK_REL_VIEW_REPORT              = "view report";
    public static final String LINK_REL_DOWNLOAD_RESULT          = "download result";
    public static final String LINK_REL_DOWNLOAD_RESULTS_TARBALL = "download results tarball";
    public static final String LINK_REL_DOWNLOAD_RESULTS_ZIPBALL = "download results zipball";
    public static final String LINK_REL_BROWSE                   = "browse";
    public static final String LINK_REL_CANCEL                   = "cancel";

    // config properties
    /** URLs of slave builders that should be registered in RunQueue. */
    public static final String BUILDER_SLAVE_BUILDER_URLS = "builder.slave_builder_urls";
    /** Name of configuration parameter that points to the directory where all builds stored. */
    public static final String BASE_DIRECTORY             = "builder.base_directory";
    /**
     * Name of configuration parameter that sets the number of build workers. In other words it set the number of build
     * process that can be run at the same time. If this parameter is set to -1 then the number of available processors
     * used, e.g. {@code Runtime.getRuntime().availableProcessors();}
     */
    public static final String NUMBER_OF_WORKERS          = "builder.workers_number";
    /**
     * Name of configuration parameter that sets time (in seconds) of keeping the results (artifact and logs) of build. After this time the
     * results of build may be removed.
     */
    public static final String KEEP_RESULT_TIME           = "builder.keep_result_time";
    /**
     * Name of parameter that set the max size of build queue. The number of build task in queue may not be greater than provided by this
     * parameter.
     */
    public static final String QUEUE_SIZE                 = "builder.queue_size";
    /**
     * Max waiting time in seconds for starting build process. If process is not started after this time, it will be removed from the
     * queue.
     */
    public static final String WAITING_TIME               = "builder.waiting_time";
    /**
     * Max execution time in seconds for a build process if workspace doesn't have own setting, see {@link #BUILDER_EXECUTION_TIME}. After
     * this time build may be terminated.
     */
    public static final String MAX_EXECUTION_TIME         = "builder.max_execution_time";

    /** Build results archive type: .zip */
    public static final String RESULT_ARCHIVE_ZIP         = "zip";
    /** Build results archive type: .tar */
    public static final String RESULT_ARCHIVE_TAR         = "tar";

    /* ================================================= */

    /** @deprecated use {@link #BASE_DIRECTORY} */
    @Deprecated
    public static final String REPOSITORY          = BASE_DIRECTORY;
    /** @deprecated use {@link #KEEP_RESULT_TIME} */
    @Deprecated
    public static final String CLEANUP_RESULT_TIME = KEEP_RESULT_TIME;
    /** @deprecated use {@link #QUEUE_SIZE} */
    @Deprecated
    public static final String INTERNAL_QUEUE_SIZE = QUEUE_SIZE;

    // attributes of workspace which are interested for builder
    public static final String BUILDER_EXECUTION_TIME = "codenvy:builder_execution_time";

    private Constants() {
    }
}
