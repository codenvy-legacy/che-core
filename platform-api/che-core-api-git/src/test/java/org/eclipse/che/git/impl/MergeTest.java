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

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import org.eclipse.che.api.git.GitConnection;
import org.eclipse.che.api.git.GitConnectionFactory;
import org.eclipse.che.api.git.shared.AddRequest;
import org.eclipse.che.api.git.shared.BranchCheckoutRequest;
import org.eclipse.che.api.git.shared.BranchCreateRequest;
import org.eclipse.che.api.git.shared.CommitRequest;
import org.eclipse.che.api.git.shared.LogRequest;
import org.eclipse.che.api.git.shared.MergeRequest;
import org.eclipse.che.api.git.shared.MergeResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.eclipse.che.dto.server.DtoFactory.newDto;
import static org.eclipse.che.git.impl.GitTestUtil.addFile;
import static org.eclipse.che.git.impl.GitTestUtil.cleanupTestRepo;
import static org.eclipse.che.git.impl.GitTestUtil.connectToGitRepositoryWithContent;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Eugene Voevodin
 */
public class MergeTest {

    private String branchName = "MergeTestBranch";

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
    public void testMergeNoChanges(GitConnectionFactory connectionFactory) throws Exception {
        //given
        GitConnection connection = connectToGitRepositoryWithContent(connectionFactory, repository);
        connection.branchCreate(newDto(BranchCreateRequest.class).withName(branchName));
        //when
        MergeResult mergeResult = connection.merge(newDto(MergeRequest.class).withCommit(branchName));
        //then
        assertEquals(mergeResult.getMergeStatus(), MergeResult.MergeStatus.ALREADY_UP_TO_DATE);
    }

    @Test(dataProvider = "GitConnectionFactory", dataProviderClass = org.eclipse.che.git.impl.GitConnectionFactoryProvider.class)
    public void testMerge(GitConnectionFactory connectionFactory) throws Exception {
        //given
        GitConnection connection = connectToGitRepositoryWithContent(connectionFactory, repository);
        connection.branchCheckout(newDto(BranchCheckoutRequest.class).withName(branchName).withCreateNew(true));
        File file = addFile(connection, "t-merge", "aaa\n");

        connection.add(newDto(AddRequest.class).withFilepattern(new ArrayList<>(Arrays.asList("."))));
        connection.commit(newDto(CommitRequest.class).withMessage("add file in new branch"));
        connection.branchCheckout(newDto(BranchCheckoutRequest.class).withName("master"));
        //when
        MergeResult mergeResult = connection.merge(newDto(MergeRequest.class).withCommit(branchName));
        //then
        assertEquals(mergeResult.getMergeStatus(), MergeResult.MergeStatus.FAST_FORWARD);
        assertTrue(file.exists());
        assertEquals(Files.toString(file, Charsets.UTF_8), "aaa\n");
        assertEquals(connection.log(newDto(LogRequest.class)).getCommits().get(0).getMessage(), "add file in new branch");
    }

    @Test(dataProvider = "GitConnectionFactory", dataProviderClass = org.eclipse.che.git.impl.GitConnectionFactoryProvider.class)
    public void testMergeConflict(GitConnectionFactory connectionFactory) throws Exception {
        //given
        GitConnection connection = connectToGitRepositoryWithContent(connectionFactory, repository);
        connection.branchCheckout(newDto(BranchCheckoutRequest.class).withName(branchName).withCreateNew(true));
        addFile(connection, "t-merge-conflict", "aaa\n");
        connection.add(newDto(AddRequest.class).withFilepattern(new ArrayList<>(Arrays.asList("."))));
        connection.commit(newDto(CommitRequest.class).withMessage("add file in new branch"));

        connection.branchCheckout(newDto(BranchCheckoutRequest.class).withName("master"));
        addFile(connection, "t-merge-conflict", "bbb\n");
        connection.add(newDto(AddRequest.class).withFilepattern(new ArrayList<>(Arrays.asList("."))));
        connection.commit(newDto(CommitRequest.class).withMessage("add file in new branch"));
        //when
        MergeResult mergeResult = connection.merge(newDto(MergeRequest.class).withCommit(branchName));
        //then
        List<String> conflicts = mergeResult.getConflicts();
        assertEquals(conflicts.size(), 1);
        assertEquals(conflicts.get(0), "t-merge-conflict");

        assertEquals(mergeResult.getMergeStatus(), MergeResult.MergeStatus.CONFLICTING);

        String expContent = "<<<<<<< HEAD\n" //
                + "bbb\n" //
                + "=======\n" //
                + "aaa\n" //
                + ">>>>>>> MergeTestBranch\n";
        String actual = Files.toString(new File(connection.getWorkingDir(), "t-merge-conflict"), Charsets.UTF_8);
        assertEquals(actual, expContent);
    }

//        TODO Uncomment as soon as IDEX-1776 is fixed
//    @Test(dataProvider = "GitConnectionFactory", dataProviderClass = org.eclipse.che.git.impl.GitConnectionFactoryProvider.class)
//    public void testFailed(GitConnectionFactory connectionFactory) throws GitException, IOException {
//        //given
//        GitConnection connection = connectToGitRepositoryWithContent(connectionFactory, repository);
//
//        connection.branchCheckout(newDto(BranchCheckoutRequest.class).withName(branchName).withCreateNew(true));
//        addFile(connection, "t-merge-failed", "aaa\n");
//        connection.add(newDto(AddRequest.class).withFilepattern(new ArrayList<>(Arrays.asList("."))));
//        connection.commit(newDto(CommitRequest.class).withMessage("add file in new branch"));
//
//        connection.branchCheckout(newDto(BranchCheckoutRequest.class).withName("master"));
//        addFile(connection, "t-merge-failed", "bbb\n");
//        //when
//        MergeResult mergeResult = connection.merge(newDto(MergeRequest.class).withCommit(branchName));
//        //then
//        assertEquals(mergeResult.getMergeStatus(), MergeResult.MergeStatus.FAILED);
//        assertEquals(mergeResult.getFailed().size(), 1);
//        assertEquals(mergeResult.getFailed().get(0), "t-merge-failed");
//    }
}
