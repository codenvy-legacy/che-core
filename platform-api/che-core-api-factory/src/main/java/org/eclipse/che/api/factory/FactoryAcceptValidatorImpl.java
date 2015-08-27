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

import org.eclipse.che.api.account.server.dao.AccountDao;
import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.factory.dto.Factory;
import org.eclipse.che.api.user.server.dao.PreferenceDao;
import org.eclipse.che.api.user.server.dao.UserDao;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Factory URL accept stage builder.
 */
@Singleton
public class FactoryAcceptValidatorImpl extends FactoryBaseValidator implements FactoryAcceptValidator {
    @Inject
    public FactoryAcceptValidatorImpl(AccountDao accountDao,
                                      UserDao userDao,
                                      PreferenceDao preferenceDao) {
        super(accountDao,userDao, preferenceDao);
    }

    @Override
    public void validateOnAccept(Factory factory, boolean encoded) throws ApiException {
        if (!encoded) {
            validateSource(factory);
            validateProjectName(factory);
        }
        validateWorkspace(factory);
        validateModules(factory);
        validateCreator(factory);
        validateCurrentTimeBetweenSinceUntil(factory);
        validateProjectActions(factory);
    }
}
