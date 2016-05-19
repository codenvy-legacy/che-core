/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.project.server;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.core.util.LineConsumerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * Provide possibility for importing source from some resource e.g. VCS (like Git or SVN) or from ZIP archive
 *
 * @author Vitaly Parfonov
 */
public interface ProjectImporter {
    public enum ImporterCategory {
        SOURCE_CONTROL("Source Control"),
        ARCHIVE("Archive");

        private final String value;

        private ImporterCategory(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * @return unique id of importer e.g git, zip
     */
    String getId();

    /**
     * @return {@code true} if this importer uses only internal and not accessible for users call otherwise {@code false}
     */
    boolean isInternal();

    /**
     * @return {@link String} importer's category (example: source control, archive)
     */
    ImporterCategory getCategory();

    /**
     * @return human readable description about this importer
     */
    String getDescription();

    /**
     * Imports source from the given {@code location} to the specified folder.
     *
     * @param baseFolder
     *         base project folder
     * @param location
     *         location to the import sources
     * @param parameters
     *         optional implementation specific parameters, e.g. branch name, commit id for GIT importer
     * @throws ForbiddenException
     *         if some operations in {@code baseFolder} are forbidden, e.g. current user doesn't have write permissions to the {@code
     *         baseFolder}
     * @throws ConflictException
     *         if import causes any conflicts, e.g. if import operation causes name conflicts in {@code baseFolder}
     * @throws UnauthorizedException
     *         if user isn't authorized to access to access {@code location}
     * @throws IOException
     *         if any i/o errors occur, e.g. when try to access {@code location}
     * @throws ServerException
     *         if import causes some errors that should be treated as internal errors
     */
    void importSources(FolderEntry baseFolder, String location, Map<String, String> parameters)
            throws ForbiddenException, ConflictException, UnauthorizedException, IOException, ServerException;

    /**
     * Imports source from the given {@code location} to the specified folder.
     *
     * @param baseFolder
     *         base project folder
     * @param location
     *         location to the import sources
     * @param parameters
     *         optional implementation specific parameters, e.g. branch name, commit id for GIT importer
     * @param importOutputConsumerFactory
     *         an optional output line consumer factory to get the import process output. For instance, Git command output for the Git
     *         importer
     * @throws ForbiddenException
     *         if some operations in {@code baseFolder} are forbidden, e.g. current user doesn't have write permissions to the {@code
     *         baseFolder}
     * @throws ConflictException
     *         if import causes any conflicts, e.g. if import operation causes name conflicts in {@code baseFolder}
     * @throws UnauthorizedException
     *         if user isn't authorized to access to access {@code location}
     * @throws IOException
     *         if any i/o errors occur, e.g. when try to access {@code location}
     * @throws ServerException
     *         if import causes some errors that should be treated as internal errors
     */
    void importSources(FolderEntry baseFolder, String location, Map<String, String> parameters,
                       LineConsumerFactory importOutputConsumerFactory)
            throws ForbiddenException, ConflictException, UnauthorizedException, IOException, ServerException;
}
