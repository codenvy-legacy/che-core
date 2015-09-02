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
package org.eclipse.che.api.project.gwt.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.eclipse.che.api.project.shared.dto.ProjectTypeDefinition;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper.RequestCall;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.AsyncRequestLoader;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static org.eclipse.che.api.promises.client.callback.PromiseHelper.newCallback;
import static org.eclipse.che.api.promises.client.callback.PromiseHelper.newPromise;
import static org.eclipse.che.ide.MimeType.APPLICATION_JSON;
import static org.eclipse.che.ide.rest.HTTPHeader.ACCEPT;

/**
 * The implementation of {@link ProjectTypeServiceClient}.
 *
 * @author Artem Zatsarynnyy
 */
public class ProjectTypeServiceClientImpl implements ProjectTypeServiceClient {
    private final AsyncRequestLoader     loader;
    private final AsyncRequestFactory    asyncRequestFactory;
    private final DtoUnmarshallerFactory dtoUnmarshallerFactory;
    private final String                 baseUrl;

    @Inject
    protected ProjectTypeServiceClientImpl(@Named("cheExtensionPath") String extPath,
                                           @Named("workspaceId") String workspaceId,
                                           AsyncRequestLoader loader,
                                           AsyncRequestFactory asyncRequestFactory,
                                           DtoUnmarshallerFactory dtoUnmarshallerFactory) {
        this.loader = loader;
        this.asyncRequestFactory = asyncRequestFactory;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        baseUrl = extPath + "/project-type/" + workspaceId;
    }

    @Override
    public Promise<List<ProjectTypeDefinition>> getProjectTypes() {
        return newPromise(new RequestCall<List<ProjectTypeDefinition>>() {
            @Override
            public void makeCall(AsyncCallback<List<ProjectTypeDefinition>> callback) {
                getProjectTypes(callback);
            }
        }).then(new Function<List<ProjectTypeDefinition>, List<ProjectTypeDefinition>>() {
            @Override
            public List<ProjectTypeDefinition> apply(List<ProjectTypeDefinition> arg) throws FunctionException {
                final List<ProjectTypeDefinition> descriptors = new ArrayList<>();
                for (ProjectTypeDefinition descriptor : arg) {
                    descriptors.add(descriptor);
                }
                return descriptors;
            }
        });
    }

    private void getProjectTypes(@Nonnull AsyncCallback<List<ProjectTypeDefinition>> callback) {
        final String url = baseUrl;
        asyncRequestFactory.createGetRequest(url)
                           .header(ACCEPT, APPLICATION_JSON)
                           .loader(loader, "Getting info about registered project types...")
                           .send(newCallback(callback, dtoUnmarshallerFactory.newListUnmarshaller(ProjectTypeDefinition.class)));
    }

    @Override
    public Promise<ProjectTypeDefinition> getProjectType(final String id) {
        return newPromise(new RequestCall<ProjectTypeDefinition>() {
            @Override
            public void makeCall(AsyncCallback<ProjectTypeDefinition> callback) {
                getProjectType(id, callback);
            }
        });
    }

    private void getProjectType(@Nonnull String id, @Nonnull AsyncCallback<ProjectTypeDefinition> callback) {
        final String url = baseUrl + '/' + id;
        asyncRequestFactory.createGetRequest(url)
                           .header(ACCEPT, APPLICATION_JSON)
                           .loader(loader, "Getting info about project type...")
                           .send(newCallback(callback, dtoUnmarshallerFactory.newUnmarshaller(ProjectTypeDefinition.class)));
    }
}
