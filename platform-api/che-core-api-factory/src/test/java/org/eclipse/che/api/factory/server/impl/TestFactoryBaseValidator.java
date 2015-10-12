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
import org.eclipse.che.api.factory.server.impl.FactoryBaseValidator;
import org.eclipse.che.api.user.server.dao.PreferenceDao;

/**
 * @author Sergii Kabashniuk
 */
public class TestFactoryBaseValidator extends FactoryBaseValidator {

    public TestFactoryBaseValidator(AccountDao accountDao,
                                    PreferenceDao preferenceDao
                                    ) {
        super(accountDao, preferenceDao);
    }
}
