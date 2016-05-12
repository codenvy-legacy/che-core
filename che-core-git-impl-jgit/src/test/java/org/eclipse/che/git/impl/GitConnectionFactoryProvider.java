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
import org.eclipse.che.git.impl.jgit.JGitConnectionFactory;
import org.eclipse.che.git.impl.jgit.ssh.SshKeyProvider;
import org.testng.annotations.DataProvider;

import static org.mockito.Mockito.mock;

/**
 * @author Sergii Kabashniuk
 */
public class GitConnectionFactoryProvider {

    @DataProvider(name = "GitConnectionFactory")
    public static Object[][] createConnection() throws ServerException, NotFoundException {
        return new Object[][]{
                new Object[]{
                        new JGitConnectionFactory(
                                mock(CredentialsLoader.class),
                                mock(SshKeyProvider.class)
                        )
                }
        };
    }
}
