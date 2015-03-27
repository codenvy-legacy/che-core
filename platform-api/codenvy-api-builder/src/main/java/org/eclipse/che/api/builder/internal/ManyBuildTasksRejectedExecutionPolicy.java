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

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/** @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a> */
public final class ManyBuildTasksRejectedExecutionPolicy implements RejectedExecutionHandler {
    private final RejectedExecutionHandler delegate;

    public ManyBuildTasksRejectedExecutionPolicy(RejectedExecutionHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        if (executor.getPoolSize() >= executor.getCorePoolSize()) {
            throw new RejectedExecutionException("Too many builds in progress ");
        }
        delegate.rejectedExecution(r, executor);
    }
}
