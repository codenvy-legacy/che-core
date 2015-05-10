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
package org.eclipse.che.api.user.gwt.client;

import org.eclipse.che.api.user.shared.dto.UserDescriptor;
import org.eclipse.che.ide.MimeType;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.AsyncRequestLoader;
import org.eclipse.che.ide.rest.RestContext;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import javax.annotation.Nonnull;

import static org.eclipse.che.ide.rest.HTTPHeader.ACCEPT;
import static org.eclipse.che.ide.rest.HTTPHeader.CONTENT_TYPE;
import static com.google.gwt.http.client.RequestBuilder.DELETE;

/**
 * Implementation of {@link UserServiceClient}.
 *
 * @author Ann Shumilova
 */
public class UserServiceClientImpl implements UserServiceClient {
    private final String              USER;
    private final String              CREATE;
    private final String              FIND;
    private final String              PASSWORD;
    private final AsyncRequestLoader  loader;
    private final AsyncRequestFactory asyncRequestFactory;

    @Inject
    protected UserServiceClientImpl(@RestContext String restContext,
                                    AsyncRequestLoader loader,
                                    AsyncRequestFactory asyncRequestFactory) {
        this.loader = loader;
        this.asyncRequestFactory = asyncRequestFactory;
        USER = restContext + "/user/";
        CREATE = USER + "create";
        FIND = USER + "find";
        PASSWORD = USER + "password";
    }

    /** {@inheritDoc} */
    @Override
    public void createUser(@Nonnull String token, boolean isTemporary, AsyncRequestCallback<UserDescriptor> callback) {
        StringBuilder requestUrl = new StringBuilder(CREATE);
        requestUrl.append("?token=").append(token).append("&temporary=").append(isTemporary);

        asyncRequestFactory.createPostRequest(requestUrl.toString(), null)
                           .header(ACCEPT, MimeType.APPLICATION_JSON)
                           .loader(loader, "Creating user...")
                           .send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getCurrentUser(AsyncRequestCallback<UserDescriptor> callback) {

        asyncRequestFactory.createGetRequest(USER)
                           .header(ACCEPT, MimeType.APPLICATION_JSON)
                           .loader(loader, "Retrieving current user...")
                           .send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void updatePassword(@Nonnull String password, AsyncRequestCallback<Void> callback) {
        // TODO form parameter
        String requestUrl = PASSWORD + "?password=" + password;

        asyncRequestFactory.createPostRequest(requestUrl, null)
                           .header(CONTENT_TYPE, MimeType.APPLICATION_FORM_URLENCODED)
                           .loader(loader, "Updating user's password...")
                           .send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getUserById(@Nonnull String id, AsyncRequestCallback<UserDescriptor> callback) {
        String requestUrl = USER + id;

        asyncRequestFactory.createGetRequest(requestUrl)
                           .header(ACCEPT, MimeType.APPLICATION_JSON)
                           .loader(loader, "Retrieving user...")
                           .send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getUserByEmail(@Nonnull String email, AsyncRequestCallback<UserDescriptor> callback) {
        String requestUrl = FIND + "?email=" + email;

        asyncRequestFactory.createGetRequest(requestUrl)
                           .header(ACCEPT, MimeType.APPLICATION_JSON)
                           .loader(loader, "Retrieving user...")
                           .send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void removeUser(@Nonnull String id, AsyncRequestCallback<Void> callback) {
        String requestUrl = USER + id;

        asyncRequestFactory.createRequest(DELETE, requestUrl, null, false)
                           .loader(loader, "Deleting user...")
                           .send(callback);
    }

}
