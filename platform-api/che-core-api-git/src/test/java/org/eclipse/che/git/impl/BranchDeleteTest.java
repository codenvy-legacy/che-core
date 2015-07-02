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

import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.git.GitConnection;
import org.eclipse.che.api.git.GitConnectionFactory;
import org.eclipse.che.api.git.GitException;
import org.eclipse.che.api.git.shared.AddRequest;
import org.eclipse.che.api.git.shared.Branch;
import org.eclipse.che.api.git.shared.BranchCheckoutRequest;
import org.eclipse.che.api.git.shared.BranchCreateRequest;
import org.eclipse.che.api.git.shared.BranchDeleteRequest;
import org.eclipse.che.api.git.shared.BranchListRequest;
import org.eclipse.che.api.git.shared.CommitRequest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.eclipse.che.api.git.shared.BranchListRequest.LIST_LOCAL;
import static org.eclipse.che.dto.server.DtoFactory.newDto;
import static org.eclipse.che.git.impl.GitTestUtil.addFile;
import static org.eclipse.che.git.impl.GitTestUtil.connectToInitializedGitRepository;
import static org.testng.Assert.fail;

/**
 * @author Eugene Voevodin
 * @author Mihail Kuznyetsov
 */
public class BranchDeleteTest {

    private File repository;

    @BeforeMethod
    public void setUp() {
        repository = Files.createTempDir();
    }

    @Test(dataProvider = "GitConnectionFactory", dataProviderClass = org.eclipse.che.git.impl.GitConnectionFactoryProvider.class)
    public void testSimpleDelete(GitConnectionFactory connectionFactory) throws GitException, IOException, UnauthorizedException {
        GitConnection connection = connectToInitializedGitRepository(connectionFactory, repository);

        //create branch "master"
        addFile(connection, "README.txt", org.eclipse.che.git.impl.GitTestUtil.CONTENT);
        connection.add(newDto(AddRequest.class).withFilepattern(ImmutableList.of("README.txt")));
        connection.commit(newDto(CommitRequest.class).withMessage("Initial addd"));

        //given
        connection.branchCreate(newDto(BranchCreateRequest.class).withName("newbranch"));

        validateBranchList(
                connection.branchList(newDto(BranchListRequest.class).withListMode(LIST_LOCAL)),
                Arrays.asList(
                        newDto(Branch.class).withName("refs/heads/master")
                                            .withDisplayName("master").withActive(true).withRemote(false),
                        newDto(Branch.class).withName("refs/heads/newbranch")
                                            .withDisplayName("newbranch").withActive(false).withRemote(false)
                             )
                          );
        //when
        connection.branchDelete(newDto(BranchDeleteRequest.class).withName("newbranch").withForce(false));
        //then
        validateBranchList(
                connection.branchList(newDto(BranchListRequest.class).withListMode(LIST_LOCAL)),
                Arrays.asList(
                        newDto(Branch.class).withName("refs/heads/master")
                                            .withDisplayName("master").withActive(true).withRemote(false)
                             )
                          );
    }

    @Test(dataProvider = "GitConnectionFactory", dataProviderClass = org.eclipse.che.git.impl.GitConnectionFactoryProvider.class)
    public void shouldDeleteNotFullyMergedBranchWithForce(GitConnectionFactory connectionFactory)
            throws GitException, IOException, UnauthorizedException {
        GitConnection connection = connectToInitializedGitRepository(connectionFactory, repository);

        //create branch "master"
        addFile(connection, "README.txt", org.eclipse.che.git.impl.GitTestUtil.CONTENT);
        connection.add(newDto(AddRequest.class).withFilepattern(ImmutableList.of("README.txt")));
        connection.commit(newDto(CommitRequest.class).withMessage("Initial addd"));

        //given
        connection.branchCheckout(newDto(BranchCheckoutRequest.class).withName("newbranch").withCreateNew(true));
        addFile(connection, "newfile", "new file content");
        connection.add(newDto(AddRequest.class).withFilepattern(Arrays.asList(".")));
        connection.commit(newDto(CommitRequest.class).withMessage("second commit"));
        connection.branchCheckout(newDto(BranchCheckoutRequest.class).withName("master"));
        //when
        connection.branchDelete(newDto(BranchDeleteRequest.class).withName("newbranch").withForce(true));
        //then
        validateBranchList(
                connection.branchList(newDto(BranchListRequest.class).withListMode(LIST_LOCAL)),
                Arrays.asList(
                        newDto(Branch.class).withName("refs/heads/master")
                                .withDisplayName("master").withActive(true).withRemote(false)
                )
        );
    }

    @Test(dataProvider = "GitConnectionFactory", dataProviderClass = org.eclipse.che.git.impl.GitConnectionFactoryProvider.class,
            expectedExceptions = GitException.class)
    public void shouldThrowExceptionOnDeletingNotFullyMergedBranchWithoutForce(GitConnectionFactory connectionFactory)
            throws GitException, IOException, UnauthorizedException, NoSuchFieldException, IllegalAccessException {
        GitConnection connection = connectToInitializedGitRepository(connectionFactory, repository);

        //create branch "master"
        addFile(connection, "README.txt", org.eclipse.che.git.impl.GitTestUtil.CONTENT);
        connection.add(newDto(AddRequest.class).withFilepattern(ImmutableList.of("README.txt")));
        connection.commit(newDto(CommitRequest.class).withMessage("Initial addd"));

        connection.branchCheckout(newDto(BranchCheckoutRequest.class).withName("newbranch").withCreateNew(true));
        addFile(connection, "newfile", "new file content");
        connection.add(newDto(AddRequest.class).withFilepattern(Arrays.asList(".")));
        connection.commit(newDto(CommitRequest.class).withMessage("second commit"));
        connection.branchCheckout(newDto(BranchCheckoutRequest.class).withName("master"));

        connection.branchDelete(newDto(BranchDeleteRequest.class).withName("newbranch").withForce(false));
    }
    private void validateBranchList(List<Branch> toValidate, List<Branch> pattern) {
        l1:
        for (Branch tb : toValidate) {
            for (Branch pb : pattern) {
                if (tb.getName().equals(pb.getName()) //
                    && tb.getDisplayName().equals(pb.getDisplayName()) //
                    && tb.isActive() == pb.isActive())
                    continue l1;
            }
            fail("List of branches is not matches to expected. Branch " + tb + " is not expected in result. ");
        }
    }
}
