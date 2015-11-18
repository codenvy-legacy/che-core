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
package org.eclipse.che.api.factory.server.impl;

import org.eclipse.che.api.account.server.dao.AccountDao;
import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.factory.server.FactoryCreateValidator;
import org.eclipse.che.api.factory.shared.dto.Factory;
import org.eclipse.che.api.user.server.dao.PreferenceDao;
import org.eclipse.che.api.workspace.server.WorkspaceConfigValidator;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Factory creation stage validator.
 */
@Singleton
public class FactoryCreateValidatorImpl extends FactoryBaseValidator implements FactoryCreateValidator {
    private WorkspaceConfigValidator workspaceConfigValidator;

    @Inject
    public FactoryCreateValidatorImpl(AccountDao accountDao,
                                      PreferenceDao preferenceDao,
                                      WorkspaceConfigValidator workspaceConfigValidator) {
        super(accountDao, preferenceDao);
        this.workspaceConfigValidator = workspaceConfigValidator;
    }

    @Override
    public void validateOnCreate(Factory factory) throws ApiException {
        validateSource(factory);
        validateProjectNames(factory);
        validateAccountId(factory);
        validateCurrentTimeBeforeSinceUntil(factory);
        validateProjectActions(factory);
        workspaceConfigValidator.validate(factory.getWorkspace());
    }
}
