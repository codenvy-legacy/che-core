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

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Set;

/**
 * Helps register {@link Runner}s on startup.
 *
 * @author andrew00x
 */
@Singleton
public final class RunnerRegistryPlugin {
    @Inject
    public RunnerRegistryPlugin(RunnerRegistry registry, Set<Runner> runners) {
        for (Runner runner : runners) {
            registry.add(runner);
        }
    }
}
