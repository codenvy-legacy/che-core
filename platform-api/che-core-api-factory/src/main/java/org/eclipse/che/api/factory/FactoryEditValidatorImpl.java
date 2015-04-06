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
package org.eclipse.che.api.factory;

import org.eclipse.che.api.account.server.dao.AccountDao;
import org.eclipse.che.api.account.server.dao.Member;
import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.factory.dto.Factory;
import org.eclipse.che.api.factory.dto.Author;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

import static java.lang.String.format;

/**
 * This validator ensures that a factory can be edited by a user that has the associated rights (author or account owner)
 *
 * @author Florent Benoit
 */
@Singleton
public class FactoryEditValidatorImpl implements FactoryEditValidator {

    /**
     * Account DAO.
     */
    @Inject
    private AccountDao accountDao;

    /**
     * Validates given factory by checking the current user is granted to edit the factory
     *
     * @param factory
     *         factory object to validate
     * @param userId
     *         user Id that needs to be checked
     * @throws org.eclipse.che.api.core.ApiException
     *         - in case if factory is not valid
     */
    @Override
    public void validate(Factory factory, String userId) throws ApiException {
        // ensure user has the correct permissions
        boolean granted = validateAuthor(factory, userId);

        // check also for account owner
        if (!granted) {
            validateAccountOwner(factory, userId);
        }

        // ok access is granted !
    }

    /**
     * Ensures that the given user is the same author than the one that has created the factory
     * @param factory the factory to check
     * @param userId the user id to check
     * @return true if this is matching, else false
     */
    protected boolean validateAuthor(Factory factory, String userId) throws ApiException {
        // Checks if there is an author from the factory (It may be missing for some old factories)
        Author author = factory.getCreator();
        if (author == null || author.getUserId() == null) {
            throw new ServerException(format("Invalid factory without author stored. Please contact the support about the factory ID '%s'", factory.getId()));
        }

        // Gets the userId of the factory
        String factoryUserId = factory.getCreator().getUserId();

        // return true if it's the same user
        return factoryUserId.equals(userId);
    }

    /**
     * Ensures that the given user may be an account owner
     * @param factory the factory to check
     * @param userId the user id to check
     * @throws org.eclipse.che.api.core.ApiException
     */
    protected void validateAccountOwner(Factory factory, String userId) throws ApiException {
        // Checks if there is an author from the factory (It may be missing for some old factories)
        // And if there is an accountID
        Author author = factory.getCreator();
        if (author == null || author.getAccountId() == null) {
            throw new ForbiddenException(format("You are not authorized for the factory '%s'", factory.getId()));
        }

        // Gets accountID
        String factoryAccountId = factory.getCreator().getAccountId();

        List<Member> members = accountDao.getMembers(factoryAccountId);
        if (members.isEmpty()) {
            throw new ForbiddenException(format("You are not authorized for the factory '%s'", factory.getId()));
        }

        boolean isOwner = false;
        for (Member accountMember : members) {
            if (accountMember.getUserId().equals(userId) && accountMember.getRoles().contains("account/owner")) {
                isOwner = true;
                break;
            }
        }
        if (!isOwner) {
            throw new ForbiddenException(format("You are not an account/owner for the factory '%s'", factory.getId()));
        }
    }
}
