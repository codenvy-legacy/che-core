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

    public Set<ProjectTypeDetector> getDetectors() {
        return new HashSet<>(detectors);
    }

    public void register(ProjectTypeDetector detector) {
        detectors.add(detector);
    }

    public void unregister(ProjectTypeDetector detector) {
        detectors.remove(detector);
    }
}
