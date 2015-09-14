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
package org.eclipse.che.api.machine.gwt.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.eclipse.che.api.machine.shared.dto.CommandDescriptor;
import org.eclipse.che.api.machine.shared.dto.CommandUpdate;
import org.eclipse.che.api.machine.shared.dto.NewCommand;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper.RequestCall;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.AsyncRequestLoader;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.RestContext;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

import static com.google.gwt.http.client.RequestBuilder.DELETE;
import static com.google.gwt.http.client.RequestBuilder.PUT;
import static org.eclipse.che.api.promises.client.callback.PromiseHelper.newCallback;
import static org.eclipse.che.api.promises.client.callback.PromiseHelper.newPromise;
import static org.eclipse.che.ide.MimeType.APPLICATION_JSON;
import static org.eclipse.che.ide.rest.HTTPHeader.ACCEPT;
import static org.eclipse.che.ide.rest.HTTPHeader.CONTENT_TYPE;

/**
 * Implementation for {@link CommandServiceClient}.
 *
 * @author Artem Zatsarynnyy
 */
public class CommandServiceClientImpl implements CommandServiceClient {
    private final DtoFactory             dtoFactory;
    private final DtoUnmarshallerFactory dtoUnmarshallerFactory;
    private final AsyncRequestFactory    asyncRequestFactory;
    private final AsyncRequestLoader     loader;
    private final String                 workspaceId;
    private final String                 baseHttpUrl;

    @Inject
    protected CommandServiceClientImpl(@RestContext String restContext,
                                       DtoFactory dtoFactory,
                                       DtoUnmarshallerFactory dtoUnmarshallerFactory,
                                       AsyncRequestFactory asyncRequestFactory,
                                       AsyncRequestLoader loader,
                                       @Named("workspaceId") String workspaceId) {
        this.dtoFactory = dtoFactory;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.asyncRequestFactory = asyncRequestFactory;
        this.loader = loader;
        this.workspaceId = workspaceId;
        this.baseHttpUrl = restContext + "/command";
    }

    @Override
    public Promise<CommandDescriptor> createCommand(@NotNull final String name,
                                                    @NotNull final String commandLine,
                                                    @NotNull final String type) {
        return newPromise(new RequestCall<CommandDescriptor>() {
            @Override
            public void makeCall(AsyncCallback<CommandDescriptor> callback) {
                createCommand(workspaceId, name, commandLine, type, callback);
            }
        });
    }

    private void createCommand(@NotNull final String workspaceId,
                               @NotNull final String name,
                               @NotNull final String commandLine,
                               @NotNull final String type,
                               @NotNull AsyncCallback<CommandDescriptor> callback) {
        final NewCommand request = dtoFactory.createDto(NewCommand.class)
                                             .withName(name)
                                             .withCommandLine(commandLine)
                                             .withType(type);

        asyncRequestFactory.createPostRequest(baseHttpUrl + '/' + workspaceId, request)
                           .header(ACCEPT, APPLICATION_JSON)
                           .header(CONTENT_TYPE, APPLICATION_JSON)
                           .loader(loader, "Creating command...")
                           .send(newCallback(callback, dtoUnmarshallerFactory.newUnmarshaller(CommandDescriptor.class)));
    }

    @Override
    public Promise<List<CommandDescriptor>> getCommands() {
        return newPromise(new RequestCall<List<CommandDescriptor>>() {
            @Override
            public void makeCall(AsyncCallback<List<CommandDescriptor>> callback) {
                getCommands(workspaceId, callback);
            }
        }).then(new Function<List<CommandDescriptor>, List<CommandDescriptor>>() {
            @Override
            public List<CommandDescriptor> apply(List<CommandDescriptor> arg) throws FunctionException {
                final ArrayList<CommandDescriptor> descriptors = new ArrayList<>();
                for (CommandDescriptor descriptor : arg) {
                    descriptors.add(descriptor);
                }
                return descriptors;
            }
        });
    }

    private void getCommands(@NotNull String workspaceId, @NotNull AsyncCallback<List<CommandDescriptor>> callback) {
        final String url = baseHttpUrl + '/' + workspaceId + "/all";
        asyncRequestFactory.createGetRequest(url)
                           .header(ACCEPT, APPLICATION_JSON)
                           .loader(loader, "Getting commands...")
                           .send(newCallback(callback, dtoUnmarshallerFactory.newListUnmarshaller(CommandDescriptor.class)));
    }

    @Override
    public Promise<CommandDescriptor> updateCommand(@NotNull final String id,
                                                    @NotNull final String name,
                                                    @NotNull final String commandLine) {
        return newPromise(new RequestCall<CommandDescriptor>() {
            @Override
            public void makeCall(AsyncCallback<CommandDescriptor> callback) {
                updateCommand(id, name, commandLine, callback);
            }
        });
    }

    private void updateCommand(@NotNull final String id,
                               @NotNull final String name,
                               @NotNull final String commandLine,
                               @NotNull AsyncCallback<CommandDescriptor> callback) {
        final CommandUpdate request = dtoFactory.createDto(CommandUpdate.class)
                                                .withId(id)
                                                .withName(name)
                                                .withCommandLine(commandLine);

        asyncRequestFactory.createRequest(PUT, baseHttpUrl, request, false)
                           .header(ACCEPT, APPLICATION_JSON)
                           .header(CONTENT_TYPE, APPLICATION_JSON)
                           .loader(loader, "Updating command...")
                           .send(newCallback(callback, dtoUnmarshallerFactory.newUnmarshaller(CommandDescriptor.class)));
    }

    @Override
    public Promise<Void> removeCommand(@NotNull final String id) {
        return newPromise(new RequestCall<Void>() {
            @Override
            public void makeCall(AsyncCallback<Void> callback) {
                removeCommand(id, callback);
            }
        });
    }

    private void removeCommand(@NotNull String commandId, @NotNull AsyncCallback<Void> callback) {
        asyncRequestFactory.createRequest(DELETE, baseHttpUrl + '/' + commandId, null, false)
                           .loader(loader, "Deleting command...")
                           .send(newCallback(callback));
    }
}
