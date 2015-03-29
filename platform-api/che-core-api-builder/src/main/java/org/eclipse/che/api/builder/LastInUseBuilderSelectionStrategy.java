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
package org.eclipse.che.api.builder;

import javax.inject.Singleton;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Implementation of BuilderSelectionStrategy that selects most recent used builder.
 *
 * @author andrew00x
 */
@Singleton
public class LastInUseBuilderSelectionStrategy implements BuilderSelectionStrategy, Comparator<RemoteBuilder> {
    @Override
    public RemoteBuilder select(List<RemoteBuilder> slaveBuilders) {
        if (slaveBuilders == null || slaveBuilders.isEmpty()) {
            throw new IllegalArgumentException("empty or null list");
        }
        Collections.sort(slaveBuilders, this);
        return slaveBuilders.get(0);
    }

    @Override
    public int compare(RemoteBuilder o1, RemoteBuilder o2) {
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
