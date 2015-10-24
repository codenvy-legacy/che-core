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

import static com.google.common.base.Preconditions.checkNotNull;


import org.eclipse.che.commons.user.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;
import java.security.Principal;

/**
 * Wraps HttpServletRequest and provide correct answers for
 * getRemoteUser, isUserInRole and getUserPrincipal getSession.
 */
public class RequestWrapper extends HttpServletRequestWrapper {
    private final HttpSession session;
    private final User        user;

    /**
     * Constructs a request object wrapping the given request.
     *
     * @param request
     * @throws IllegalArgumentException
     *         if the request is null
     */
    public RequestWrapper(HttpServletRequest request, HttpSession session, User user) {
        super(request);
        checkNotNull(request);
        checkNotNull(user);
        this.session = session;
        this.user = user;
    }

    @Override
    public String getRemoteUser() {
        return user.getName();
    }

    @Override
    public boolean isUserInRole(String role) {
        return user.isMemberOf(role);
    }

    @Override
    public Principal getUserPrincipal() {
        return new Principal() {
            @Override
            public String getName() {
                return user.getName();
            }
        };
    }

    @Override
    public HttpSession getSession() {
        return session;
    }

}