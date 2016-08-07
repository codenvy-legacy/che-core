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

import java.io.File;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.che.commons.lang.IoUtil;
import org.eclipse.che.commons.schedule.ScheduleRate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages clean-up operations for build tasks of local slave builders.
 * 
 * @author Tareq Sharafy
 */
@Singleton
public class BuilderCleaner {

    private static final Logger LOG = LoggerFactory.getLogger(BuilderCleaner.class);

    private final BuilderRegistry builders;
    private final long keepResultTimeMillis;

    @Inject
    public BuilderCleaner(BuilderRegistry builders, @Named(Constants.KEEP_RESULT_TIME) int keepResultTime) {
        this.builders = builders;
        this.keepResultTimeMillis = TimeUnit.SECONDS.toMillis(keepResultTime);
    }

    @ScheduleRate(initialDelay = 1, period = 1, unit = TimeUnit.MINUTES)
    public void cleanExpiredTasks() {
        int num = 0;
        for (Builder builder : builders.getAll()) {
            for (BuildTask task : builder.getAllBduildTasks()) {
                long endTime = task.getEndTime();
                if (endTime > 0 && (endTime + keepResultTimeMillis) < System.currentTimeMillis()) {
                    try {
                        builder.cleanBuildTask(task.getId());
                    } catch (Exception e) {
                        LOG.error(e.getMessage(), e);
                    }
                    num++;
                }
            }
        }
        if (num > 0) {
            LOG.debug("Remove {} expired tasks", num);
        }
    }

    @PreDestroy
    public void stop() {
        // Delete all builder folders
        for (Builder builder : builders.getAll()) {
            File repository = builder.getRepository();
            final File[] files = repository.listFiles();
            if (files != null) {
                for (File f : files) {
                    boolean deleted = IoUtil.deleteRecursive(f);
                    if (!deleted) {
                        LOG.warn("Failed delete {}", f);
                    }
                }
            }
        }
    }

}
