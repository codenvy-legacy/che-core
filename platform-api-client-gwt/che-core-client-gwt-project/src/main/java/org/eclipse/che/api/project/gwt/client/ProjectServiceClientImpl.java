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

import org.eclipse.che.api.machine.gwt.client.ExtServerStateController;
import org.eclipse.che.api.project.shared.dto.CopyOptions;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.MoveOptions;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.project.shared.dto.ProjectReference;
import org.eclipse.che.api.project.shared.dto.SourceEstimation;
import org.eclipse.che.api.project.shared.dto.TreeElement;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.workspace.shared.dto.ModuleConfigDto;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.api.workspace.shared.dto.SourceStorageDto;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper;
import org.eclipse.che.ide.MimeType;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.AsyncRequestLoader;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.websocket.Message;
import org.eclipse.che.ide.websocket.MessageBuilder;
import org.eclipse.che.ide.websocket.MessageBus;
import org.eclipse.che.ide.websocket.WebSocketException;
import org.eclipse.che.ide.websocket.rest.RequestCallback;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

import static com.google.gwt.http.client.RequestBuilder.DELETE;
import static com.google.gwt.http.client.RequestBuilder.POST;
import static com.google.gwt.http.client.RequestBuilder.PUT;
import static org.eclipse.che.api.promises.client.callback.PromiseHelper.newCallback;
import static org.eclipse.che.api.promises.client.callback.PromiseHelper.newPromise;
import static org.eclipse.che.ide.MimeType.APPLICATION_JSON;
import static org.eclipse.che.ide.rest.HTTPHeader.ACCEPT;
import static org.eclipse.che.ide.rest.HTTPHeader.CONTENTTYPE;
import static org.eclipse.che.ide.rest.HTTPHeader.CONTENT_TYPE;

/**
 * Implementation of {@link ProjectServiceClient}.
 *
 * @author Vitaly Parfonov
 * @author Artem Zatsarynnyy
 * @author Valeriy Svydenko
 */
public class ProjectServiceClientImpl implements ProjectServiceClient {
    private final String                   extPath;
    private final String                   workspaceId;
    private final String                   projectServicePath;
    private       ExtServerStateController extServerStateController;
    private final AsyncRequestLoader       loader;
    private final AsyncRequestFactory      asyncRequestFactory;
    private final DtoFactory               dtoFactory;
    private final DtoUnmarshallerFactory   dtoUnmarshaller;
    private final String                   baseHttpUrl;

    @Inject
    protected ProjectServiceClientImpl(@Named("cheExtensionPath") String extPath,
                                       @Named("workspaceId") String workspaceId,
                                       ExtServerStateController extServerStateController,
                                       AsyncRequestLoader loader,
                                       AsyncRequestFactory asyncRequestFactory,
                                       DtoFactory dtoFactory,
                                       DtoUnmarshallerFactory dtoUnmarshaller) {
        this.extPath = extPath;
        this.workspaceId = workspaceId;
        this.projectServicePath = "/project/" + workspaceId;
        this.extServerStateController = extServerStateController;
        this.loader = loader;
        this.asyncRequestFactory = asyncRequestFactory;
        this.dtoFactory = dtoFactory;
        this.dtoUnmarshaller= dtoUnmarshaller;

        baseHttpUrl = extPath + projectServicePath;
    }

    @Override
    public void getProjects(boolean includeAttributes, AsyncRequestCallback<List<ProjectDescriptor>> callback) {
        asyncRequestFactory.createGetRequest(baseHttpUrl + "?includeAttributes=" + includeAttributes)
                           .header(ACCEPT, MimeType.APPLICATION_JSON)
                           .loader(loader, "Getting projects...")
                           .send(callback);
    }

    @Override
    public Promise<List<ProjectDescriptor>> getProjects(boolean includeAttributes) {
        return newPromise(new AsyncPromiseHelper.RequestCall<List<ProjectDescriptor>>() {
            @Override
            public void makeCall(AsyncCallback<List<ProjectDescriptor>> callback) {
                        getProjects(false, newCallback(callback, dtoUnmarshaller.newListUnmarshaller(ProjectDescriptor.class)));
            }
        });
    }

    @Override
    public void getProjectsInSpecificWorkspace(String wsId, AsyncRequestCallback<List<ProjectReference>> callback) {
        final String requestUrl = extPath + "/project/" + wsId;
        asyncRequestFactory.createGetRequest(requestUrl)
                           .header(ACCEPT, MimeType.APPLICATION_JSON)
                           .loader(loader, "Getting projects...")
                           .send(callback);
    }

    @Override
    public void cloneProjectToCurrentWorkspace(String srcWorkspaceId, String srcProjectPath, String newNameForProject,
                                               AsyncRequestCallback<String> callback) {
        final String requestUrl = extPath + "/vfs/" + workspaceId + "/v2/clone" + "?srcVfsId=" + srcWorkspaceId +
                                  "&srcPath=" + srcProjectPath +
                                  "&parentPath=/" +
                                  "&name=" + newNameForProject;

        asyncRequestFactory.createPostRequest(requestUrl, null)
                           .header(ACCEPT, MimeType.APPLICATION_JSON)
                           .loader(loader, "Copying project...")
                           .send(callback);
    }

    @Override
    public void getProject(String path, AsyncRequestCallback<ProjectDescriptor> callback) {
        final String requestUrl = baseHttpUrl + normalizePath(path);
        asyncRequestFactory.createGetRequest(requestUrl)
                           .header(ACCEPT, MimeType.APPLICATION_JSON)
                           .loader(loader, "Getting project...")
                           .send(callback);
    }

    @Override
    public void getItem(String path, AsyncRequestCallback<ItemReference> callback) {
        final String requestUrl = baseHttpUrl + "/item" + normalizePath(path);
        asyncRequestFactory.createGetRequest(requestUrl)
                           .header(ACCEPT, MimeType.APPLICATION_JSON)
                           .loader(loader, "Getting item...")
                           .send(callback);
    }

    @Override
    public void createProject(String name, ProjectConfigDto projectConfig, AsyncRequestCallback<ProjectDescriptor> callback) {
        final String requestUrl = baseHttpUrl + "?name=" + name;
        asyncRequestFactory.createPostRequest(requestUrl, projectConfig)
                           .header(ACCEPT, MimeType.APPLICATION_JSON)
                           .loader(loader, "Creating project...")
                           .send(callback);
    }

    @Override
    public void estimateProject(String path, String projectType, AsyncRequestCallback<Map<String, List<String>>> callback) {
        final String requestUrl = baseHttpUrl + "/estimate" + normalizePath(path) + "?type=" + projectType;
        asyncRequestFactory.createGetRequest(requestUrl)
                           .header(ACCEPT, MimeType.APPLICATION_JSON)
                           .loader(loader, "Estimating project...")
                           .send(callback);
    }

    @Override
    public void resolveSources(String path, AsyncRequestCallback<List<SourceEstimation>> callback) {
        final String requestUrl = baseHttpUrl + "/resolve" + normalizePath(path);
        asyncRequestFactory.createGetRequest(requestUrl)
                           .header(ACCEPT, MimeType.APPLICATION_JSON)
                           .loader(loader, "Resolving sources...")
                           .send(callback);
    }


    @Override
    public void getModules(String path, AsyncRequestCallback<List<ProjectDescriptor>> callback) {
        final String requestUrl = baseHttpUrl + "/modules" + normalizePath(path);
        asyncRequestFactory.createGetRequest(requestUrl)
                           .header(ACCEPT, MimeType.APPLICATION_JSON)
                           .send(callback);
    }

    @Override
    public void createModule(String parentProjectPath, ProjectConfigDto projectConfig, AsyncRequestCallback<ModuleConfigDto> callback) {
        final String requestUrl = baseHttpUrl + normalizePath(parentProjectPath);
        asyncRequestFactory.createPostRequest(requestUrl, projectConfig)
                           .header(ACCEPT, MimeType.APPLICATION_JSON)
                           .loader(loader, "Creating module...")
                           .send(callback);
    }

    @Override
    @Deprecated
    public void updateProject(String path, ProjectDescriptor descriptor, AsyncRequestCallback<ProjectDescriptor> callback) {
        final String requestUrl = baseHttpUrl + normalizePath(path);
        asyncRequestFactory.createRequest(PUT, requestUrl, descriptor, false)
                           .header(CONTENT_TYPE, MimeType.APPLICATION_JSON)
                           .header(ACCEPT, MimeType.APPLICATION_JSON)
                           .loader(loader, "Updating project...")
                           .send(callback);
    }

    @Override
    public void updateProject(String path, ProjectConfigDto projectConfig, AsyncRequestCallback<ProjectDescriptor> callback) {
        final String requestUrl = baseHttpUrl + normalizePath(path);
        asyncRequestFactory.createRequest(PUT, requestUrl, projectConfig, false)
                           .header(CONTENT_TYPE, MimeType.APPLICATION_JSON)
                           .header(ACCEPT, MimeType.APPLICATION_JSON)
                           .loader(loader, "Updating project...")
                           .send(callback);
    }

    @Override
    public void createFile(String parentPath, String name, String content, String contentType,
                           AsyncRequestCallback<ItemReference> callback) {
        final String requestUrl = baseHttpUrl + "/file" + normalizePath(parentPath) + "?name=" + name;
        // com.google.gwt.http.client.RequestBuilder doesn't allow to send requests without "Content-type" header. If header isn't set then
        // RequestBuilder adds "text/plain; charset=utf-8", seen javadocs for method send(). Let server resolve media type.
        // Agreement with server side: send "application/unknown" means we not set mime-type on client side in this case mime-type will be
        // resolved on server side
        if (contentType == null) {
            contentType = "application/unknown";
        }
        asyncRequestFactory.createPostRequest(requestUrl, null)
                           .header(CONTENT_TYPE, contentType)
                           .data(content)
                           .loader(loader, "Creating file...")
                           .send(callback);
    }

    @Override
    public void getFileContent(String path, AsyncRequestCallback<String> callback) {
        final String requestUrl = baseHttpUrl + "/file" + normalizePath(path);
        asyncRequestFactory.createGetRequest(requestUrl)
                           .loader(loader, "Loading file content...")
                           .send(callback);
    }

    @Override
    public void updateFile(String path, String content, String contentType, AsyncRequestCallback<Void> callback) {
        final String requestUrl = baseHttpUrl + "/file" + normalizePath(path);
        // com.google.gwt.http.client.RequestBuilder doesn't allow to send requests without "Content-type" header. If header isn't set then
        // RequestBuilder adds "text/plain; charset=utf-8", seen javadocs for method send(). Let server resolve media type.
        // Agreement with server side: send "application/unknown" means we not set mime-type on client side in this case mime-type will be
        // resolved on server side
        if (contentType == null) {
            contentType = "application/unknown";
        }
        asyncRequestFactory.createRequest(PUT, requestUrl, null, false)
                           .header(CONTENT_TYPE, contentType)
                           .data(content)
                           .send(callback);
    }

    @Override
    public void createFolder(String path, AsyncRequestCallback<ItemReference> callback) {
        final String requestUrl = baseHttpUrl + "/folder" + normalizePath(path);
        asyncRequestFactory.createPostRequest(requestUrl, null)
                           .loader(loader, "Creating folder...")
                           .send(callback);
    }

    @Override
    public void delete(String path, AsyncRequestCallback<Void> callback) {
        final String requestUrl = baseHttpUrl + normalizePath(path);
        asyncRequestFactory.createRequest(DELETE, requestUrl, null, false)
                           .loader(loader, "Deleting project...")
                           .send(callback);
    }

    @Override
    public void deleteModule(String path, String modulePath, AsyncRequestCallback<Void> callback) {
        final String requestUrl = baseHttpUrl + normalizePath(path) + "?module=" + modulePath;
        asyncRequestFactory.createRequest(DELETE, requestUrl, null, false)
                           .loader(loader, "Deleting module...")
                           .send(callback);
    }

    @Override
    public void copy(String path, String newParentPath, String newName, AsyncRequestCallback<Void> callback) {
        final String requestUrl = baseHttpUrl + "/copy" + normalizePath(path) + "?to=" + newParentPath;

        final CopyOptions copyOptions = dtoFactory.createDto(CopyOptions.class);
        copyOptions.setName(newName);
        copyOptions.setOverWrite(false);

        asyncRequestFactory.createPostRequest(requestUrl, copyOptions)
                           .loader(loader, "Copying item...")
                           .send(callback);
    }

    @Override
    public void move(String path, String newParentPath, String newName, AsyncRequestCallback<Void> callback) {
        final String requestUrl = baseHttpUrl + "/move" + normalizePath(path) + "?to=" + newParentPath;

        final MoveOptions moveOptions = dtoFactory.createDto(MoveOptions.class);
        moveOptions.setName(newName);
        moveOptions.setOverWrite(false);

        asyncRequestFactory.createPostRequest(requestUrl, moveOptions)
                           .loader(loader, "Moving item...")
                           .send(callback);
    }

    @Override
    public void rename(String path, String newName, String newMediaType, AsyncRequestCallback<Void> callback) {
        String requestUrl = baseHttpUrl + "/rename" + normalizePath(path) + "?name=" + newName;
        if (newMediaType != null) {
            requestUrl += "&mediaType=" + newMediaType;
        }
        asyncRequestFactory.createPostRequest(requestUrl, null)
                           .loader(loader, "Renaming item...")
                           .send(callback);
    }

    @Override
    public void importProject(String path, boolean force, SourceStorageDto sourceStorage, RequestCallback<Void> callback) {
        final StringBuilder requestUrl = new StringBuilder(projectServicePath);
        requestUrl.append("/import").append(normalizePath(path));
        if (force) {
            requestUrl.append("?force=true");
        }

        MessageBuilder builder = new MessageBuilder(POST, requestUrl.toString());
        builder.data(dtoFactory.toJson(sourceStorage)).header(CONTENTTYPE, APPLICATION_JSON);
        Message message = builder.build();

        sendMessageToWS(message, callback);
    }


    private void sendMessageToWS(final @NotNull Message message, final @NotNull RequestCallback<?> callback) {
        extServerStateController.getMessageBus().then(new Operation<MessageBus>() {
            @Override
            public void apply(MessageBus arg) throws OperationException {
                try {
                    arg.send(message, callback);
                } catch (WebSocketException e) {
                    throw new OperationException(e.getMessage(), e);
                }
            }
        });
    }

    @Override
    public void getChildren(String path, AsyncRequestCallback<List<ItemReference>> callback) {
        final String requestUrl = baseHttpUrl + "/children" + normalizePath(path);
        asyncRequestFactory.createGetRequest(requestUrl)
                           .header(ACCEPT, MimeType.APPLICATION_JSON)
                           .send(callback);
    }

    @Override
    public void getTree(String path, int depth, AsyncRequestCallback<TreeElement> callback) {
        final String requestUrl = baseHttpUrl + "/tree" + normalizePath(path) + "?depth=" + depth;
        asyncRequestFactory.createGetRequest(requestUrl)
                           .header(ACCEPT, MimeType.APPLICATION_JSON)
                           .send(callback);
    }

    @Override
    public void search(QueryExpression expression, AsyncRequestCallback<List<ItemReference>> callback) {
        final String requestUrl = baseHttpUrl + "/search" + normalizePath(expression.getPath());

        StringBuilder queryParameters = new StringBuilder();
        if (expression.getName() != null && !expression.getName().isEmpty()) {
            queryParameters.append("&name=").append(expression.getName());
        }
        if (expression.getMediaType() != null && !expression.getMediaType().isEmpty()) {
            queryParameters.append("&mediatype=").append(expression.getMediaType());
        }
        if (expression.getText() != null && !expression.getText().isEmpty()) {
            queryParameters.append("&text=").append(expression.getText());
        }
        if (expression.getMaxItems() != 0) {
            queryParameters.append("&maxItems=").append(expression.getMaxItems());
        }
        if (expression.getSkipCount() != 0) {
            queryParameters.append("&skipCount=").append(expression.getSkipCount());
        }

        asyncRequestFactory.createGetRequest(requestUrl + queryParameters.toString().replaceFirst("&", "?"))
                           .header(ACCEPT, MimeType.APPLICATION_JSON)
                           .send(callback);
    }

    @Override
    public void switchVisibility(String path, String visibility, AsyncRequestCallback<Void> callback) {
        final String requestUrl = baseHttpUrl + "/switch_visibility" + normalizePath(path) + "?visibility=" + visibility;
        asyncRequestFactory.createPostRequest(requestUrl, null)
                           .loader(loader, "Switching visibility...")
                           .send(callback);
    }

    /**
     * Normalizes the path by adding a leading '/' if it doesn't exist.
     * Also escapes some special characters.
     * <p/>
     * See following javascript functions for details:
     * escape() will not encode: @ * / +
     * encodeURI() will not encode: ~ ! @ # $ & * ( ) = : / , ; ? + '
     * encodeURIComponent() will not encode: ~ ! * ( ) '
     *
     * @param path
     *         path to normalize
     * @return normalized path
     */
    private String normalizePath(String path) {
        while (path.indexOf('+') >= 0) {
            path = path.replace("+", "%2B");
        }

        return path.startsWith("/") ? path : '/' + path;
    }
}
