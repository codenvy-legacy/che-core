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
package org.eclipse.che.api.factory.server;

import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.factory.shared.dto.Factory;

/**
 * Interface for validations of factory creation stage.
 *
 * @author Alexander Garagatyi
 */
public interface FactoryCreateValidator {

    /**
     * Validates factory object on creation stage. Implementation should throw
     * {@link org.eclipse.che.api.core.ApiException} if factory object is invalid.
     *
     * @param factory
     *         factory object to validate
     * @throws org.eclipse.che.api.core.ApiException
     *         - in case if factory is not valid
     */
    void validateOnCreate(Factory factory) throws ApiException;
}
