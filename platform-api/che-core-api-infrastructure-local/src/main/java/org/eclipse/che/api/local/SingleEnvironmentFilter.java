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

import org.eclipse.che.commons.env.EnvironmentContext;

import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * Set up environment variable. Only for local packaging with single workspace. Don't use it in production packaging.
 *
 * @author andrew00x
 */
@Singleton
public class SingleEnvironmentFilter implements Filter {
    private String wsName;
    private String wsId;
    private String accountId;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        wsName = filterConfig.getInitParameter("ws-name");
        wsId = filterConfig.getInitParameter("ws-id");
        accountId = filterConfig.getInitParameter("account-id");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        final EnvironmentContext env = EnvironmentContext.getCurrent();
        try {
            env.setWorkspaceName(wsName);
            env.setWorkspaceId(wsId);
            env.setAccountId(accountId);
            chain.doFilter(request, response);
        } finally {
            EnvironmentContext.reset();
        }
    }

    @Override
    public void destroy() {
    }

}
