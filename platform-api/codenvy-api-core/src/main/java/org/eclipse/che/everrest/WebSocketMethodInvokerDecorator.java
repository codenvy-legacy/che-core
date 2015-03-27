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
package org.eclipse.che.everrest;

import org.eclipse.che.commons.env.EnvironmentContext;

import org.everrest.core.ApplicationContext;
import org.everrest.core.impl.method.MethodInvokerDecorator;
import org.everrest.core.method.MethodInvoker;
import org.everrest.core.resource.GenericMethodResource;
import org.everrest.websockets.WSConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Intended to prepare environment to invoke resource method when request received through web socket connection.
 *
 * @author andrew00x
 */
class WebSocketMethodInvokerDecorator extends MethodInvokerDecorator {
    private static final Logger LOG = LoggerFactory.getLogger(WebSocketMethodInvokerDecorator.class);

    WebSocketMethodInvokerDecorator(MethodInvoker decoratedInvoker) {
        super(decoratedInvoker);
    }

    @Override
    public Object invokeMethod(Object resource, GenericMethodResource genericMethodResource, ApplicationContext context) {
        WSConnection wsConnection = (WSConnection)org.everrest.core.impl.EnvironmentContext.getCurrent().get(WSConnection.class);
        if (wsConnection != null) {


            try {
                EnvironmentContext.setCurrent((EnvironmentContext)wsConnection.getAttribute(
                        CodenvyEverrestWebSocketServlet.ENVIRONMENT_CONTEXT));

                LOG.debug("Websocket {} in http session {} context of ws {} is temporary {}",
                         wsConnection.getId(),
                         wsConnection.getHttpSession().getId(),
                         EnvironmentContext.getCurrent().getWorkspaceName(),
                         EnvironmentContext.getCurrent().isWorkspaceTemporary());
                return super.invokeMethod(resource, genericMethodResource, context);
            } finally {
                EnvironmentContext.reset();
            }
        }
        return super.invokeMethod(resource, genericMethodResource, context);
    }
}
