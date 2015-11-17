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
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.git.GitConnection;
import org.eclipse.che.api.git.GitConnectionFactory;
import org.eclipse.che.api.git.GitException;
import org.eclipse.che.api.git.shared.AddRequest;
import org.eclipse.che.api.git.shared.CommitRequest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;

import static org.eclipse.che.dto.server.DtoFactory.newDto;
import static org.eclipse.che.git.impl.GitTestUtil.addFile;
import static org.eclipse.che.git.impl.GitTestUtil.cleanupTestRepo;
import static org.eclipse.che.git.impl.GitTestUtil.connectToInitializedGitRepository;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author Dmytro Nochevnov
 */
public class IsInsideWorkTreeTest {

    private File repository;
    private File notRepoDir;

    @BeforeMethod
    public void setUp() {
        repository = Files.createTempDir();
        notRepoDir = Files.createTempDir();
    }

    @AfterMethod
    public void cleanUp() {
        cleanupTestRepo(repository);
    }

    @Test(dataProvider = "GitConnectionFactory", dataProviderClass = GitConnectionFactoryProvider.class)
    public void shouldReturnTrueInsideWorkingTree(GitConnectionFactory connectionFactory)
            throws ServerException, IOException, UnauthorizedException, URISyntaxException {
        // given
        GitConnection connection = connectToInitializedGitRepository(connectionFactory, repository);
        Path internalDir = connection.getWorkingDir().toPath().resolve("new_directory");

        // add new dir into working tree
        addFile(connection.getWorkingDir().toPath().resolve("new_directory"), "a", "content of a");
        connection.add(newDto(AddRequest.class).withFilepattern(Arrays.asList(".")));
        connection.commit(newDto(CommitRequest.class).withMessage("test"));

        // when
        GitConnection internalDirConnection = connectionFactory.getConnection(internalDir.toFile());
        boolean isInsideWorkingTree = internalDirConnection.isInsideWorkTree();

        // then
        assertTrue(isInsideWorkingTree);
    }

    @Test(dataProvider = "GitConnectionFactory", dataProviderClass = GitConnectionFactoryProvider.class,
          expectedExceptions = GitException.class, expectedExceptionsMessageRegExp = "(?s).*fatal: Not a git repository.*(?s).*")
    public void shouldThrowGitExceptionOutsideWorkingTree(GitConnectionFactory connectionFactory)
            throws ServerException, IOException, UnauthorizedException, URISyntaxException {
        // given
        GitConnection connection = connectionFactory.getConnection(notRepoDir);

        // when-then
        connection.isInsideWorkTree();
    }
}
