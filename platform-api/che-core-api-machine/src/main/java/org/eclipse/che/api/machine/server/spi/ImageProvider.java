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
import org.eclipse.che.api.machine.server.InvalidImageException;
import org.eclipse.che.api.machine.server.InvalidRecipeException;
import org.eclipse.che.api.machine.server.MachineException;
import org.eclipse.che.api.machine.server.UnsupportedRecipeException;
import org.eclipse.che.api.machine.shared.Recipe;

import java.util.Set;

/**
 * Provides instances of {@link Image} in implementation specific way.
 *
 * @author gazarenkov
 */
public interface ImageProvider {
    /**
     * Gets type of image that this provider supports. Must be unique per system.
     *
     * @return type of image that this provider supports
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
     * Creates image from scratch.
     *
     * @param recipe
     *         image creation {@link Recipe}
     * @param creationLogsOutput
     *         output for image creation logs
     * @return newly created {@link Image}
     * @throws UnsupportedRecipeException
     *         if specified {@code recipe} is not supported
     * @throws InvalidRecipeException
     *         if {@code recipe} is invalid
     */
    Image createImage(Recipe recipe, LineConsumer creationLogsOutput)
            throws UnsupportedRecipeException, InvalidRecipeException, MachineException;


    /**
     * Creates image using implementation specific {@link ImageKey}.
     *
     * @param imageKey
     *         implementation specific {@link ImageKey}
     * @param creationLogsOutput
     *         output for image creation logs
     * @return newly created image
     * @throws NotFoundException
     *         if image described by {@code imageKey} doesn't exists
     * @throws InvalidImageException
     *         if other errors occurs while restoring image
     */
    Image createImage(ImageKey imageKey, LineConsumer creationLogsOutput) throws NotFoundException, InvalidImageException, MachineException;

    void removeImage(ImageKey imageKey) throws MachineException;
}
