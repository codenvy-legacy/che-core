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

import com.google.common.annotations.Beta;

import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.dto.server.JsonSerializable;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;
import java.util.Map;

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
public interface HttpJsonRequest extends HttpRequestBase<HttpJsonRequest> {

    /**
     * Sets request body.
     *
     * @param body
     *         should be instance of {@link JsonSerializable}
     * @return this request instance
     * @throws NullPointerException
     *         when {@code body} is null
     */
    HttpJsonRequest setBody(@NotNull Object body);

    /**
     * Sets given string map as request body.
     *
     * @param map
     *         request body
     * @return this request instance
     * @throws NullPointerException
     *         when {@code body} is null
     */
    HttpJsonRequest setBody(@NotNull Map<String, String> map);

    /**
     * Sets given list as request body.
     *
     * <p>List should contain only {@link JsonSerializable} elements
     *
     * @param list
     *         list of {@link JsonSerializable}
     * @return this request instance
     * @throws NullPointerException
     *         when {@code body} is null
     */
    HttpJsonRequest setBody(@NotNull List<?> list);


    /**
     * Makes http request with content type "application/json" and authorization headers
     * based on current {@link EnvironmentContext#getCurrent() context}.
     *
     * @return {@link HttpJsonResponse} instance which represents response of this request
     * @throws IOException
     *          when server response content type is different from "application/json"(Not acceptable)
     * @throws IOException
     *          when any io error occurs
     * @throws ServerException
     *          when response code is 500 or it is different from 400, 401, 403, 404, 409
     * @throws ForbiddenException
     *          when response code is 403
     * @throws NotFoundException
     *          when response code is 404
     * @throws UnauthorizedException
     *          when response code is 401
     * @throws ConflictException
     *          when response code is 409
     * @throws BadRequestException
     *          when response code is 400
     */
    HttpJsonResponse request() throws IOException,
                                      ServerException,
                                      UnauthorizedException,
                                      ForbiddenException,
                                      NotFoundException,
                                      ConflictException,
                                      BadRequestException;

}
