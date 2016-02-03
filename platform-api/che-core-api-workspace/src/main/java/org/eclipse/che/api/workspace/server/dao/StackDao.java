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
package org.eclipse.che.api.workspace.server.dao;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.workspace.server.model.impl.stack.DecoratedStackImpl;

import java.util.List;

/**
 * Defines data access object for {@link DecoratedStackImpl}
 *
 * @author Alexander Andrienko
 */
public interface StackDao {

    /**
     * Create new Stack.
     *
     * @param stack stack to create
     * @throws NullPointerException
     *          when {@code stack} is not specified
     * @throws ConflictException
     *          when stack with id equal to {@code stack.getId()} is already exist
     * @throws ServerException
     *          when any error occurs
     */
    void create(DecoratedStackImpl stack) throws ConflictException, ServerException;

    /**
     * Returns existing stack by specified {@code id} or throws {@link NotFoundException}
     * when stack with such identifier doesn't exist.
     *
     * @param id the stack id
     * @throws NullPointerException
     *          when {@code id} is not specified
     * @throws NotFoundException
     *          if stack was not found
     * @throws ServerException
     *          when any error occurs
     */
    DecoratedStackImpl getById(String id) throws NotFoundException, ServerException;

    /**
     * Remove the stack by specified {@code id}.
     *
     * @param id
     *          stack identifier to remove stack
     * @throws NullPointerException
     *          when {@code id} is not specified
     * @throws ServerException
     *          when any error occurs
     */
    void remove(String id) throws ServerException;

    /**
     * Full update existing stack. Existed stack with id equal to {@code update.getId()} should be replaced by {@code update}.
     *
     * @param update the stack for update
     * @throws NullPointerException
     *          when {@code update} is not specified
     * @throws NotFoundException
     *          when stack with {@code update.getId()} doesn't exist
     * @throws ServerException
     *          when any error occurs
     */
    void update(DecoratedStackImpl update) throws NotFoundException, ServerException;

    /**
     * Returns limited by {@code skipCount} and {@code maxItems} stacks which creator is equal to specified {@code creator}
     * or empty list when such stacks were not found.
     *
     * @param creator
     *         stack creator to filter stacks
     * @param skipCount
     *         count of items which should be skipped,
     *         if found items contain fewer than {@code skipCount} items
     *         then return empty list items
     * @param maxItems
     *          max count of items to fetch
     * @return stacks which creator matches to specified {@code creator}
     *
     * @throws ServerException
     *          when any error occurs
     * @throws NullPointerException
     *          when {@code creator} is null
     */
    List<DecoratedStackImpl> getByCreator(String creator, int skipCount, int maxItems) throws ServerException;

    /**
     * Searches for stacks which contains all of specified {@code tags}.
     * Not specified {@code tags} will not take part of search
     * <b>Note: only stack which contains permission <i>public: search<i/> take part of the search</b>
     *
     * @param tags
     *          stack tags to search stacks, may be {@code null}
     * @param skipCount
     *          count of items which should be skipped,
     *          if found items contain fewer than {@code skipCount} items
     *          then return empty list items
     * @param maxItems
     *           max count of items to fetch
     * @return list stacks which contains all of specified {@code tags}
     * @throws ServerException
     *          when any error occurs
     */
    List<DecoratedStackImpl> searchStacks(List<String> tags, int skipCount, int maxItems) throws ServerException;
}
