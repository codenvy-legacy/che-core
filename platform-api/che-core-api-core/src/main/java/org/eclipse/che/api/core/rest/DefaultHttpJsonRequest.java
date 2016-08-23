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

import com.google.common.io.CharStreams;

import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.core.rest.shared.dto.ServiceError;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.commons.user.User;
import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.dto.server.JsonArrayImpl;
import org.eclipse.che.dto.server.JsonSerializable;
import org.eclipse.che.dto.server.JsonStringMapImpl;

import javax.validation.constraints.NotNull;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Simple implementation of {@link HttpJsonRequest} based on {@link HttpURLConnection}.
 *
 * <p>The implementation is not thread-safe, instance of this class must be created each time when it's needed.
 *
 * <p>The instance of this request is reusable, which means that
 * it is possible to call {@link #request()} method more than one time per instance
 *
 * @author Yevhenii Voevodin
 * @see DefaultHttpJsonRequestFactory
 */
public class DefaultHttpJsonRequest implements HttpJsonRequest {
 
    private static final int      DEFAULT_QUERY_PARAMS_LIST_SIZE = 5;

    private final String url;

    private int                   timeout;
    private String                method;
    private Object                body;
    private BodyWriter            bodyWriter;
    private List<Pair<String, ?>> queryParams;
    private Map<String,String>    headers;

    protected DefaultHttpJsonRequest(String url) {
        this.url = requireNonNull(url, "Required non-null url");
        this.method = HttpMethod.GET; // Default is GET for convenience
    }

    protected DefaultHttpJsonRequest(Link link) {
        this(requireNonNull(link, "Required non-null link").getHref());
        this.method = link.getMethod();
    }

    @Override
    public HttpJsonRequest setMethod(@NotNull String method) {
        this.method = requireNonNull(method, "Required non-null http method");
        return this;
    }

    @Override
    public HttpJsonRequest setBody(@NotNull Object body) {
        this.body = requireNonNull(body, "Required non-null body");
        this.bodyWriter = null;
        return this;
    }

    @Override
    public HttpJsonRequest setBody(@NotNull Map<String, String> map) {
        this.body = new JsonStringMapImpl<>(requireNonNull(map, "Required non-null body"));
        this.bodyWriter = null;
        return this;
    }

    @Override
    public HttpJsonRequest setBody(@NotNull List<?> list) {
        this.body = new JsonArrayImpl<>(requireNonNull(list, "Required non-null body"));
        this.bodyWriter = null;
        return this;
    }

    @Override
    public HttpJsonRequest setBodyWriter(BodyWriter bodyWriter) {
        this.bodyWriter = Objects.requireNonNull(bodyWriter, "Required non-null body writer");
        this.body = null;
        return this;
    }

    @Override
    public HttpJsonRequest addQueryParam(@NotNull String name, @NotNull Object value) {
        requireNonNull(name, "Required non-null query parameter name");
        requireNonNull(value, "Required non-null query parameter value");
        if (queryParams == null) {
            queryParams = new ArrayList<>(DEFAULT_QUERY_PARAMS_LIST_SIZE);
        }
        queryParams.add(Pair.of(name, value));
        return this;
    }

    @Override
    public HttpJsonRequest addHeader(String name, String value) {
        requireNonNull(name, "Required non-null header name");
        this.headers.put(name, value);
        return this;
    }

    @Override
    public HttpJsonRequest setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    @Override
    public HttpJsonResponse request() throws IOException,
                                             ServerException,
                                             UnauthorizedException,
                                             ForbiddenException,
                                             NotFoundException,
                                             ConflictException,
                                             BadRequestException {
        if (method == null) {
            throw new IllegalStateException("Could not perform request, request method wasn't set");
        }
        if (bodyWriter != null) {
            throw new IllegalStateException("Could issue a JSON rqeuest in stream mode");
        }
        return doRequest(timeout, url, method, body, queryParams);
    }

    @Override
    public HttpResponse requestGeneral() throws IOException {
        if (method == null) {
            throw new IllegalStateException("Could not perform request, request method wasn't set");
        }
        if (bodyWriter == null) {
            throw new IllegalStateException("Could issue a stream rqeuest in JSON mode");
        }
        return doRequestGeneral(timeout, url, method, bodyWriter, headers, queryParams);
    }

    /**
     * Makes this request using {@link HttpURLConnection}.
     *
     * <p>Uses {@link HttpHeaders#AUTHORIZATION} header with value from {@link EnvironmentContext}.
     * <br>uses {@link HttpHeaders#ACCEPT} header with "application/json" value.
     * <br>Encodes query parameters in "UTF-8".
     *
     * @param timeout
     *         request timeout, used only if it is greater than 0
     * @param url
     *         request url
     * @param method
     *         request method
     * @param body
     *         request body, must be instance of {@link JsonSerializable}
     * @param parameters
     *         query parameters, may be null
     * @return response to this request
     * @throws IOException
     *         when connection content type is not "application/json"
     * @throws ServerException
     *         when response code is 500 or it is different from 400, 401, 403, 404, 409
     * @throws ForbiddenException
     *         when response code is 403
     * @throws NotFoundException
     *         when response code is 404
     * @throws UnauthorizedException
     *         when response code is 401
     * @throws ConflictException
     *         when response code is 409
     * @throws BadRequestException
     *         when response code is 400
     */
    protected DefaultHttpJsonResponse doRequest(int timeout,
                                      String url,
                                      String method,
                                      Object body,
                                      List<Pair<String, ?>> parameters) throws IOException,
                                                                               ServerException,
                                                                               ForbiddenException,
                                                                               NotFoundException,
                                                                               UnauthorizedException,
                                                                               ConflictException,
                                                                               BadRequestException {

        Map<String,String> headers = (this.headers == null ? new HashMap<>() : new HashMap<>(this.headers));
        headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        BodyWriter bw = null;
        if (body != null) {
            headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            bw = new BodyWriter() {
                @Override
                public void writeTo(OutputStream out) throws IOException {
                    out.write(DtoFactory.getInstance().toJson(body).getBytes());
                }
            };
        }

        try (HttpResponse conn = doRequestGeneral(timeout, url, method, bw, headers, parameters)) {
            final int responseCode = conn.getResponseCode();
            if ((responseCode / 100) != 2) {
                InputStream in = conn.getErrorStream();
                if (in == null) {
                    in = conn.getInputStream();
                }
                final String str;
                try (Reader reader = new InputStreamReader(in)) {
                    str = CharStreams.toString(reader);
                }
                final String contentType = conn.getContentType();
                if (contentType != null && contentType.startsWith(MediaType.APPLICATION_JSON)) {
                    final ServiceError serviceError = DtoFactory.getInstance().createDtoFromJson(str, ServiceError.class);
                    if (serviceError.getMessage() != null) {
                        if (responseCode == Response.Status.FORBIDDEN.getStatusCode()) {
                            throw new ForbiddenException(serviceError);
                        } else if (responseCode == Response.Status.NOT_FOUND.getStatusCode()) {
                            throw new NotFoundException(serviceError);
                        } else if (responseCode == Response.Status.UNAUTHORIZED.getStatusCode()) {
                            throw new UnauthorizedException(serviceError);
                        } else if (responseCode == Response.Status.CONFLICT.getStatusCode()) {
                            throw new ConflictException(serviceError);
                        } else if (responseCode == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
                            throw new ServerException(serviceError);
                        } else if (responseCode == Response.Status.BAD_REQUEST.getStatusCode()) {
                            throw new BadRequestException(serviceError);
                        }
                        throw new ServerException(serviceError);
                    }
                }
                // Can't parse content as json or content has format other we expect for error.
                throw new IOException(String.format("Failed access: %s, method: %s, response code: %d, message: %s",
                                                    UriBuilder.fromUri(url).replaceQuery("token").build(), method, responseCode, str));
            }
            final String contentType = conn.getContentType();
            if (contentType != null && !contentType.startsWith(MediaType.APPLICATION_JSON)) {
                throw new IOException(Response.Status.Family.familyOf(conn.getResponseCode()) + " [ Content-Type: " + contentType + " ]");
            }

            try (Reader reader = new InputStreamReader(conn.getInputStream())) {
                return new DefaultHttpJsonResponse(CharStreams.toString(reader), responseCode);
            }
        }
    }

    protected HttpResponse doRequestGeneral(int timeout, String url, String method, BodyWriter bodyWriter,
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
