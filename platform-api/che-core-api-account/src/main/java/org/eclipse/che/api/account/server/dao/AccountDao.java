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

import org.eclipse.che.api.account.shared.dto.AccountSearchCriteria;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
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
 * @author Anatoliy Bazko
 */
public interface AccountDao {

    /**
     * Adds new account to persistent layer
     *
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
     * Adds new subscription to account that already exists in persistent layer
     *
     * @param subscription
     *         subscription POJO
     */
    void addSubscription(Subscription subscription) throws NotFoundException, ConflictException, ServerException;

    /**
     * Get subscription from persistent layer
     *
     * @param subscriptionId
     *         subscription identifier
     * @return Subscription POJO
     * @throws org.eclipse.che.api.core.NotFoundException
     *         when subscription doesn't exist
     */
    Subscription getSubscriptionById(String subscriptionId) throws NotFoundException, ServerException;

    /**
     * Gets list of active subscriptions related to given account.
     *
     * @param accountId
     *         account id
     * @return list of subscriptions, or empty list if no subscriptions found
     */
    List<Subscription> getActiveSubscriptions(String accountId) throws NotFoundException, ServerException;

    /**
     * Gets active subscription with given service related to given account.
     *
     * @param accountId
     *         account id
     * @param serviceId
     *         service id
     * @return subscription or {@code null} if no subscription found
     */
    Subscription getActiveSubscription(String accountId, String serviceId) throws ServerException, NotFoundException;

    /**
     * Update existing subscription.
     *
     * @param subscription
     *         new subscription
     */
    void updateSubscription(Subscription subscription) throws NotFoundException, ServerException;

    /**
     * Remove subscription related to existing account
     *
     * @param subscriptionId
     *         subscription identifier for removal
     */
    void removeSubscription(String subscriptionId) throws NotFoundException, ServerException;

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

    SubscriptionQueryBuilder getSubscriptionQueryBuilder();

    /**
     * Get all accounts which are locked after RAM runner resources was exceeded.
     *
     * @return all locked accounts
     */
    List<Account> getAccountsWithLockedResources() throws ServerException, ForbiddenException;

    /**
     * Gets list of existing in persistent layer Account satisfying given criteria.
     * It is supposed that search result might be very huge, that's why it is recommended to
     * retrieve result using pagination approach to reduce network traffic.
     *
     * @param searchCriteria
     *         search criteria
     * @param skipLimit
     *         the number of items to skip at first
     * @param maxItems
     *         the maximum number if items to return
     * @return list of accounts, or empty list if no accounts found
     */
    List<Account> find(AccountSearchCriteria searchCriteria, int skipLimit, int maxItems) throws ServerException;
}
