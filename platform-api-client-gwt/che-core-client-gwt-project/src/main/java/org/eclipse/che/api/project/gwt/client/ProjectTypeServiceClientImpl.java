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
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.AsyncRequestLoader;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.RestContext;

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

    private final String baseUrl;

    private final AsyncRequestFactory    asyncRequestFactory;
    private final DtoUnmarshallerFactory dtoUnmarshallerFactory;
    private       String                 extPath;
    private       String                 workspaceId;
    private final AsyncRequestLoader loader;

    @Inject
    protected ProjectTypeServiceClientImpl(@Named("cheExtensionPath") String extPath,
                                           @Named("workspaceId") String workspaceId,
                                           AsyncRequestLoader loader,
                                           AsyncRequestFactory asyncRequestFactory,
                                           DtoUnmarshallerFactory dtoUnmarshallerFactory) {
        this.extPath = extPath;
        this.workspaceId = workspaceId;
        this.loader = loader;
        this.asyncRequestFactory = asyncRequestFactory;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        baseUrl = extPath + "/project-type/" + workspaceId +"/";
    }

    @Override
    public Promise<List<ProjectTypeDefinition>> getProjectTypes() {
        return newPromise(new RequestCall<Array<ProjectTypeDefinition>>() {
            @Override
            public void makeCall(AsyncCallback<Array<ProjectTypeDefinition>> callback) {
                getProjectTypes(callback);
            }
        }).then(new Function<Array<ProjectTypeDefinition>, List<ProjectTypeDefinition>>() {
            @Override
            public List<ProjectTypeDefinition> apply(Array<ProjectTypeDefinition> arg) throws FunctionException {
                final List<ProjectTypeDefinition> descriptors = new ArrayList<>();
                for (ProjectTypeDefinition descriptor : arg.asIterable()) {
                    descriptors.add(descriptor);
                }
                return descriptors;
            }
        });
    }

    private void getProjectTypes(@Nonnull AsyncCallback<Array<ProjectTypeDefinition>> callback) {
        final String url = baseUrl;
        asyncRequestFactory.createGetRequest(url)
                           .header(ACCEPT, APPLICATION_JSON)
                           .loader(loader, "Getting info about registered project types...")
                           .send(newCallback(callback, dtoUnmarshallerFactory.newArrayUnmarshaller(ProjectTypeDefinition.class)));
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

    private void getProjectType(@Nonnull String machineId, @Nonnull AsyncCallback<ProjectTypeDefinition> callback) {
        final String url = baseUrl + '/' + machineId;
        asyncRequestFactory.createGetRequest(url)
                           .header(ACCEPT, APPLICATION_JSON)
                           .loader(loader, "Getting info about project type...")
                           .send(newCallback(callback, dtoUnmarshallerFactory.newUnmarshaller(ProjectTypeDefinition.class)));
    }
}
