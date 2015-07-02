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


import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;

import org.eclipse.che.api.git.GitConnection;
import org.eclipse.che.api.git.GitConnectionFactory;
import org.eclipse.che.api.git.GitException;
import org.eclipse.che.api.git.shared.AddRequest;
import org.eclipse.che.api.git.shared.BranchCheckoutRequest;
import org.eclipse.che.api.git.shared.BranchCreateRequest;
import org.eclipse.che.api.git.shared.BranchListRequest;
import org.eclipse.che.api.git.shared.CommitRequest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.eclipse.che.dto.server.DtoFactory.newDto;
import static org.eclipse.che.git.impl.GitTestUtil.addFile;
import static org.eclipse.che.git.impl.GitTestUtil.connectToInitializedGitRepository;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author Eugene Voevodin
 */
public class BranchCheckoutTest {
    private static final String FIRST_BRANCH_NAME  = "firstBranch";
    private static final String SECOND_BRANCH_NAME = "secondBranch";

    private File repository;

    @BeforeMethod
    public void setUp() {
        repository = Files.createTempDir();
    }

    @Test(dataProvider = "GitConnectionFactory", dataProviderClass = org.eclipse.che.git.impl.GitConnectionFactoryProvider.class)
    public void testSimpleCheckout(GitConnectionFactory connectionFactory) throws GitException, IOException {
        GitConnection connection = connectToInitializedGitRepository(connectionFactory, repository);

        //create branch "master"
        addFile(connection, "README.txt", org.eclipse.che.git.impl.GitTestUtil.CONTENT);
        connection.add(newDto(AddRequest.class).withFilepattern(ImmutableList.of("README.txt")));
        connection.commit(newDto(CommitRequest.class).withMessage("Initial addd"));

        //create additional branch
        connection.branchCreate(newDto(BranchCreateRequest.class).withName(FIRST_BRANCH_NAME));
        connection.branchCheckout(newDto(BranchCheckoutRequest.class).withName(FIRST_BRANCH_NAME));
        addFile(connection, "newfile", "new file content");
        connection.add(newDto(AddRequest.class).withFilepattern(Arrays.asList(".")));
        connection.commit(newDto(CommitRequest.class).withMessage("Commit message"));
        connection.branchCheckout(newDto(BranchCheckoutRequest.class).withName("master"));

        assertFalse(new File(repository, "newf3ile").exists());
        connection.branchCheckout(newDto(BranchCheckoutRequest.class).withName(FIRST_BRANCH_NAME));
        assertTrue(new File(repository, "newfile").exists());
    }

    @Test(dataProvider = "GitConnectionFactory", dataProviderClass = org.eclipse.che.git.impl.GitConnectionFactoryProvider.class)
    public void testCreateNewAndCheckout(GitConnectionFactory connectionFactory) throws GitException, IOException {
        GitConnection connection = connectToInitializedGitRepository(connectionFactory, repository);

        //create branch "master"
        addFile(connection, "README.txt", org.eclipse.che.git.impl.GitTestUtil.CONTENT);
        connection.add(newDto(AddRequest.class).withFilepattern(ImmutableList.of("README.txt")));
        connection.commit(newDto(CommitRequest.class).withMessage("Initial addd"));

        assertEquals(connection.branchList(newDto(BranchListRequest.class)).size(), 1);

        connection.branchCheckout(newDto(BranchCheckoutRequest.class).withName("thirdBranch").withCreateNew(true));
        assertEquals(connection.branchList(newDto(BranchListRequest.class)).size(), 2);
    }

    @Test(dataProvider = "GitConnectionFactory", dataProviderClass = org.eclipse.che.git.impl.GitConnectionFactoryProvider.class)
    public void testCheckoutFromStartPoint(GitConnectionFactory connectionFactory) throws GitException, IOException {
        GitConnection connection = connectToInitializedGitRepository(connectionFactory, repository);

        //create branch "master"
        addFile(connection, "README.txt", org.eclipse.che.git.impl.GitTestUtil.CONTENT);
        connection.add(newDto(AddRequest.class).withFilepattern(ImmutableList.of("README.txt")));
        connection.commit(newDto(CommitRequest.class).withMessage("Initial addd"));

        //create branch additional branch
        connection.branchCreate(newDto(BranchCreateRequest.class).withName(FIRST_BRANCH_NAME));
        connection.branchCheckout(newDto(BranchCheckoutRequest.class).withName(FIRST_BRANCH_NAME));
        addFile(connection, "newfile", "new file content");
        connection.add(newDto(AddRequest.class).withFilepattern(Arrays.asList(".")));
        connection.commit(newDto(CommitRequest.class).withMessage("Commit message"));
        connection.branchCheckout(newDto(BranchCheckoutRequest.class).withName("master"));

        //check existence of 2 branches
        assertEquals(connection.branchList(newDto(BranchListRequest.class)).size(), 2);

        //when
        connection.branchCheckout(newDto(BranchCheckoutRequest.class)
                                          .withName(SECOND_BRANCH_NAME)
                                          .withStartPoint(FIRST_BRANCH_NAME)
                                          .withCreateNew(true));
        //then
        assertEquals(connection.branchList(newDto(BranchListRequest.class)).size(), 3);
        assertTrue(new File(repository, "newfile").exists());
    }
}
