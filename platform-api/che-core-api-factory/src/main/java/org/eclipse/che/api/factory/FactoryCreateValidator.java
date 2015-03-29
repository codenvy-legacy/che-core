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
package org.eclipse.che.api.factory;

import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.factory.dto.Factory;

/**
 * Interface for validations of factory urls on creation stage.
 *
 * @author Alexander Garagatyi
 */
public interface FactoryCreateValidator {

    /**
     * Validates factory url object on creation stage. Implementation should throw
     * {@link org.eclipse.che.api.core.ApiException} if factory url object is invalid.
     *
     * @param factory
     *         factory object to validate
     * @throws org.eclipse.che.api.core.ApiException
     *         - in case if factory is not valid
     */
    void validateOnCreate(Factory factory) throws ApiException;
}
