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
package org.eclipse.che.api.auth.pac4j;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.user.server.dao.UserDao;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.http.credentials.UsernamePasswordCredentials;
import org.pac4j.http.credentials.authenticator.UsernamePasswordAuthenticator;

import javax.inject.Inject;

/**
 * @author Sergii Kabashniuk
 */
public class UserAuthenticator implements UsernamePasswordAuthenticator {

    private final UserDao userDao;

    @Inject
    public UserAuthenticator(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public void validate(UsernamePasswordCredentials credentials) {
        try {
            userDao.authenticate(credentials.getUsername(), credentials.getPassword());
        } catch (NotFoundException | ServerException e) {
            throw new CredentialsException(e.getLocalizedMessage());
        }
    }
}
