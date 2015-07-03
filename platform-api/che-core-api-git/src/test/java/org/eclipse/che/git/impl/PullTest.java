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
import org.eclipse.che.api.git.shared.*;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.eclipse.che.commons.lang.IoUtil.deleteRecursive;
import static org.eclipse.che.dto.server.DtoFactory.newDto;
import static org.eclipse.che.git.impl.GitTestUtil.addFile;
import static org.eclipse.che.git.impl.GitTestUtil.cleanupTestRepo;
import static org.eclipse.che.git.impl.GitTestUtil.connectToInitializedGitRepository;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Eugene Voevodin
 */
public class PullTest {

    private File repository;
    private final List<File> forClean = new ArrayList<>();

    @BeforeMethod
    public void setUp() {
        repository = Files.createTempDir();
    }

    @AfterMethod
    public void cleanUp() {
        cleanupTestRepo(repository);
        for (File file : forClean) {
            deleteRecursive(file);
        }
        forClean.clear();
    }

    @Test(dataProvider = "GitConnectionFactory", dataProviderClass = org.eclipse.che.git.impl.GitConnectionFactoryProvider.class)
    public void testSimplePull(GitConnectionFactory connectionFactory) throws IOException, ServerException, URISyntaxException, UnauthorizedException {
        //given
        //create new repository clone of default
        GitConnection connection = connectToInitializedGitRepository(connectionFactory, repository);
        File repo2 = new File(connection.getWorkingDir().getParent(), "repo2");
        repo2.mkdir();
        forClean.add(repo2);
        GitConnection connection2 = connectToInitializedGitRepository(connectionFactory, repo2);
        connection2.clone(newDto(CloneRequest.class)
                .withRemoteUri(connection.getWorkingDir().getAbsolutePath()));
        addFile(connection, "newfile1", "new file1 content");
        connection.add(newDto(AddRequest.class).withFilepattern(Arrays.asList(".")));
        connection.commit(newDto(CommitRequest.class).withMessage("Test commit"));
        //when
        connection2.pull(newDto(PullRequest.class).withRemote("origin").withTimeout(-1));
        //then
        assertTrue(new File(repo2.getAbsolutePath(), "newfile1").exists());
    }


    @Test(dataProvider = "GitConnectionFactory", dataProviderClass = org.eclipse.che.git.impl.GitConnectionFactoryProvider.class)
    public void testPullWithRefSpec(GitConnectionFactory connectionFactory)
            throws ServerException, URISyntaxException, IOException, UnauthorizedException {
        //given
        //create new repository clone of default
        GitConnection connection = connectToInitializedGitRepository(connectionFactory, repository);
        File repo2 = new File(connection.getWorkingDir().getParent(), "repo2");
        repo2.mkdir();
        forClean.add(repo2);
        GitConnection connection2 = connectToInitializedGitRepository(connectionFactory, repo2);
        connection2.clone(newDto(CloneRequest.class).withRemoteUri(connection.getWorkingDir().getAbsolutePath())
                .withWorkingDir(repo2.getAbsolutePath()));
        //add new branch
        connection.branchCheckout(newDto(BranchCheckoutRequest.class).withName("b1").withCreateNew(true));
        addFile(connection, "newfile1", "new file1 content");
        connection.add(newDto(AddRequest.class).withFilepattern(Arrays.asList(".")));
        connection.commit(newDto(CommitRequest.class).withMessage("Test commit"));
        int branchesBefore = connection2.branchList(newDto(BranchListRequest.class)).size();
        //when
        connection.pull(newDto(PullRequest.class)
                .withRemote("origin")
                .withRefSpec("refs/heads/b1:refs/heads/b1")
                .withTimeout(-1));
        int branchesAfter = connection2.branchList(newDto(BranchListRequest.class)).size();
        assertEquals(branchesAfter, branchesBefore + 1);
    }

    @Test(dataProvider = "GitConnectionFactory", dataProviderClass = org.eclipse.che.git.impl.GitConnectionFactoryProvider.class)
    public void testPullRemote(GitConnectionFactory connectionFactory)
            throws GitException, IOException, URISyntaxException, UnauthorizedException {
        //given
        GitConnection connection = connectToInitializedGitRepository(connectionFactory, repository);
        String branchName = "remoteBranch";
        connection.branchCheckout(newDto(BranchCheckoutRequest.class).withCreateNew(true).withName(branchName));
        addFile(connection, "remoteFile", "");
        connection.add(newDto(AddRequest.class).withFilepattern(Arrays.asList(".")));
        connection.commit(newDto(CommitRequest.class).withMessage("remote test"));

        File newRepo = new File(connection.getWorkingDir().getParent(), "newRepo");
        newRepo.mkdir();
        forClean.add(newRepo);
        GitConnection connection2 = connectToInitializedGitRepository(connectionFactory, newRepo);
        addFile(newRepo.toPath(), "EMPTY", "");
        connection2.add(newDto(AddRequest.class).withFilepattern(Arrays.asList(".")));
        connection2.commit(newDto(CommitRequest.class).withMessage("init"));

        //when
        PullRequest request = newDto(PullRequest.class);
        request.setRemote(connection.getWorkingDir().getAbsolutePath());
        request.setRefSpec(branchName);
        connection2.pull(request);
        //then
        assertTrue(new File(newRepo.getAbsolutePath(), "remoteFile").exists());
    }
}