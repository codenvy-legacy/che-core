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
package org.eclipse.che.git.impl;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.git.CredentialsLoader;
import org.eclipse.che.api.user.server.dao.User;
import org.eclipse.che.api.user.server.dao.UserDao;
import org.eclipse.che.git.impl.jgit.JGitConnectionFactory;
import org.eclipse.che.git.impl.jgit.ssh.SshKeyProvider;
import org.testng.annotations.DataProvider;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Sergii Kabashniuk
 */
public class GitConnectionFactoryProvider {

    @DataProvider(name = "GitConnectionFactory")
    public static Object[][] createConnection() throws ServerException, NotFoundException {
        final UserDao dao = mock(UserDao.class);
        when(dao.getById(anyString())).thenReturn(new User().withEmail("email@com.com").withId("id"));
        return new Object[][]{
                new Object[]{
                        new JGitConnectionFactory(
                                mock(CredentialsLoader.class),
                                dao,
                                mock(SshKeyProvider.class)
                        )
                }
        };
    }
}
