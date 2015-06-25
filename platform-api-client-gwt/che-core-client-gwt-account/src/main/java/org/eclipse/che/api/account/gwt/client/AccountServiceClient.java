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
package org.eclipse.che.api.account.gwt.client;

import org.eclipse.che.api.account.shared.dto.AccountDescriptor;
import org.eclipse.che.api.account.shared.dto.MemberDescriptor;
import org.eclipse.che.api.account.shared.dto.UpdateResourcesDescriptor;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.rest.AsyncRequestCallback;

import javax.annotation.Nonnull;

/**
 * Client for IDE3 Subscription service.
 *
 * @author Sergii Leschenko
 */
public interface AccountServiceClient {

    /**
     * Get account by id.
     *
     * @param accountId
     *         id of account
     * @param callback
     *         the callback to use for the response
     */
    void getAccountById(@Nonnull String accountId, AsyncRequestCallback<AccountDescriptor> callback);

    /**
     * Get memberships for current user
     *
     * @param callback
     *         the callback to use for the response
     */
    void getMemberships(AsyncRequestCallback<Array<MemberDescriptor>> callback);

    void redistributeResources(@Nonnull String accountId,
                               @Nonnull Array<UpdateResourcesDescriptor> updateResources,
                               AsyncRequestCallback<Void> callback);
}
