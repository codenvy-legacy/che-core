/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2015] Codenvy, S.A. 
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package org.eclipse.che.git.impl;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.write;

import org.eclipse.che.api.git.GitConnection;
import org.eclipse.che.api.git.GitException;
import org.eclipse.che.api.git.shared.InitRequest;
import org.eclipse.che.dto.server.DtoFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;

/**
 * @author Sergii Kabashniuk
 */
public class GitTestUtil {

    public static final String     CONTENT     = "git repository content\n";

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

    public static String readFile(File file) throws IOException {
        if (file.isDirectory())
            throw new IllegalArgumentException("Can't read content from directory " + file.getAbsolutePath());
        FileReader reader = null;
        StringBuilder content = new StringBuilder();
        try {
            reader = new FileReader(file);
            int ch = -1;
            while ((ch = reader.read()) != -1)
                content.append((char)ch);
        } finally {
            if (reader != null)
                reader.close();
        }
        return content.toString();
    }

    public static <T> T newDTO(Class<T> dtoInterface) {
        return DtoFactory.getInstance().createDto(dtoInterface);
    }

    public static void init(GitConnection connection) throws GitException {
        connection.init(newDTO(InitRequest.class).withBare(false));
    }

}
