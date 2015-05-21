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
package org.eclipse.che.api.machine.server.spi;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.machine.server.InvalidInstanceSnapshotException;
import org.eclipse.che.api.machine.server.InvalidRecipeException;
import org.eclipse.che.api.machine.server.MachineException;
import org.eclipse.che.api.machine.server.UnsupportedRecipeException;
import org.eclipse.che.api.machine.shared.Recipe;

import java.util.Set;

/**
 * Provides instances of {@link Instance} in implementation specific way.
 *
 * @author gazarenkov
 * @author Alexander Garagatyi
 */
public interface InstanceProvider {
    /**
     * Gets type of instance that this provider supports. Must be unique per system.
     *
     * @return type of instance that this provider supports
     */
    String getType();

    /**
     * Gets supported recipe types.
     *
     * @return supported recipe types
     * @see org.eclipse.che.api.machine.shared.Recipe#getType()
     */
    Set<String> getRecipeTypes();

    /**
     * Creates instance from scratch.
     *
     * @param recipe
     *         instance creation {@link Recipe}
     * @param creationLogsOutput
     *         output for instance creation logs
     * @return newly created {@link Instance}
     * @throws UnsupportedRecipeException
     *         if specified {@code recipe} is not supported
     * @throws InvalidRecipeException
     *         if {@code recipe} is invalid
     */
    Instance createInstance(Recipe recipe, LineConsumer creationLogsOutput)
            throws UnsupportedRecipeException, InvalidRecipeException, MachineException;

    /**
     * Creates instance using implementation specific {@link InstanceKey}.
     *
     * @param instanceKey
     *         implementation specific {@link InstanceKey}
     * @param creationLogsOutput
     *         output for instance creation logs
     * @return newly created instance
     * @throws NotFoundException
     *         if instance described by {@code InstanceKey} doesn't exists
     * @throws InvalidInstanceSnapshotException
     *         if other errors occurs while restoring instance
     */
    Instance createInstance(InstanceKey instanceKey, LineConsumer creationLogsOutput)
            throws NotFoundException, InvalidInstanceSnapshotException, MachineException;

    /**
     * Removes snapshot of the instance in implementation specific way.
     *
     * @param instanceKey key of the snapshot of the instance that should be removed
     * @throws MachineException if exception occurs on instance snapshot removal
     */
    void removeInstanceSnapshot(InstanceKey instanceKey) throws MachineException;
}
