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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.inject.Singleton;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.commons.user.User;

/**
 * Default implementation of {@link HttpHelper}.
 * 
 * @author Tareq Sharafy (tareq.sha@gmail.com)
 */
@Singleton
public class DefaultHttpHelper implements HttpHelper {

    public HttpResponse request(int timeout, URI url, String method, InputStream body, List<Pair<String, ?>> parameters)
            throws IOException {
        final String authToken = getAuthenticationToken();
        if ((parameters != null && !parameters.isEmpty()) || authToken != null) {
            final UriBuilder ub = UriBuilder.fromUri(url);
            // remove sensitive information from url.
            ub.replaceQueryParam("token", null);

            if (parameters != null) {
                for (Pair<String, ?> parameter : parameters) {
                    String name = URLEncoder.encode(parameter.first, StandardCharsets.UTF_8.name());
                    String value = parameter.second == null ? null
                            : URLEncoder.encode(String.valueOf(parameter.second), StandardCharsets.UTF_8.name());
                    ub.replaceQueryParam(name, value);
                }
            }
            url = ub.build();
        }
        final HttpURLConnection conn = (HttpURLConnection) new URL(url.toString()).openConnection();
        conn.setConnectTimeout(timeout > 0 ? timeout : 60000);
        conn.setReadTimeout(timeout > 0 ? timeout : 60000);
        conn.setRequestMethod(method);
        // drop a hint for server side that we want to receive application/json
        conn.addRequestProperty(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        if (authToken != null) {
            conn.setRequestProperty(HttpHeaders.AUTHORIZATION, authToken);
        }
        if (body != null) {
            conn.setDoOutput(true);

            if (HttpMethod.DELETE.equals(method)) { // to avoid jdk bug described here
                                                    // http://bugs.java.com/view_bug.do?bug_id=7157360
                conn.setRequestMethod(HttpMethod.POST);
                conn.setRequestProperty("X-HTTP-Method-Override", HttpMethod.DELETE);
            }

            try (OutputStream output = conn.getOutputStream()) {
                IOUtils.copy(body, output);
            }
        }

        final int statusCode;
        final InputStream stream;
        try {
            statusCode = conn.getResponseCode();
            InputStream tmpStream = conn.getInputStream();
            stream = (tmpStream != null ? tmpStream : conn.getErrorStream());
        } finally {
            conn.disconnect();
        }

        return new HttpResponse() {

            @Override
            public int getStatusCode() {
                return statusCode;
            }

            @Override
            public String getContentType() {
                return conn.getContentType();
            }

            @Override
            public InputStream getInputStream() {
                return stream;
            }

            @Override
            public void close() {
                conn.disconnect();
            }

        };

    }

    private String getAuthenticationToken() {
        User user = EnvironmentContext.getCurrent().getUser();
        if (user != null) {
            return user.getToken();
        }
        return null;
    }

}
