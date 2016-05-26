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
package org.eclipse.che.api.factory;

import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.factory.dto.Factory;

/**
 * This validator ensures that a factory can be edited by a user that has the associated rights (author or account owner)
 *
 * @author Florent Benoit
 */
public interface FactoryEditValidator {

    /**
     * Validates given factory by checking the current user is granted to edit the factory
     *
     * @param factory
     *         factory object to validate
     * @param userId
     *         user Id that needs to be checked
     * @throws org.eclipse.che.api.core.ApiException
     *         - in case if factory is not valid
     */
    void validate(Factory factory, String userId) throws ApiException;
}
