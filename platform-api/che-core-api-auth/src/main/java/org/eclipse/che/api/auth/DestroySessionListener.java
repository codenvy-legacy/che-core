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

import com.google.inject.Injector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * Removes HttpSession from  client session store, if it invalidated by http container.
 *
 * @author Andrey Parfonov
 * @author Sergii Kabashniuk
 */
public class DestroySessionListener implements HttpSessionListener {
    static final         String INJECTOR_NAME = Injector.class.getName();
    private static final Logger LOG           = LoggerFactory.getLogger(DestroySessionListener.class);

    @Override
    public final void sessionCreated(HttpSessionEvent se) {
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        LOG.debug("Removing session {} from client session store", se.getSession().getId());
        SessionStore sessionStore = getInstance(SessionStore.class, se.getSession().getServletContext());
        if (sessionStore != null) {
            sessionStore.removeSessionById(se.getSession().getId());
        } else {
            LOG.error("Unable to remove session from store. Session store is not configured in servlet context.");
        }
    }

    /** Searches  component in servlet context when with help of guice injector. */
    private <T> T getInstance(Class<T> type, ServletContext servletContext) {
        T result = (T)servletContext.getAttribute(type.getName());
        if (result == null) {
            Injector injector = (Injector)servletContext.getAttribute(INJECTOR_NAME);
            if (injector != null) {
                result = injector.getInstance(type);
            }
        }
        return result;

    }

}
