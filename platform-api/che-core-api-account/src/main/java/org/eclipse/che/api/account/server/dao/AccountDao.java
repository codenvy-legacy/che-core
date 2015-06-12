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
package org.eclipse.che.api.account.server.dao;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;

import java.util.List;

/**
 * DAO interface offers means to perform CRUD operations with {@link Account} data.
 * The implementation is not required
 * to be responsible for persistent layer data dto consistency. It simply transfers data from one layer to another,
 * so
 * if you're going to call any of implemented methods it is considered that all needed verifications are already done.
 * <p> <strong>Note:</strong> This particularly does not mean that method call will not make any inconsistency but this
 * mean that such kind of inconsistencies are expected by design and may be treated further. </p>
 *
 * @author Eugene Voevodin
 * @author Alexander Garagatyi
 */
public interface AccountDao {

    /**
     * Adds new account to persistent layer
     *
     * @param account
     *         POJO representation of account
     */
    void create(Account account) throws ConflictException, ServerException;

    /**
     * Gets account from persistent layer by it identifier
     *
     * @param id
     *         account identifier
     * @return account POJO
     * @throws org.eclipse.che.api.core.NotFoundException
     *         when account doesn't exist
     */
    Account getById(String id) throws NotFoundException, ServerException;

    /**
     * Gets user from persistent layer it  name
     *
     * @param name
     *         account name
     * @return account POJO
     * @throws org.eclipse.che.api.core.NotFoundException
     *         when account doesn't exist
     */
    Account getByName(String name) throws NotFoundException, ServerException;

    /**
     * Gets account from persistent level by owner
     *
     * @param owner
     *         owner id
     * @return account POJO, or empty list if nothing is found
     */
    List<Account> getByOwner(String owner) throws ServerException, NotFoundException;

    /**
     * Updates already present in persistent level account
     *
     * @param account
     *         account POJO to update
     */
    void update(Account account) throws NotFoundException, ServerException;

    /**
     * Removes account from persistent layer
     *
     * @param id
     *         account identifier
     */
    void remove(String id) throws NotFoundException, ServerException, ConflictException;

    /**
     * Adds new member to already present in persistent level account
     *
     * @param member
     *         new member
     */
    void addMember(Member member) throws NotFoundException, ConflictException, ServerException;

    /**
     * Removes member from existing account
     *
     * @param member
     *         account member to be removed
     */
    void removeMember(Member member) throws NotFoundException, ServerException, ConflictException;

    /**
     * Gets list of existing in persistent layer members related to given account
     *
     * @param accountId
     *         account id
     * @return list of members, or empty list if no members found
     */
    List<Member> getMembers(String accountId) throws ServerException;

    /**
     * Gets list of existing in persistent layer Account where given member is member
     *
     * @param userId
     *         user identifier to search
     * @return list of accounts, or empty list if no accounts found
     */
    List<Member> getByMember(String userId) throws NotFoundException, ServerException;
}
