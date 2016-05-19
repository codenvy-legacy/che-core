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
package org.eclipse.che.api.runner;

import javax.inject.Singleton;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Implementation of RunnerSelectionStrategy that selects most recent used runner.
 *
 * @author andrew00x
 */
@Singleton
public class LastInUseRunnerSelectionStrategy implements RunnerSelectionStrategy, Comparator<RemoteRunner> {
    @Override
    public RemoteRunner select(List<RemoteRunner> remoteRunners) {
        if (remoteRunners == null || remoteRunners.isEmpty()) {
            throw new IllegalArgumentException("empty or null list");
        }
        Collections.sort(remoteRunners, this);
        return remoteRunners.get(0);
    }

    @Override
    public int compare(RemoteRunner o1, RemoteRunner o2) {
        final long time1 = o1.getLastUsageTime();
        final long time2 = o2.getLastUsageTime();
        if (time1 < time2) {
            return 1;
        }
        if (time1 > time2) {
            return -1;
        }
        return 0;
    }
}
