/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - API 
 *   SAP           - initial implementation
 *******************************************************************************/
package org.eclipse.che.git.impl.jgit;

import java.io.IOException;
import java.util.Map;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.core.util.LineConsumerFactory;
import org.eclipse.che.api.git.GitConnectionFactory;
import org.eclipse.che.api.git.GitProjectImporter;
import org.eclipse.che.api.project.server.FolderEntry;
import org.eclipse.che.vfs.impl.fs.LocalPathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author Sergey Kuperman
 */
@Singleton
public class GitProjectBasicAuthenticationImporter extends GitProjectImporter {
    public static final String PASSWORD = "password";
    public static final String USER_NAME = "userName";
    private static final Logger LOG = LoggerFactory.getLogger(GitProjectBasicAuthenticationImporter.class);

    @Inject
    public GitProjectBasicAuthenticationImporter(GitConnectionFactory gitConnectionFactory,
            LocalPathResolver localPathResolver) {
        super(gitConnectionFactory, localPathResolver);

    }

    @Override
    public String getId() {
        return "git-ba";
    }

    @Override
    public void importSources(FolderEntry baseFolder, String location, Map<String, String> parameters,
            LineConsumerFactory consumerFactory)
                    throws ForbiddenException, ConflictException, UnauthorizedException, IOException, ServerException {

        // set credentials for GitProjectCredentialsProvider in thread local
        // object

        LOG.info("Setting https credentials for remote git repository " + location);
        GitBasicAuthenticationCredentialsProvider.setCurrentCredentials(parameters.get(USER_NAME),
                parameters.get(PASSWORD));
        try {
            super.importSources(baseFolder, location, parameters, consumerFactory);
        } catch (Exception e) {
            // remove base folder in case of error
            baseFolder.remove();
        } finally {
            GitBasicAuthenticationCredentialsProvider.clearCredentials();
        }
    }

}
