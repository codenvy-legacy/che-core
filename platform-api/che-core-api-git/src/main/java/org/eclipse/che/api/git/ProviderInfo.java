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
package org.eclipse.che.api.git;

/**
 * @author Max Shaposhnik
 *
 */
public class ProviderInfo {

    private String authenticateUrl;

    private String providerId;

    public ProviderInfo(String authenticateUrl, String providerId) {
        this.authenticateUrl = authenticateUrl;
        this.providerId = providerId;
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
