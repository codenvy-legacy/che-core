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
package org.eclipse.che.api.core.rest;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.core.rest.shared.dto.ServiceDescriptor;
import org.eclipse.che.dto.server.DtoFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.HttpMethod;

/**
 * Provides basic functionality to access remote {@link Service Service}. Basically provides next information about {@code Service}:
 * <ul>
 * <li>URL of {@code Service}</li>
 * <li>Version of API</li>
 * <li>Optional description of {@code Service}</li>
 * <li>Set of {@link org.eclipse.che.api.core.rest.shared.dto.Link Link} to access {@code Service} functionality</li>
 * </ul>
 *
 * @author andrew00x
 * @see Service
 * @see #getLinks()
 */
public class RemoteServiceDescriptor {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteServiceDescriptor.class);

    protected final String baseUrl;
    private final   URL    baseUrlURL;

    // will be initialized when it is needed
    private volatile ServiceDescriptor serviceDescriptor;

    /**
     * Creates new descriptor of remote RESTful service.
     *
     * @throws java.lang.IllegalArgumentException
     *         if URL is invalid
     */
    public RemoteServiceDescriptor(String baseUrl) throws IllegalArgumentException {
        this.baseUrl = baseUrl;
        try {
            baseUrlURL = new URL(baseUrl);
            final String protocol = baseUrlURL.getProtocol();
            if (!(protocol.equals("http") || protocol.equals("https"))) {
                throw new IllegalArgumentException(String.format("Invalid URL: %s", baseUrl));
            }
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(String.format("Invalid URL: %s", baseUrl));
        }
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /** @see ServiceDescriptor#getLinks() */
    public List<Link> getLinks() throws ServerException, IOException {
        final List<Link> links = getServiceDescriptor().getLinks();
        // always copy list and links itself!
        final List<Link> copy = new ArrayList<>(links.size());
        for (Link link : links) {
            copy.add(DtoFactory.getInstance().clone(link));
        }
        return copy;
    }

    public Link getLink(String rel) throws ServerException, IOException {
        final Link link = getServiceDescriptor().getLink(rel);
        return link == null ? null : DtoFactory.getInstance().clone(link);
    }

    public ServiceDescriptor getServiceDescriptor() throws IOException, ServerException {
        ServiceDescriptor myServiceDescriptor = serviceDescriptor;
        if (myServiceDescriptor == null) {
            synchronized (this) {
                myServiceDescriptor = serviceDescriptor;
                if (myServiceDescriptor == null) {
                    try {
                        myServiceDescriptor = serviceDescriptor = HttpJsonHelper.options(getServiceDescriptorClass(), baseUrl);
                    } catch (NotFoundException | ConflictException | UnauthorizedException | ForbiddenException e) {
                        throw new ServerException(e.getServiceError());
                    }
                }
            }
        }
        return myServiceDescriptor;
    }

    protected Class<? extends ServiceDescriptor> getServiceDescriptorClass() {
        return ServiceDescriptor.class;
    }

    /** Checks service availability. */
    public boolean isAvailable() {
        LOG.debug("Testing availability {}", baseUrlURL);
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection)baseUrlURL.openConnection();
            conn.setConnectTimeout(3 * 1000);
            conn.setReadTimeout(10 * 1000);
            conn.setRequestMethod(HttpMethod.OPTIONS);
            int responseCode = conn.getResponseCode();
            LOG.debug("OPTIONS {} response {}", baseUrlURL, responseCode);
            return 200 == responseCode;
        } catch (IOException e) {
            LOG.warn(e.getLocalizedMessage());
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
