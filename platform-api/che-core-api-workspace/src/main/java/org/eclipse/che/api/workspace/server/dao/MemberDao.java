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

import java.util.List;

/**
 * DAO interface offers means to perform CRUD operations with {@link Member} data.
 * The implementation is not required
 * to be responsible for persistent layer data dto consistency. It simply transfers data from one layer to another,
 * so
 * if you're going to call any of implemented methods it is considered that all needed verifications are already done.
 * <p> <strong>Note:</strong> This particularly does not mean that method call will not make any inconsistency but this
 * mean that such kind of inconsistencies are expected by design and may be treated further. </p>
 */
public interface MemberDao {

    /**
     * Adds a new Member to persistent layer.
     *
     * @param member
     *         POJO representation of workspace member
     */
    void create(Member member) throws ConflictException, NotFoundException, ServerException;


    /**
     * Updates member in persistent layer.
     *
     * @param member
     *         POJO representation of workspace member
     */
    void update(Member member) throws NotFoundException, ServerException, ConflictException;

    /**
     * Gets a list of all members of the given workspace.
     *
     * @param wsId
     *         workspace to search in
     * @return list of workspace members
     */
    List<Member> getWorkspaceMembers(String wsId) throws NotFoundException, ServerException;


    /**
     * Gets a list of all relationships of the given user and workspaces.
     *
     * @param userId
     *         user to get relationships
     * @return list of user relations
     */
    public List<Member> getUserRelationships(String userId) throws ServerException;

    /**
     * Gets membership for the given user in the given workspace.
     *
     * @param userId
     *         user to get relationships
     * @return member
     */
    public Member getWorkspaceMember(String wsId, String userId) throws NotFoundException, ServerException;

    /**
     * Removes a given member from specified workspace.
     *
     * @param member
     *         member to remove
     */
    void remove(Member member) throws NotFoundException, ConflictException, ServerException;
}
