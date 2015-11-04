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

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * Extract token first from query parameter then from session-access-key cookie.
 *
 * @author Sergii Kabashniuk
 */
public class CookiesTokenExtractor extends QueryParameterTokenExtractor {
    @Override
    public String getToken(HttpServletRequest req) {
        String token = super.getToken(req);
        if (token == null) {
            Cookie[] cookies = req.getCookies();
            if (cookies == null) {
                return null;
            }
            for (Cookie cookie : cookies) {
                if ("session-access-key".equalsIgnoreCase(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return token;
    }
}
