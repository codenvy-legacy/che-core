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
import org.eclipse.che.api.auth.TokenExtractor;
import org.pac4j.core.client.BaseClient;
import org.pac4j.core.client.ClientType;
import org.pac4j.core.client.RedirectAction;
import org.pac4j.core.context.J2EContext;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.http.client.indirect.IndirectHttpClient;
import org.pac4j.http.credentials.TokenCredentials;

/**
 * @author Sergii Kabashniuk
 */
public class RemoteAuthenticationClient extends IndirectHttpClient {

    private final TokenExtractor tokenExtractor = new CookiesTokenExtractor();

    @Override
    protected boolean isDirectRedirection() {
        return true;
    }

    @Override
    protected RedirectAction retrieveRedirectAction(WebContext context) {
        return RedirectAction.redirect("http://dev.box.com/api/auth/login");
    }

    @Override
    protected Credentials retrieveCredentials(WebContext context) throws RequiresHttpAction {
        return new TokenCredentials(tokenExtractor.getToken(((J2EContext)context).getRequest()), "client");
    }

    @Override
    protected BaseClient newClient() {
        return new RemoteAuthenticationClient();
    }

    @Override
    public ClientType getClientType() {
        return ClientType.BASICAUTH_BASED;
    }


}
