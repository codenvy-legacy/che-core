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
package org.eclipse.che.api.core.rest;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Simple implementation of {@link HttpJsonRequest} based on {@link HttpURLConnection}.
 *
 * <p>
 * The implementation is not thread-safe, instance of this class must be created each time when it's needed.
 *
 * <p>
 * The instance of this request is reusable, which means that it is possible to call {@link #request()} method more than
 * one time per instance
 *
 * @author Yevhenii Voevodin
 * @see DefaultHttpJsonRequestFactory
 */
public class DefaultHttpRequest extends DefaultHttpRequestBase<HttpRequest> implements HttpRequest {

    private BodyWriter bodyWriter;
    private Map<String, String> headers;

    public DefaultHttpRequest(String url) {
        super(url, null);
    }

    @Override
    public HttpRequest setBodyWriter(BodyWriter bodyWriter) {
        this.bodyWriter = Objects.requireNonNull(bodyWriter, "Required non-null body writer");
        return this;
    }

    @Override
    public HttpRequest addHeader(String name, String value) {
        requireNonNull(name, "Required non-null header name");
        if (headers == null) {
            headers = new HashMap<>();
        }
        headers.put(name, value);
        return this;
    }

    @Override
    public HttpResponse request() throws IOException {
        beforeRequest();
        return doGeneralRequest(timeout, url, method, bodyWriter, headers, queryParams);
    }

}
