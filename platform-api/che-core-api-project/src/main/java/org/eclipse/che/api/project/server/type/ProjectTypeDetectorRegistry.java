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
package org.eclipse.che.api.project.server.type;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Set;

/**
 * Registry for {@link ProjectTypeDetector}
 *
 * @author Roman Nikitenko
 */
@Singleton
public class ProjectTypeDetectorRegistry {
    private Set<ProjectTypeDetector> detectors = new HashSet<>();

    @Inject
    public ProjectTypeDetectorRegistry(Set<ProjectTypeDetector> detectors) {
        for (ProjectTypeDetector detector : detectors) {
            register(detector);
        }
    }

    /**
     * Gets the set of registered {@link ProjectTypeDetector}
     * <p/>
     * It is thread-safe!
     *
     * @return the set of registered {@link ProjectTypeDetector}
     */
    public synchronized Set<ProjectTypeDetector> getDetectors() {
        return new HashSet<>(detectors);
    }

    /**
     * Adds the specified detector to the set of registered detectors.
     * <p/>
     * It is thread-safe!
     *
     * @param detector
     *         detector to be added to the set of registered detectors.
     */
    public synchronized void register(ProjectTypeDetector detector) {
        detectors.add(detector);
    }

    /**
     * Removes the specified detector from the set of registered detectors.
     * <p/>
     * It is thread-safe!
     *
     * @param detector
     *         detector to be removed from the set of registered detectors
     */
    public synchronized void unregister(ProjectTypeDetector detector) {
        detectors.remove(detector);
    }
}
