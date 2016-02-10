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
package org.eclipse.che.ide.bootstrap;

import org.eclipse.che.api.user.gwt.client.UserProfileServiceClient;
import org.eclipse.che.api.user.shared.dto.ProfileDescriptor;
import org.eclipse.che.ide.api.app.CurrentUser;
import org.eclipse.che.ide.api.component.Component;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.util.loging.Log;
import com.google.gwt.core.client.Callback;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author Evgen Vidolob
 */
@Singleton
public class ProfileComponent implements Component {
    private final DtoUnmarshallerFactory   dtoUnmarshallerFactory;
    private final UserProfileServiceClient userProfileService;
    private final CurrentUser              currentUser;

    @Inject
    public ProfileComponent(DtoUnmarshallerFactory dtoUnmarshallerFactory,
                            UserProfileServiceClient userProfileService, CurrentUser currentUser) {
        this.userProfileService = userProfileService;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.currentUser = currentUser;
    }

    @Override
    public void start(final Callback<Component, Exception> callback) {
        AsyncRequestCallback<ProfileDescriptor> asyncRequestCallback = new AsyncRequestCallback<ProfileDescriptor>(
                dtoUnmarshallerFactory.newUnmarshaller(ProfileDescriptor.class)) {
            @Override
            protected void onSuccess(final ProfileDescriptor profile) {
                currentUser.setProfile(profile);
                callback.onSuccess(ProfileComponent.this);
            }

            @Override
            protected void onFailure(Throwable error) {
                Log.error(ProfileComponent.class, "Unable to get Profile", error);
                callback.onFailure(new Exception("Unable to get Profile", error));
            }
        };
        userProfileService.getCurrentProfile(asyncRequestCallback);
    }
}
