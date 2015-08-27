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

import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.git.GitConnection;
import org.eclipse.che.api.git.GitConnectionFactory;
import org.eclipse.che.api.git.GitException;
import org.eclipse.che.api.git.shared.LsRemoteRequest;
import org.eclipse.che.api.git.shared.RemoteReference;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.eclipse.che.dto.server.DtoFactory.newDto;
import static org.eclipse.che.git.impl.GitTestUtil.cleanupTestRepo;
import static org.eclipse.che.git.impl.GitTestUtil.connectToInitializedGitRepository;
import static org.testng.Assert.assertTrue;

/**
 * @author Alexander Garagatyi
 */
public class LsRemoteTest {

    private File repository;

    @BeforeMethod
    public void setUp() {
        repository = Files.createTempDir();
    }

    @AfterMethod
    public void cleanUp() {
        cleanupTestRepo(repository);
    }

    @Test(dataProvider = "GitConnectionFactory", dataProviderClass = org.eclipse.che.git.impl.GitConnectionFactoryProvider.class)
    public void testShouldBeAbleToGetResultFromPublicRepo(GitConnectionFactory connectionFactory)
            throws GitException, IOException, UnauthorizedException {

        GitConnection connection = connectToInitializedGitRepository(connectionFactory, repository);

        //when
        Set<RemoteReference> remoteReferenceSet =
                new HashSet<>(connection.lsRemote(newDto(LsRemoteRequest.class)
                                                          .withRemoteUrl("https://github.com/codenvy/everrest.git")));

        //then
        assertTrue(remoteReferenceSet.contains(newDto(RemoteReference.class)
                                                       .withCommitId("259e24c83c8a122af858c8306c3286586404ef3f")
                                                       .withReferenceName("refs/tags/1.1.9")));
    }

    @Test(dataProvider = "GitConnectionFactory", dataProviderClass = org.eclipse.che.git.impl.GitConnectionFactoryProvider.class,
          expectedExceptions = UnauthorizedException.class,
          expectedExceptionsMessageRegExp = "Not authorized")
    public void testShouldThrowUnauthorizedExceptionIfUserTryGetInfoAboutPrivateRepoAndUserIsUnauthorized(GitConnectionFactory connectionFactory)
            throws GitException, UnauthorizedException, IOException {

        GitConnection connection = connectToInitializedGitRepository(connectionFactory, repository);

        connection.lsRemote(newDto(LsRemoteRequest.class)
                                    .withRemoteUrl("https://bitbucket.org/exoinvitemain/privater.git"));
    }
}
