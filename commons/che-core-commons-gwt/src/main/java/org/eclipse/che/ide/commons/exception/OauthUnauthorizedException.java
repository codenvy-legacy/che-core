/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.commons.exception;

/**
 * @author Max Shaposhnik
 *
 */
public class OauthUnauthorizedException extends UnauthorizedException {

    private String providerId;

    private String authenticateUrl;

    public OauthUnauthorizedException(String providerId, String authenticateUrl, String message) {
        super(message);
        this.providerId = providerId;
        this.authenticateUrl = authenticateUrl;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getAuthenticateUrl() {
        return authenticateUrl;
    }

    public void setAuthenticateUrl(String authenticateUrl) {
        this.authenticateUrl = authenticateUrl;
    }
}
