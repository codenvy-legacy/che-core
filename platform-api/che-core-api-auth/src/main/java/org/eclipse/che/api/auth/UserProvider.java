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


import org.eclipse.che.commons.user.User;

/**
 * Provider user by his authentication token.
 *
 * @author Sergii Kabashniuk
 */
public interface UserProvider {
    /**
     * @param token
     *         authentication token.
     * @return user that will be user in request.
     */
    User getUser(String token);
}
