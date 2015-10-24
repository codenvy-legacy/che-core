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
package org.eclipse.che.api.auth;


/**
 * Manager to handle access token for authentication-authorization process
 * <p/>
 *
 * @author Andrey Parfonov
 * @author Sergey Kabashniuk
 */
public interface TokenManager {
    /**
     * Create new access token and associate with given user id.
     *
     * @param userId
     *         user id
     * @return access token.
     */
    String createToken(String userId);

    /**
     * @param token
     *         access token.
     * @return userId associated with token
     */
    String getUserId(String token);

    /**
     * @param token
     *         access token.
     * @return true if provided token is valid
     */
    boolean isValid(String token);

    /**
     * Remove access token from manager.
     *
     * @param token
     *         unique token to remove
     */
    void invalidateToken(String token);

    /**
     * Invalidate all user tokens.
     *
     * @param userId
     */
    void invalidateUserToken(String userId);
}
