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

import java.io.IOException;
import java.io.OutputStream;

import javax.validation.constraints.NotNull;

import com.google.common.annotations.Beta;

/**
 * Defines simple set of methods for requesting json objects.
 *
 * <p>
 * Unlike {@link HttpJsonHelper} - provides <i>builder-like</i> style for building requests and getting responses.
 *
 * <p>
 * Simple use-cases:
 * 
 * <pre>
 * {@code
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
 * }
 * </pre>
 *
 * <p>
 * Do not use this class for requesting content different from "application/json".
 *
 * @author Yevhenii Voevodin
 * @see HttpJsonRequestFactory
 */
@Beta
public interface HttpRequest extends HttpRequestBase<HttpRequest> {

    public interface BodyWriter {
        public void writeTo(OutputStream out) throws IOException;
    }

    /**
     * Copy the given input stream to the request body.
     *
     * @param bodyWriter
     *            write data to the request output stream.
     * @return this request instance
     * @throws NullPointerException
     *             when {@code body} is null
     */
    HttpRequest setBodyWriter(@NotNull BodyWriter bodyWriter);

    /**
     * Adds a header to the request.
     * 
     * @param name
     *            The name of the header.
     * @param value
     *            The value of the header.
     * @return this request instance
     */
    HttpRequest addHeader(@NotNull String name, String value);

    /**
     * Makes http request.
     * 
     * @return {@link HttpResponse} instance which represents response of this request
     * @throws IOException
     *             when any io error occurs
     */
    HttpResponse request() throws IOException;

}
