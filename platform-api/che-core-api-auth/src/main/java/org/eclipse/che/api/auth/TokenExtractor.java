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

import javax.servlet.http.HttpServletRequest;

/** Allow extract authentication token from request. */
public interface TokenExtractor {
    /**
     * Extract token from request.
     *
     * @param req
     *         - request object.
     * @return - token if it was found, null otherwise.
     */
    String getToken(HttpServletRequest req);
}
