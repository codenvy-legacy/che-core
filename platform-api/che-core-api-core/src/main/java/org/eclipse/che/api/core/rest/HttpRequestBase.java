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

import java.util.Map;
import java.util.Objects;

import javax.validation.constraints.NotNull;
import javax.ws.rs.HttpMethod;

import com.google.common.annotations.Beta;

/**
 * Defines simple set of methods for requesting json objects.
 *
 * <p>Unlike {@link HttpJsonHelper} - provides <i>builder-like</i> style for building requests and getting responses.
 *
 * <p>Simple use-cases:
 * <pre>{@code
 *     // starting new workspace
 *     requestFactory.fromUri(apiEndpoint + "/workspace/" + id + "/runtime")
 *                   .setMethod("POST")
 *                   .addQueryParam("envName", envName)
 *                   .addQueryParam("accountId", accountId)
 *                   .request();
 *
 *     // getting user preferences
 *     Map<String, String> prefs = requestFactory.fromUri(apiEndpoint + "/profile/prefs")
 *                                               .setMethod("GET")
 *                                               .request()
 *                                               .asProperties();
 *
 *    // getting workspace
 *    UsersWorkspaceDto workspace = requestFactory.fromLink(getWorkspaceLink)
 *                                                .request()
 *                                                .asDto(UsersWorkspaceDto.class);
 * }</pre>
 *
 * <p>Do not use this class for requesting content different from "application/json".
 *
 * @author Yevhenii Voevodin
 * @see HttpJsonRequestFactory
 */
@Beta
public interface HttpRequestBase<RequestT extends HttpRequestBase<RequestT>> {

    static final int DEFAULT_TIMEOUT = 60 * 1000;

    /**
     * Sets http method to use in this request(e.g. {@link javax.ws.rs.HttpMethod#GET GET}).
     *
     * @param method
     *         http method
     * @return this request instance
     * @throws NullPointerException
     *         when {@code method} is null
     */
    RequestT setMethod(@NotNull String method);

    /**
     * Adds query parameter to the request.
     *
     * @param name
     *         query parameter name
     * @param value
     *         query parameter value
     * @return this request instance
     * @throws NullPointerException
     *         when either name or value is null
     */
    RequestT addQueryParam(@NotNull String name, @NotNull Object value);

    /**
     * Sets request timeout.
     *
     * @param timeout
     *         request timeout
     * @return this request instance
     */
    RequestT setTimeout(int timeout);

    /**
     * Uses {@link HttpMethod#GET} as a request method.
     *
     * @return this request instance
     */
    default RequestT useGetMethod() {
        return setMethod(HttpMethod.GET);
    }

    /**
     * Uses {@link HttpMethod#OPTIONS} as a request method.
     *
     * @return this request instance
     */
    default RequestT useOptionsMethod() {
        return setMethod(HttpMethod.OPTIONS);
    }

    /**
     * Uses {@link HttpMethod#POST} as a request method.
     *
     * @return this request instance
     */
    default RequestT usePostMethod() {
        return setMethod(HttpMethod.POST);
    }

    /**
     * Uses {@link HttpMethod#DELETE} as a request method.
     *
     * @return this request instance
     */
    default RequestT useDeleteMethod() {
        return setMethod(HttpMethod.DELETE);
    }

    /**
     * Uses {@link HttpMethod#PUT} as a request method.
     *
     * @return this request instance
     */
    default RequestT usePutMethod() {
        return setMethod(HttpMethod.PUT);
    }

    /**
     * Adds set of query parameters to this request.
     *
     * @param params
     *         query parameters map
     * @return this request instance
     */
    default RequestT addQueryParams(@NotNull Map<String, ?> params) {
        Objects.requireNonNull(params, "Required non-null query parameters");
        params.forEach(this::addQueryParam);
        @SuppressWarnings("unchecked")
        RequestT r = (RequestT) this; // to avoid suppressing on the whole method
        return r;
    }
}
