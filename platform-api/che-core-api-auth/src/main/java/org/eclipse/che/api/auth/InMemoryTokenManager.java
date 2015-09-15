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

import com.google.common.collect.HashBiMap;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

/**
 * Hold information about tokens in memory.
 *
 * @author Sergii Kabashniuk
 */
@Singleton
public class InMemoryTokenManager implements TokenManager {
    private final Map<String, String> tokens;
    private final Map<String, String> users;
    private final TokenGenerator      tokenGenerator;


    @Inject
    public InMemoryTokenManager(final TokenGenerator tokenGenerator, final TokenInvalidationHandler invalidationHandler) {
        this.tokenGenerator = tokenGenerator;
        this.tokens = HashBiMap.create();
        this.users = ((HashBiMap)tokens).inverse();
    }

    @Override
    public synchronized String createToken(String userId) {
        String token = users.get(userId);
        if (token == null) {
            token = tokenGenerator.generate();
            users.put(userId, token);
        }
        return token;
    }

    @Override
    public String getUserId(String token) {
        return tokens.get(token);
    }

    @Override
    public boolean isValid(String token) {
        return tokens.containsKey(token);
    }

    @Override
    public void invalidateToken(String token) {
        tokens.remove(token);
    }

    @Override
    public void invalidateUserToken(String userId) {
        users.remove(userId);
    }
}
