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
package org.eclipse.che.api.machine.server.dao;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.machine.shared.ManagedCommand;

import java.util.List;

/**
 * Data access object for {@link ManagedCommand}
 *
 * @author Eugene Voevodin
 */
public interface CommandDao {

    /**
     * Creates new command
     * <p>
     * Each command which is going to be created should have unique
     * combination of <i>workspaceId, name, creator</i>, it means that
     * user may create only one command in the same workspace with the same name.
     *
     * @param command
     *         command to create
     * @throws NullPointerException
     *         when {@code command} is null
     * @throws ConflictException
     *         when command with specified identifier or with combination of
     *         specified <i>name, workspaceId, creator</i> already exists
     * @throws ServerException
     *         when any other error occurs
     */
    void create(ManagedCommand command) throws ConflictException, ServerException;

    /**
     * Updates existing command
     * <p>
     * All data except of command identifier, type, creator and workspace identifier may be updated
     *
     * @param update
     *         command update
     * @throws NullPointerException
     *         when {@code update} is null
     * @throws NotFoundException
     *         when command with specified identifier does not exist
     * @throws ConflictException
     *         when command with specified {@code name} already exists for current user in given workspace
     * @throws ServerException
     *         when any other error occurs
     */
    void update(ManagedCommand update) throws NotFoundException, ServerException, ConflictException;

    /**
     * Removes command
     * <p>
     * If recipe with specified {@code id} doesn't exist then nothing will be done
     *
     * @param id
     *         command identifier
     * @throws ServerException
     *         when any error occurs
     */
    void remove(String id) throws ServerException;

    /**
     * Returns command with specified {@code id} or throws {@link NotFoundException}
     * when command with such identifier doesn't exist
     *
     * @param id
     *         command identifier to search command
     *         max count of items to fetch
     * @return found command
     * @throws NullPointerException
     *         when {@code id} is {@code null}
     * @throws NotFoundException
     *         when command with specified {@code id} was not found
     * @throws ServerException
     *         when any other error occurs
     */
    ManagedCommand getCommand(String id) throws NotFoundException, ServerException;

    /**
     * Searches for commands which are available for certain user in certain workspace
     * <p>
     * Command is available for user when it has <i>public</i> visibility or user is command creator
     *
     * @param workspaceId
     *         workspace identifier to search related commands
     * @param creator
     *         user identifier to search created by certain user commands
     * @param skipCount
     *         count of items which should be skipped,
     *         if found items contain fewer than {@code skipCount} items
     *         then empty list will be returned
     * @param maxItems
     *         max count of items to fetch
     * @return found commands
     * @throws ServerException
     *         when any error occurs
     */
    List<ManagedCommand> getCommands(String workspaceId, String creator, int skipCount, int maxItems) throws ServerException;
}

