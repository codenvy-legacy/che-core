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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpSession;

/**
 * Perform session invalidation after user call logout.
 *
 * @author Sergii Kabashniuk
 */

public class LocalSessionInvalidationHandler implements TokenInvalidationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(LocalSessionInvalidationHandler.class);

    private final SessionStore store;

    @Inject
    public LocalSessionInvalidationHandler(SessionStore store) {
        this.store = store;
    }

    @Override
    public void onTokenInvalidated(String token) {
        HttpSession session = store.removeSessionByToken(token);
        if (session != null) {
            session.invalidate();
            LOG.debug("logout [token: {}, session: {}, context {}]", token, session.getId(),
                      session.getServletContext().getServletContextName());
        } else {
            LOG.warn("Not found session associated to {}", token);
        }
    }
}
