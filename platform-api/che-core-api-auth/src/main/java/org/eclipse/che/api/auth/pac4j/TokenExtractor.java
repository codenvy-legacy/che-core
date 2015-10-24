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
package org.eclipse.che.api.auth.pac4j;

import org.eclipse.che.api.auth.CookiesTokenExtractor;
import org.pac4j.core.context.J2EContext;
import org.pac4j.core.context.WebContext;
import org.pac4j.http.credentials.TokenCredentials;
import org.pac4j.http.credentials.extractor.Extractor;

import javax.inject.Inject;

/**
 * @author Sergii Kabashniuk
 */
public class TokenExtractor implements Extractor<TokenCredentials> {
    private final org.eclipse.che.api.auth.TokenExtractor extractor = new CookiesTokenExtractor();

    private final String clientName;

    @Inject
    public TokenExtractor(String clientName) {
        this.clientName = clientName;
    }

    @Override
    public TokenCredentials extract(WebContext context) {
        return new TokenCredentials(extractor.getToken(((J2EContext)context).getRequest()), clientName);
    }
}
