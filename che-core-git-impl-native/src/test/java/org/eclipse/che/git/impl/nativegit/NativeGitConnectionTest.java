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
package org.eclipse.che.git.impl.nativegit;

import com.google.common.io.Files;

import org.eclipse.che.api.git.GitConnectionFactory;
import org.eclipse.che.api.git.GitException;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

import static org.eclipse.che.git.impl.GitTestUtil.connectToInitializedGitRepository;
import static org.eclipse.che.git.impl.GitTestUtil.getTestUserConnection;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Provide test for methods for testing is current folder under git or not
 *
 * @author Vitalii Parfonov
 */
public class NativeGitConnectionTest {


    @Test(dataProvider = "GitConnectionFactory",
          dataProviderClass = org.eclipse.che.git.impl.GitConnectionFactoryProvider.class)
    public void testNotGitRepo(GitConnectionFactory connectionFactory) throws GitException, IOException {
        File repository = Files.createTempDir();
        NativeGitConnection connection = (NativeGitConnection)getTestUserConnection(connectionFactory, repository);
        assertFalse(connection.isInsideWorkTree());
    }

    @Test(dataProvider = "GitConnectionFactory",
          dataProviderClass = org.eclipse.che.git.impl.GitConnectionFactoryProvider.class)
    public void testInitializedGitRepo(GitConnectionFactory connectionFactory) throws GitException, IOException {
        File repository = Files.createTempDir();
        NativeGitConnection connection = (NativeGitConnection)connectToInitializedGitRepository(connectionFactory, repository);
        assertTrue(connection.isInsideWorkTree());
        connection.ensureExistenceRepoRootInWorkingDirectory();
    }

    @Test(dataProvider = "GitConnectionFactory",
          dataProviderClass = org.eclipse.che.git.impl.GitConnectionFactoryProvider.class,
          expectedExceptions = GitException.class)
    public void testNotGitRepoShouldThrowException(GitConnectionFactory connectionFactory) throws GitException, IOException {
        File repository = Files.createTempDir();
        NativeGitConnection connection = (NativeGitConnection)getTestUserConnection(connectionFactory, repository);
        connection.ensureExistenceRepoRootInWorkingDirectory();
    }
}
