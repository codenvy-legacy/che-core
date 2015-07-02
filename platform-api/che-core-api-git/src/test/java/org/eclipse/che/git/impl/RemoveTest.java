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

import com.google.common.io.Files;
import org.eclipse.che.api.git.GitConnection;
import org.eclipse.che.api.git.GitConnectionFactory;
import org.eclipse.che.api.git.GitException;
import org.eclipse.che.api.git.shared.RmRequest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.eclipse.che.dto.server.DtoFactory.newDto;
import static org.eclipse.che.git.impl.GitTestUtil.checkNotCached;
import static org.eclipse.che.git.impl.GitTestUtil.cleanupTestRepo;
import static org.eclipse.che.git.impl.GitTestUtil.connectToGitRepositoryWithContent;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author Eugene Voevodin
 */
public class RemoveTest {

    public File repository;

    @BeforeMethod
    public void setUp() {
        repository = Files.createTempDir();
    }

    @AfterMethod
    public void cleanUp() {
        cleanupTestRepo(repository);
    }

    @Test(dataProvider = "GitConnectionFactory", dataProviderClass = org.eclipse.che.git.impl.GitConnectionFactoryProvider.class)
    public void testNotCachedRemove(GitConnectionFactory connectionFactory) throws GitException, IOException {
        GitConnection connection = connectToGitRepositoryWithContent(connectionFactory, repository);
        connection.rm(newDto(RmRequest.class).withItems(Arrays.asList("README.txt")).withCached(false));
        assertFalse(new File(connection.getWorkingDir(), "README.txt").exists());
        checkNotCached(connection, "README.txt");
    }

    @Test(dataProvider = "GitConnectionFactory", dataProviderClass = org.eclipse.che.git.impl.GitConnectionFactoryProvider.class)
    public void testCachedRemove(GitConnectionFactory connectionFactory) throws GitException, IOException {
        GitConnection connection = connectToGitRepositoryWithContent(connectionFactory, repository);
        connection.rm(newDto(RmRequest.class).withItems(Arrays.asList("README.txt")).withCached(true));
        assertTrue(new File(connection.getWorkingDir(), "README.txt").exists());
        checkNotCached(connection, "README.txt");
    }
}

