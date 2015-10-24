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

import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.user.User;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Provide  request authorization by authentication token.
 * <p/>
 * Token which was obtained during authentication should be combined with
 * original request. {@link TokenExtractor} suppose to be able to extract token from request
 * and with help of  {@link UserProvider} can be transformed to real user with roles
 * actual for the current environment {@link org.eclipse.che.commons.env.EnvironmentContext}.
 * <p/>
 * To make LoginFilter work such a requirement has to be satisfied
 * <p/>
 * <ul>
 * <li>
 * All environment variables have to be set before  LoginFilter.
 * This will allow {@link UserProvider} identify user with correct roles for this moment.
 * </li>
 * <li>
 * {@link DestroySessionListener} have to be set in container to be able correctly close
 * opened sessions assisted with tokens by container timeout.
 * </li>
 * </ul>
 *
 * @author Sergii Kabashniuk
 * @see AuthenticationService
 * @see UserProvider
 * @see TokenExtractor
 * @see DestroySessionListener
 */
@Singleton
public abstract class AuthorizationFilter implements Filter {

    @Inject
    protected TokenExtractor tokenExtractor;

    @Inject
    protected SessionStore sessionStore;

    @Inject
    protected TokenManager tokenManager;

    @Inject
    protected UserProvider userProvider;


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest httpRequest = (HttpServletRequest)request;
        String token = tokenExtractor.getToken(httpRequest);
        if (token != null) {
            HttpSession session = null;
            synchronized (sessionStore) {
                session = sessionStore.getSession(token);
                if (session == null) {
                    session = httpRequest.getSession();
                    sessionStore.saveSession(token, session);
                }
            }

            if (tokenManager.isValid(token)) {
                handleValidToken(request, response, chain, session, userProvider.getUser(token));
            } else {
                handleInvalidToken(request, response, chain, token);
            }
        } else {
            //token not exists
            handleMissingToken(request, response, chain);
        }

    }

    /**
     * Handle request with valid token and user.
     * Method should commit response.
     *
     * @param request
     *         original request
     * @param response
     *         original response
     * @param chain
     *         filter chain
     * @param session
     *         first session associated with given token.
     * @param user
     *         user associated with given token
     * @throws IOException
     * @throws ServletException
     */
    protected void handleValidToken(ServletRequest request,
                                    ServletResponse response,
                                    FilterChain chain,
                                    HttpSession session,
                                    User user) throws IOException, ServletException {
        EnvironmentContext environmentContext = EnvironmentContext.getCurrent();
        environmentContext.setUser(user);
        chain.doFilter(new RequestWrapper((HttpServletRequest)request, session, user), response);
    }


    /**
     * Handle request with invalid token.
     * Method should commit response.
     *
     * @param request
     *         original request
     * @param response
     *         original response
     * @param chain
     *         filter chain
     * @param token
     *         invalid token.
     * @throws IOException
     * @throws ServletException
     */
    protected abstract void handleInvalidToken(ServletRequest request,
                                               ServletResponse response,
                                               FilterChain chain,
                                               String token) throws IOException, ServletException;

    /**
     * Handle request when token can't be extracted from request.
     * Method should commit response.
     *
     * @param request
     *         original request
     * @param response
     *         original response
     * @param chain
     *         filter chain
     * @throws IOException
     * @throws ServletException
     */
    protected abstract void handleMissingToken(ServletRequest request,
                                               ServletResponse response,
                                               FilterChain chain) throws IOException, ServletException;


    @Override
    public void destroy() {
    }
}
