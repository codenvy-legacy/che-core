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
package org.eclipse.che.ide.projecttype.wizard.presenter;

import org.eclipse.che.api.builder.dto.BuilderDescriptor;
import org.eclipse.che.api.builder.dto.BuilderEnvironment;
import org.eclipse.che.api.builder.gwt.client.BuilderServiceClient;
import org.eclipse.che.ide.json.JsonHelper;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;
import org.eclipse.che.ide.util.loging.Log;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helps to get name of the builder's default environment by builder name.
 *
 * @author Artem Zatsarynnyy
 */
@Singleton
class BuilderRegistry {
    private final Map<String, String> environments;

    @Inject
    BuilderRegistry(BuilderServiceClient builderServiceClient, DtoUnmarshallerFactory dtoUnmarshallerFactory) {
        environments = new HashMap<>();

        final Unmarshallable<List<BuilderDescriptor>> unmarshaller = dtoUnmarshallerFactory.newListUnmarshaller(BuilderDescriptor.class);
        builderServiceClient.getRegisteredServers(new AsyncRequestCallback<List<BuilderDescriptor>>(unmarshaller) {
            @Override
            protected void onSuccess(List<BuilderDescriptor> result) {
                for (BuilderDescriptor builderDescriptor : result) {
                    for (BuilderEnvironment environment : builderDescriptor.getEnvironments().values()) {
                        if (environment.getIsDefault()) {
                            environments.put(builderDescriptor.getName(), environment.getDisplayName());
                            break;
                        }
                    }
                }
            }

            @Override
            protected void onFailure(Throwable exception) {
                Log.error(getClass(), JsonHelper.parseJsonMessage(exception.getMessage()));
            }
        });
    }

    /** Returns display name of the default environment for the given builder. */
    String getDefaultEnvironmentName(@Nonnull String builderName) {
        return environments.get(builderName);
    }
}
