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
package org.eclipse.che.git.impl
        ;

import static org.eclipse.che.git.impl.GitTestUtil.CONTENT;
import static org.eclipse.che.git.impl.GitTestUtil.addFile;
import static org.eclipse.che.git.impl.GitTestUtil.newDTO;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import com.google.common.collect.ImmutableList;

import org.eclipse.che.api.git.GitConnection;
import org.eclipse.che.api.git.GitException;
import org.eclipse.che.api.git.shared.AddRequest;
import org.eclipse.che.api.git.shared.CommitRequest;
import org.eclipse.che.api.git.shared.LsFilesRequest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;

/**
 * @author Eugene Voevodin
 * @author Sergii Kabnashniuk.
 *
 */
public class AddTest {

    @Test
    public void testSimpleAdd(GitConnection connection) throws GitException, IOException {
        //given
        addFile(connection, "testAdd", GitTestUtil.CONTENT);
        //when
        connection.add(newDTO(AddRequest.class).withFilepattern(AddRequest.DEFAULT_PATTERN));
        //then
        //check added files
        List<String> files = connection.listFiles(newDTO(LsFilesRequest.class));
        assertEquals(files.size(), 1);
        assertTrue(files.contains("testAdd"));
    }

    @Test(expectedExceptions = GitException.class)
    public void testNoAddWithWrongFilePattern(GitConnection connection) throws GitException {
        connection.add(newDTO(AddRequest.class)
                               .withFilepattern(ImmutableList.of("otherFile"))
                               .withUpdate(false));
    }

    @Test
    public void testAddUpdate(GitConnection connection) throws GitException, IOException {
        addFile(connection, "README.txt", CONTENT);
        connection.add(newDTO(AddRequest.class).withFilepattern(ImmutableList.of("README.txt")));
        connection.commit(newDTO(CommitRequest.class).withMessage("Initial add"));

        //modify README.txt
        addFile(connection, "README.txt", "SOME NEW CONTENT");

        List<String> listFiles = connection.listFiles(newDTO(LsFilesRequest.class).withModified(true));

        //modified but not added to stage
        assertTrue(listFiles.contains("README.txt"));
        connection.add(newDTO(AddRequest.class).withFilepattern(AddRequest.DEFAULT_PATTERN).withUpdate(true));
        listFiles = connection.listFiles(newDTO(LsFilesRequest.class).withModified(true));
        //added to stage
        assertTrue(listFiles.contains("README.txt"));
    }
}
