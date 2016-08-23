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
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriBuilder;

import org.eclipse.che.api.core.rest.HttpRequest.BodyWriter;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.commons.user.User;

/**
 * A base class for all implementations of {@link HttpRequestBase}.
 * 
 * @author Tareq Sharafy
 *
 * @param <RequestT>
 */
public class DefaultHttpRequestBase<RequestT extends HttpRequestBase<RequestT>> implements HttpRequestBase<RequestT> {

    private static final int DEFAULT_QUERY_PARAMS_LIST_SIZE = 5;

    protected final String url;
    protected int timeout;
    protected String method;
    protected List<Pair<String, ?>> queryParams;

    protected DefaultHttpRequestBase(String url, String method) {
        this.url = requireNonNull(url, "Required non-null url");
        this.method = method;
    }

    @SuppressWarnings("unchecked")
    @Override
    public RequestT setMethod(@NotNull String method) {
        this.method = requireNonNull(method, "Required non-null http method");
        return (RequestT) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public RequestT addQueryParam(@NotNull String name, @NotNull Object value) {
        requireNonNull(name, "Required non-null query parameter name");
        requireNonNull(value, "Required non-null query parameter value");
        if (queryParams == null) {
            queryParams = new ArrayList<>(DEFAULT_QUERY_PARAMS_LIST_SIZE);
        }
        queryParams.add(Pair.of(name, value));
        return (RequestT) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public RequestT setTimeout(int timeout) {
        this.timeout = timeout;
        return (RequestT) this;
    }

    /**
     * Do any required validations before th erequest is executed.
     */
    protected void beforeRequest() {
        if (method == null) {
            throw new IllegalStateException("Could not perform request, request method wasn't set");
        }
    }

    protected HttpResponse doGeneralRequest(int timeout, String url, String method, BodyWriter bodyWriter,
            Map<String, String> headers, List<Pair<String, ?>> queryParams) throws IOException {
        // Set the query parameters
        if (queryParams != null && !queryParams.isEmpty()) {
            final UriBuilder ub = UriBuilder.fromUri(url);
            for (Pair<String, ?> parameter : queryParams) {
                String name = URLEncoder.encode(parameter.first, "UTF-8");
                String value = parameter.second == null ? null
                        : URLEncoder.encode(String.valueOf(parameter.second), "UTF-8");
                ub.queryParam(name, value);
            }
            url = ub.build().toString();
        }
        // Initialize the connection
        URL urlObj = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
        conn.setConnectTimeout(timeout > 0 ? timeout : DEFAULT_TIMEOUT);
        conn.setReadTimeout(timeout > 0 ? timeout : DEFAULT_TIMEOUT);
        if (method != null) {
            conn.setRequestMethod(method);
        }
        // Set the authorization header if present
        String authToken = getAuthenticationToken(urlObj);
        if (authToken != null) {
            conn.setRequestProperty(HttpHeaders.AUTHORIZATION, authToken);
        }
        // Set all the custom headers
        if (headers != null) {
            headers.forEach(conn::setRequestProperty);
        }
        // Write the body
        if (bodyWriter != null) {
            conn.setDoOutput(true);
            bodyWriter.writeTo(conn.getOutputStream());
        }
        // The result
        return new URLConnectionHttpResponse(conn);
    }

    protected String getAuthenticationToken(URL urlObj) {
        final User user = EnvironmentContext.getCurrent().getUser();
        if (user != null) {
            return user.getToken();
        }
        return null;
    }

}
