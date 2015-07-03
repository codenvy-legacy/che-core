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

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.write;
import static org.eclipse.che.api.core.util.LineConsumerFactory.NULL;
import static org.eclipse.che.dto.server.DtoFactory.newDto;
import static org.testng.Assert.fail;

import org.eclipse.che.api.git.GitConnection;
import org.eclipse.che.api.git.GitConnectionFactory;
import org.eclipse.che.api.git.GitException;
import org.eclipse.che.api.git.shared.AddRequest;
import org.eclipse.che.api.git.shared.Branch;
import org.eclipse.che.api.git.shared.CommitRequest;
import org.eclipse.che.api.git.shared.GitUser;
import org.eclipse.che.api.git.shared.InitRequest;
import org.eclipse.che.api.git.shared.LsFilesRequest;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.lang.IoUtil;
import org.eclipse.che.commons.user.UserImpl;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * @author Sergii Kabashniuk
 */

public class GitTestUtil {

    public static final String CONTENT = "git repository content\n";


    public static GitConnection connectToGitRepositoryWithContent(GitConnectionFactory connectionFactory, File repository) throws GitException, IOException {

        GitConnection connection = getTestUserConnection(connectionFactory, repository);
        init(connection);
        addFile(repository.toPath(), "README.txt", CONTENT);
        connection.add(newDto(AddRequest.class).withFilepattern(Arrays.asList("README.txt")));
        connection.commit(newDto(CommitRequest.class).withMessage("Initial commit"));
        EnvironmentContext.getCurrent().setUser(
                new UserImpl("codenvy", "codenvy", null, Arrays.asList("workspace/developer"), false));
        return connection;
    }

    public static GitConnection connectToInitializedGitRepository(GitConnectionFactory connectionFactory, File repository) throws GitException, IOException {

        GitConnection connection = getTestUserConnection(connectionFactory, repository);
        init(connection);
        EnvironmentContext.getCurrent().setUser(
                new UserImpl("codenvy", "codenvy", null, Arrays.asList("workspace/developer"), false));
        return connection;
    }

    public static void cleanupTestRepo(File testRepo) {
        IoUtil.deleteRecursive(testRepo);
    }

    public static GitUser getTestGitUser() {
        return newDto(GitUser.class).withName("test_name").withEmail("test@email");
    }

    public static GitConnection getTestUserConnection(GitConnectionFactory connectionFactory, File repository) throws GitException {
        GitUser user = getTestGitUser();
        return connectionFactory.getConnection(repository, user, NULL);
    }


    public static void addFile(GitConnection connection, String name, String content) throws IOException {
        addFile(connection.getWorkingDir().toPath(), name, content);
    }

    public static void deleteFile(GitConnection connection, String name) throws IOException {
        delete(connection.getWorkingDir().toPath().resolve(name));
    }

    public static File addFile(Path parent, String name, String content) throws IOException {
        if (!exists(parent)) {
            createDirectories(parent);
        }
        return write(parent.resolve(name), content.getBytes()).toFile();
    }

//    public static <T> T newDTO(Class<T> dtoInterface) {
//        return DtoFactory.getInstance().createDto(dtoInterface);
//    }

    public static void init(GitConnection connection) throws GitException {
        connection.init(newDto(InitRequest.class).withBare(false));
    }
}
