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
import org.eclipse.che.api.git.shared.CommitRequest;
import org.eclipse.che.api.git.shared.LogRequest;
import org.eclipse.che.api.git.shared.ResetRequest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import static org.eclipse.che.dto.server.DtoFactory.newDto;
import static org.eclipse.che.git.impl.GitTestUtil.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author Eugene Voevodin
 */
public class ResetTest {

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
    public void testResetHard(GitConnectionFactory connectionFactory) throws Exception {
        //given
        GitConnection connection = connectToGitRepositoryWithContent(connectionFactory, repository);

        File aaa = addFile(connection.getWorkingDir().toPath(), "aaa", "aaa\n");
        FileOutputStream fos = new FileOutputStream(new File(connection.getWorkingDir(), "README.txt"));
        fos.write("MODIFIED\n".getBytes());
        fos.flush();
        fos.close();
        String initMessage = connection.log(newDto(LogRequest.class)).getCommits().get(0).getMessage();
        connection.add(newDto(AddRequest.class).withFilepattern(new ArrayList<>(Arrays.asList("."))));
        connection.commit(newDto(CommitRequest.class).withMessage("add file"));
        //when
        ResetRequest resetRequest = newDto(ResetRequest.class).withCommit("HEAD^");
        resetRequest.setType(ResetRequest.ResetType.HARD);
        connection.reset(resetRequest);
        //then
        assertEquals(connection.log(newDto(LogRequest.class)).getCommits().get(0).getMessage(), initMessage);
        assertFalse(aaa.exists());
        checkNotCached(connection, "aaa");
        assertEquals(CONTENT, Files.toString(new File(connection.getWorkingDir(), "README.txt"), Charsets.UTF_8));
    }

    @Test(dataProvider = "GitConnectionFactory", dataProviderClass = org.eclipse.che.git.impl.GitConnectionFactoryProvider.class)
    public void testResetSoft(GitConnectionFactory connectionFactory) throws Exception {
        GitConnection connection = connectToGitRepositoryWithContent(connectionFactory, repository);
        File aaa = addFile(connection.getWorkingDir().toPath(), "aaa", "aaa\n");
        FileOutputStream fos = new FileOutputStream(new File(connection.getWorkingDir(), "README.txt"));
        fos.write("MODIFIED\n".getBytes());
        fos.flush();
        fos.close();
        String initMessage = connection.log(newDto(LogRequest.class)).getCommits().get(0).getMessage();
        connection.add(newDto(AddRequest.class).withFilepattern(new ArrayList<>(Arrays.asList("."))));
        connection.commit(newDto(CommitRequest.class).withMessage("add file"));
        //when
        ResetRequest resetRequest = newDto(ResetRequest.class).withCommit("HEAD^");
        resetRequest.setType(ResetRequest.ResetType.SOFT);
        connection.reset(resetRequest);
        //then
        assertEquals(connection.log(newDto(LogRequest.class)).getCommits().get(0).getMessage(), initMessage);
        assertTrue(aaa.exists());
        checkCached(connection, "aaa");
        assertEquals(Files.toString(new File(connection.getWorkingDir(), "README.txt"), Charsets.UTF_8), "MODIFIED\n");
    }

    @Test(dataProvider = "GitConnectionFactory", dataProviderClass = org.eclipse.che.git.impl.GitConnectionFactoryProvider.class)
    public void testResetMixed(GitConnectionFactory connectionFactory) throws Exception {
        GitConnection connection = connectToGitRepositoryWithContent(connectionFactory, repository);
        //given
        File aaa = addFile(connection.getWorkingDir().toPath(), "aaa", "aaa\n");
        FileOutputStream fos = new FileOutputStream(new File(connection.getWorkingDir(), "README.txt"));
        fos.write("MODIFIED\n".getBytes());
        fos.flush();
        fos.close();
        String initMessage = connection.log(newDto(LogRequest.class)).getCommits().get(0).getMessage();
        connection.add(newDto(AddRequest.class).withFilepattern(new ArrayList<>(Arrays.asList("."))));
        connection.commit(newDto(CommitRequest.class).withMessage("add file"));
        //when
        ResetRequest resetRequest = newDto(ResetRequest.class).withCommit("HEAD^");
        resetRequest.setType(ResetRequest.ResetType.MIXED);
        connection.reset(resetRequest);
        //then
        assertEquals(connection.log(newDto(LogRequest.class)).getCommits().get(0).getMessage(), initMessage);
        assertTrue(aaa.exists());
        checkNotCached(connection, "aaa");
        assertEquals(Files.toString(new File(connection.getWorkingDir(), "README.txt"), Charsets.UTF_8), "MODIFIED\n");
    }
}
