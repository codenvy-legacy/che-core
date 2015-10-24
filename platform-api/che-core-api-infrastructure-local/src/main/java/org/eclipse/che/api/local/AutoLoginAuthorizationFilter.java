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
package org.eclipse.che.api.local;

import static com.google.common.base.MoreObjects.firstNonNull;

import org.eclipse.che.api.auth.DefaultAuthorizationFilter;
import org.eclipse.che.api.auth.TokenExtractor;
import org.eclipse.che.api.auth.TokenManager;
import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.user.server.dao.UserDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Sergii Kabashniuk
 */
@Singleton
public class AutoLoginAuthorizationFilter extends DefaultAuthorizationFilter {
    private static final Logger LOG = LoggerFactory.getLogger(AutoLoginAuthorizationFilter.class);

    @Inject
    TokenExtractor extractor;
    @Inject
    TokenManager   tokenManager;
    @Inject
    UserDao        userDao;


    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest)request;
        String existedToken = extractor.getToken(httpServletRequest);
        if (existedToken == null || !tokenManager.isValid(existedToken)) {
            try {
                String token = tokenManager.createToken(userDao.getByAlias("che@eclipse.org").getId());

                Cookie cookie = new Cookie("session-access-key", token);
                cookie.setPath("/");
                cookie.setMaxAge(-1);
                cookie.setSecure(request.isSecure());
                cookie.setHttpOnly(true);
                ((HttpServletResponse)response).addCookie(cookie);
                //remove existed invalid token.
                if (existedToken != null) {
                    Cookie existedCookie = new Cookie("session-access-key", existedToken);
                    existedCookie.setPath("/");
                    existedCookie.setMaxAge(0);
                    existedCookie.setSecure(request.isSecure());
                    existedCookie.setHttpOnly(true);
                    ((HttpServletResponse)response).addCookie(existedCookie);
                }

                super.doFilter(new WrappedRequest(httpServletRequest, cookie), response, chain);
            } catch (ApiException e) {
                LOG.error(e.getLocalizedMessage(), e);
                handleInvalidToken(request, response, chain, "");
            }
        } else {
            super.doFilter(request, response, chain);
        }
    }

    private static final class WrappedRequest extends HttpServletRequestWrapper {

        private final Cookie[] result;

        /**
         * Constructs a request object wrapping the given request.
         *
         * @param request
         * @throws IllegalArgumentException
         *         if the request is null
         */
        public WrappedRequest(HttpServletRequest request, Cookie additionalCookie) {
            super(request);
            Cookie[] original = firstNonNull(request.getCookies(), new Cookie[0]);
            result = new Cookie[original.length + 1];
            System.arraycopy(original, 0, result, 0, original.length);
            result[original.length] = additionalCookie;

        }

        @Override
        public Cookie[] getCookies() {
            return result;
        }
    }
}
