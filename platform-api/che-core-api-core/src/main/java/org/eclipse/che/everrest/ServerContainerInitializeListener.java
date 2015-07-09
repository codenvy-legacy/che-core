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
import org.eclipse.che.commons.user.User;
import org.everrest.core.DependencySupplier;
import org.everrest.core.ResourceBinder;
import org.everrest.core.impl.ApplicationProviderBinder;
import org.everrest.core.impl.EverrestConfiguration;
import org.everrest.core.impl.EverrestProcessor;
import org.everrest.core.impl.ProviderBinder;
import org.everrest.core.impl.provider.json.JsonException;
import org.everrest.core.tools.SimplePrincipal;
import org.everrest.core.tools.SimpleSecurityContext;
import org.everrest.core.tools.WebApplicationDeclaredRoles;
import org.everrest.websockets.WSConnectionImpl;
import org.everrest.websockets.message.BaseTextDecoder;
import org.everrest.websockets.message.BaseTextEncoder;
import org.everrest.websockets.message.JsonMessageConverter;
import org.everrest.websockets.message.OutputMessage;
import org.everrest.websockets.message.RestInputMessage;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSession;
import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.DeploymentException;
import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;
import javax.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import static javax.websocket.server.ServerEndpointConfig.Builder.create;
import static javax.websocket.server.ServerEndpointConfig.Configurator;

import com.google.common.base.MoreObjects;

/**
 * @author andrew00x
 */
public class ServerContainerInitializeListener implements ServletContextListener {
    public static final String ENVIRONMENT_CONTEXT          = "ide.websocket." + EnvironmentContext.class.getName();
    public static final String EVERREST_PROCESSOR_ATTRIBUTE = EverrestProcessor.class.getName();
    public static final String HTTP_SESSION_ATTRIBUTE       = HttpSession.class.getName();
    public static final String EVERREST_CONFIG_ATTRIBUTE    = EverrestConfiguration.class.getName();
    public static final String EXECUTOR_ATTRIBUTE           = "everrest.Executor";
    public static final String SECURITY_CONTEXT             = SecurityContext.class.getName();

    private static final AtomicLong sequence = new AtomicLong(1);

    private WebApplicationDeclaredRoles webApplicationDeclaredRoles;
    private EverrestConfiguration       everrestConfiguration;
    private ServerEndpointConfig        wsServerEndpointConfig;
    private ServerEndpointConfig        eventbusServerEndpointConfig;
    private String                      websocketContext;

    @Override
    public final void contextInitialized(ServletContextEvent sce) {
        final ServletContext servletContext = sce.getServletContext();
        websocketContext = MoreObjects.firstNonNull(servletContext.getInitParameter("org.everrest.websocket.context"), "");
        webApplicationDeclaredRoles = new WebApplicationDeclaredRoles(servletContext);
        everrestConfiguration = (EverrestConfiguration)servletContext.getAttribute(EVERREST_CONFIG_ATTRIBUTE);
        if (everrestConfiguration == null) {
            everrestConfiguration = new EverrestConfiguration();
        }
        final ServerContainer serverContainer = (ServerContainer)servletContext.getAttribute("javax.websocket.server.ServerContainer");
        try {
            wsServerEndpointConfig = createWsServerEndpointConfig(servletContext);
            eventbusServerEndpointConfig = createEventbusServerEndpointConfig(servletContext);
            serverContainer.addEndpoint(wsServerEndpointConfig);
            serverContainer.addEndpoint(eventbusServerEndpointConfig);
        } catch (DeploymentException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (wsServerEndpointConfig != null) {
            ExecutorService executor = (ExecutorService)wsServerEndpointConfig.getUserProperties().get(EXECUTOR_ATTRIBUTE);
            if (executor != null) {
                executor.shutdownNow();
            }
        }
        if (eventbusServerEndpointConfig != null) {
            ExecutorService executor = (ExecutorService)eventbusServerEndpointConfig.getUserProperties().get(EXECUTOR_ATTRIBUTE);
            if (executor != null) {
                executor.shutdownNow();
            }
        }
    }

    protected ServerEndpointConfig createWsServerEndpointConfig(ServletContext servletContext) {
        final List<Class<? extends Encoder>> encoders = new LinkedList<>();
        final List<Class<? extends Decoder>> decoders = new LinkedList<>();
        encoders.add(OutputMessageEncoder.class);
        decoders.add(InputMessageDecoder.class);
        final ServerEndpointConfig endpointConfig = create(CheWSConnection.class, websocketContext+"/ws/{ws-id}")
                .configurator(createConfigurator()).encoders(encoders).decoders(decoders).build();
        endpointConfig.getUserProperties().put(EVERREST_PROCESSOR_ATTRIBUTE, getEverrestProcessor(servletContext));
        endpointConfig.getUserProperties().put(EVERREST_CONFIG_ATTRIBUTE, getEverrestConfiguration(servletContext));
        endpointConfig.getUserProperties().put(EXECUTOR_ATTRIBUTE, createExecutor(servletContext));
        return endpointConfig;
    }

    protected ServerEndpointConfig createEventbusServerEndpointConfig(ServletContext servletContext) {
        final List<Class<? extends Encoder>> encoders = new LinkedList<>();
        final List<Class<? extends Decoder>> decoders = new LinkedList<>();
        encoders.add(OutputMessageEncoder.class);
        decoders.add(InputMessageDecoder.class);
        final ServerEndpointConfig endpointConfig = create(CheWSConnection.class, websocketContext+"/eventbus/")
                .configurator(createConfigurator()).encoders(encoders).decoders(decoders).build();
        endpointConfig.getUserProperties().put(EVERREST_PROCESSOR_ATTRIBUTE, getEverrestProcessor(servletContext));
        endpointConfig.getUserProperties().put(EVERREST_CONFIG_ATTRIBUTE, getEverrestConfiguration(servletContext));
        endpointConfig.getUserProperties().put(EXECUTOR_ATTRIBUTE, createExecutor(servletContext));
        return endpointConfig;
    }


    private Configurator createConfigurator() {
        return new Configurator() {
            @Override
            public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
                super.modifyHandshake(sec, request, response);
                final HttpSession httpSession = (HttpSession)request.getHttpSession();
                if (httpSession != null) {
                    sec.getUserProperties().put(HTTP_SESSION_ATTRIBUTE, httpSession);
                }
                sec.getUserProperties().put(SECURITY_CONTEXT, createSecurityContext(request));
                sec.getUserProperties().put(ENVIRONMENT_CONTEXT, EnvironmentContext.getCurrent());
            }
        };
    }

    protected EverrestProcessor getEverrestProcessor(ServletContext servletContext) {

        final DependencySupplier dependencies = (DependencySupplier)servletContext.getAttribute(DependencySupplier.class.getName());
        final ResourceBinder resources = (ResourceBinder)servletContext.getAttribute(ResourceBinder.class.getName());
        final ProviderBinder providers = (ProviderBinder)servletContext.getAttribute(ApplicationProviderBinder.class.getName());
        final EverrestConfiguration copy = getEverrestConfiguration(servletContext);
        copy.setProperty(EverrestConfiguration.METHOD_INVOKER_DECORATOR_FACTORY, WebSocketMethodInvokerDecoratorFactory.class.getName());
        return new EverrestProcessor(resources, providers, dependencies, copy, null);
    }

    protected EverrestConfiguration getEverrestConfiguration(ServletContext servletContext) {
        return everrestConfiguration;
    }

    protected ExecutorService createExecutor(final ServletContext servletContext) {
        final EverrestConfiguration everrestConfiguration = getEverrestConfiguration(servletContext);
        return Executors.newFixedThreadPool(everrestConfiguration.getAsynchronousPoolSize(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                final Thread t =
                        new Thread(r, "everrest.WSConnection." + servletContext.getServletContextName() + sequence.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        });
    }

    protected SecurityContext createSecurityContext(final HandshakeRequest req) {
        final boolean isSecure = false; //todo: get somehow from request
        final String authType = "BASIC";
        final User user = EnvironmentContext.getCurrent().getUser();

        if (user == null) {
            return new SimpleSecurityContext(isSecure);
        }

        final Principal principal = new SimplePrincipal(user.getName());
        return new SecurityContext() {

            @Override
            public Principal getUserPrincipal() {
                return principal;
            }

            @Override
            public boolean isUserInRole(String role) {
                return user.isMemberOf(role);
            }

            @Override
            public boolean isSecure() {
                return isSecure;
            }

            @Override
            public String getAuthenticationScheme() {
                return authType;
            }
        };
    }


    public static class InputMessageDecoder extends BaseTextDecoder<RestInputMessage> {
        private final JsonMessageConverter jsonMessageConverter = new JsonMessageConverter();

        @Override
        public RestInputMessage decode(String s) throws DecodeException {
            try {
                return jsonMessageConverter.fromString(s, RestInputMessage.class);
            } catch (JsonException e) {
                throw new DecodeException(s, e.getMessage(), e);
            }
        }

        @Override
        public boolean willDecode(String s) {
            return true;
        }
    }

    public static class OutputMessageEncoder extends BaseTextEncoder<OutputMessage> {
        private final JsonMessageConverter jsonMessageConverter = new JsonMessageConverter();

        @Override
        public String encode(OutputMessage output) throws EncodeException {
            try {
                return jsonMessageConverter.toString(output);
            } catch (JsonException e) {
                throw new EncodeException(output, e.getMessage(), e);
            }
        }
    }
}
