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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;

import org.eclipse.che.commons.lang.Pair;

/**
 * A utility helper for providing HTTP clients.
 * 
 * @author Tareq Sharafy (tareq.sha@gmail.com)
 */
public interface HttpHelper {

    public interface HttpResponse extends AutoCloseable {

        int getStatusCode();

        String getContentType();

        InputStream getInputStream();

        void close();

    }

    /**
     * Issue a general HTTP request and return the response.
     * 
     * @param timeout
     *            Request timeout.
     * @param url
     *            A full URI to use for the request.
     * @param method
     *            Request method (GET, POST, PUT, ...)
     * @param body
     *            An optional stream to send in the request body. Can be null.
     * @param headers
     *            Request headers as pairs of header-name - header-value. The same name can appear several times.
     * @return The server response.
     * @throws MalformedURLException
     * @throws IOException
     */
    public HttpResponse request(int timeout, URI url, String method, InputStream body, List<Pair<String, ?>> headers)
            throws IOException;

}
