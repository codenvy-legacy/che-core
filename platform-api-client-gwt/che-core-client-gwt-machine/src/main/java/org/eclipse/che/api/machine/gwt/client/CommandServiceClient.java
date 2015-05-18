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
package org.eclipse.che.api.machine.gwt.client;

import org.eclipse.che.api.machine.shared.dto.CommandDescriptor;
import org.eclipse.che.api.promises.client.Promise;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Client for Command service.
 *
 * @author Artem Zatsarynnyy
 */
public interface CommandServiceClient {

    /**
     * Create command.
     *
     * @param name
     *         command name
     * @param commandLine
     *         command line
     * @param type
     *         type of the command
     */
    Promise<CommandDescriptor> createCommand(@Nonnull String name,
                                             @Nonnull String commandLine,
                                             @Nonnull String type);

    /** Get all commands. */
    Promise<List<CommandDescriptor>> getCommands();

    /**
     * Update command.
     *
     * @param id
     *         ID of the command that should be updated
     * @param name
     *         new command name
     * @param commandLine
     *         new command line
     */
    Promise<CommandDescriptor> updateCommand(@Nonnull String id, @Nonnull String name, @Nonnull String commandLine);

    /**
     * Remove command with the specified ID.
     *
     * @param id
     *         ID of command that should be removed
     */
    Promise<Void> removeCommand(@Nonnull String id);
}
