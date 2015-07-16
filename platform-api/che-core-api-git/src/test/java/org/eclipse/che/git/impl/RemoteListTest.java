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
import org.eclipse.che.api.git.shared.CloneRequest;
import org.eclipse.che.api.git.shared.Remote;
import org.eclipse.che.api.git.shared.RemoteAddRequest;
import org.eclipse.che.api.git.shared.RemoteListRequest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static org.eclipse.che.dto.server.DtoFactory.newDto;
import static org.eclipse.che.git.impl.GitTestUtil.cleanupTestRepo;
import static org.eclipse.che.git.impl.GitTestUtil.connectToGitRepositoryWithContent;
import static org.eclipse.che.git.impl.GitTestUtil.getTestGitUser;
import static org.testng.Assert.assertEquals;

/**
 * @author Eugene Voevodin
 */
public class RemoteListTest {

    private File repository;
    private File remoteRepo;

    @BeforeMethod
    public void setUp() {
        repository = Files.createTempDir();
        remoteRepo = Files.createTempDir();
    }

    @AfterMethod
    public void cleanUp() {
        cleanupTestRepo(repository);
        cleanupTestRepo(remoteRepo);
    }

    @Test(dataProvider = "GitConnectionFactory", dataProviderClass = GitConnectionFactoryProvider.class)
    public void testRemoteList(GitConnectionFactory connectionFactory)
            throws ServerException, URISyntaxException, UnauthorizedException, IOException {
        //given
        GitConnection connection = connectToGitRepositoryWithContent(connectionFactory, repository);
        GitConnection connection2 = connectionFactory.getConnection(remoteRepo.getAbsolutePath(), getTestGitUser());
        connection2.clone(newDto(CloneRequest.class).withRemoteUri(connection.getWorkingDir().getAbsolutePath())
                                                    .withWorkingDir(connection2.getWorkingDir().getAbsolutePath()));
        assertEquals(connection2.remoteList(newDto(RemoteListRequest.class)).size(), 1);
        //create new remote
        connection2.remoteAdd(newDto(RemoteAddRequest.class)
                                      .withName("newremote")
                                      .withUrl("newremote.url"));
        assertEquals(connection2.remoteList(newDto(RemoteListRequest.class)).size(), 2);
        //when
        RemoteListRequest request = newDto(RemoteListRequest.class);
        request.setRemote("newremote");
        List<Remote> one = connection2.remoteList(request);
        //then
        assertEquals(one.get(0).getUrl(), "newremote.url");
        assertEquals(one.size(), 1);
    }
}
