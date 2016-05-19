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
package org.eclipse.che.api.builder;

import javax.inject.Singleton;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;

/**
 * @author andrew00x
 */
@Singleton
public class RandomBuilderSelectionStrategy implements BuilderSelectionStrategy {
    private final Random random = new SecureRandom();

    @Override
    public RemoteBuilder select(List<RemoteBuilder> slaveBuilders) {
        if (slaveBuilders.size() == 1) {
            return slaveBuilders.get(0);
        }
        return slaveBuilders.get(random.nextInt(slaveBuilders.size()));
    }
}
