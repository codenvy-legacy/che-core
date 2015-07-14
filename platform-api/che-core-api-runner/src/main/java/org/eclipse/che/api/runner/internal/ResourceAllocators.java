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
package org.eclipse.che.api.runner.internal;

import org.eclipse.che.api.runner.RunnerException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.concurrent.Semaphore;

/**
 * Allocator for resources.
 * Usage (memory allocation as example):
 * <pre>
 *     int mem = ...
 *     ResourceAllocator memAllocator = ResourceAllocators.getInstance().newMemoryAllocator(mem).allocate();
 *     try {
 *         // do something
 *     } finally {
 *         memAllocator.release();
 *     }
 * </pre>
 *
 * @author andrew00x
 */
@Singleton
public class ResourceAllocators {
    /** @deprecated use {@link Constants#TOTAL_APPS_MEM_SIZE}. */
    @Deprecated
    public static final String TOTAL_APPS_MEM_SIZE = Constants.TOTAL_APPS_MEM_SIZE;

    private static final Logger LOG = LoggerFactory.getLogger(ResourceAllocators.class);

    private final int       memSize;
    private final Semaphore memSemaphore;

    @Inject
    public ResourceAllocators(@Named(Constants.TOTAL_APPS_MEM_SIZE) int memSize) {
        if (memSize <= 0) {
            throw new IllegalArgumentException(String.format("Invalid mem size %d", memSize));
        }
        this.memSize = memSize;
        memSemaphore = new Semaphore(memSize);
    }

    /**
     * Create new memory allocator. Returned instance doesn't manage memory directly or indirectly it just remembers size of requested
     * memory and prevents getting more amount of memory than it is set in configuration. To 'allocate' memory caller must call method
     * {@link ResourceAllocator#allocate()}. It is important to call method {@link
     * ResourceAllocator#release()} to release allocated memory. Typically this should be done after stopping of
     * application.
     *
     * @param size
     *         memory size in megabytes
     * @return memory allocator
     * @see #freeMemory()
     * @see #totalMemory()
     * @see Constants#TOTAL_APPS_MEM_SIZE
     */
    public ResourceAllocator newMemoryAllocator(int size) {
        return new MemoryAllocator(size);
    }

    /**
     * Returns amount of 'free' memory in megabytes. The returned value is not related to amount of free memory in the Java virtual
     * machine or free physical memory. It shows how much memory is available for <b>all</b> Runners for starting new applications.
     *
     * @return amount of 'free' memory in megabytes
     * @see #totalMemory()
     * @see Constants#TOTAL_APPS_MEM_SIZE
     */
    public int freeMemory() {
        return memSemaphore.availablePermits();
    }

    /**
     * Returns 'total' amount of memory in megabytes. The returned value is not related to amount of memory in the Java virtual machine or
     * total physical memory. It shows how much memory is defined for <b>all</b> Runners for starting applications.
     *
     * @return amount of 'total' memory in megabytes
     * @see #freeMemory()
     * @see Constants#TOTAL_APPS_MEM_SIZE
     */
    public int totalMemory() {
        return memSize;
    }

    /* ===== INTERNAL STUFF ===== */

    /** Manages memory available for running applications. */
    private class MemoryAllocator implements ResourceAllocator {
        final int size;

        MemoryAllocator(int size) {
            this.size = size;
        }

        @Override
        public MemoryAllocator allocate() throws RunnerException {
            if (!memSemaphore.tryAcquire(size)) {
                throw new RunnerException(String.format("Couldn't allocate %dM for starting application", size));
            }
            LOG.debug("allocate memory: {}M, available: {}M", size, memSemaphore.availablePermits());
            return this;
        }

        @Override
        public void release() {
            memSemaphore.release(size);
            LOG.debug("release memory: {}M, available: {}M", size, memSemaphore.availablePermits());
        }
    }
}
