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


import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.user.User;
import org.eclipse.che.dto.server.DtoFactory;

import javax.inject.Singleton;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Provide simple implementation for the case of invalid and missing.
 * <p/>
 * Respond 403 if token is invalid
 * <p/>
 * Continue filter request with anonymous user without roles if token is missing.
 *
 * @author Sergii Kabashniuk
 */
@Singleton
public class DefaultAuthorizationFilter extends AuthorizationFilter {
    @Override
    protected void handleInvalidToken(ServletRequest request, ServletResponse response, FilterChain chain, String token)
            throws IOException, ServletException {
        ((HttpServletResponse)response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON);
        try (PrintWriter writer = response.getWriter();) {
            writer.write(DtoFactory.getInstance()
                                   .toJson(new UnauthorizedException("Provided " + token + " is invalid").getServiceError()));
        }
    }

    @Override
    protected void handleMissingToken(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        EnvironmentContext environmentContext = EnvironmentContext.getCurrent();
        environmentContext.setUser(User.ANONYMOUS);
        HttpServletRequest httpServletRequest = (HttpServletRequest)request;
        chain.doFilter(new RequestWrapper(httpServletRequest, httpServletRequest.getSession(), User.ANONYMOUS), response);
    }
}
