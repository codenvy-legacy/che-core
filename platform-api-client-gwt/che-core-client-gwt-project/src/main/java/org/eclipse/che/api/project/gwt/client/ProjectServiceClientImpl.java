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

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.eclipse.che.api.project.shared.dto.ImportProject;
import org.eclipse.che.api.project.shared.dto.ImportResponse;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.NewProject;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.project.shared.dto.ProjectReference;
import org.eclipse.che.api.project.shared.dto.ProjectUpdate;
//import org.eclipse.che.api.project.shared.dto.RunnerEnvironmentTree;
import org.eclipse.che.api.project.shared.dto.TreeElement;
import org.eclipse.che.ide.MimeType;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.AsyncRequestLoader;
import org.eclipse.che.ide.rest.RestContext;

import java.util.List;
import java.util.Map;

import static com.google.gwt.http.client.RequestBuilder.DELETE;
import static com.google.gwt.http.client.RequestBuilder.PUT;
import static org.eclipse.che.ide.rest.HTTPHeader.ACCEPT;
import static org.eclipse.che.ide.rest.HTTPHeader.CONTENT_TYPE;

/**
 * Implementation of {@link ProjectServiceClient}.
 *
 * @author Vitaly Parfonov
 * @author Artem Zatsarynnyy
 */
public class ProjectServiceClientImpl implements ProjectServiceClient {
    private final String              PROJECT;
    private final String              PROJECTS_IN_SPECIFIC_WORKSPACE;
    private final String              MODULES;
    private final String              ITEM;
    private final String              FILE;
    private final String              FOLDER;
    private final String              COPY;
    private final String              CLONE;
    private final String              MOVE;
    private final String              RENAME;
    private final String              IMPORT_PROJECT;
    private final String              GET_CHILDREN;
    private final String              GET_TREE;
    private final String              SEARCH;
    private final String              SWITCH_VISIBILITY;
    private final String              ENVIRONMENTS;
    private final String              ESTIMATE;
    private final AsyncRequestLoader  loader;
    private final AsyncRequestFactory asyncRequestFactory;

    @Inject
    protected ProjectServiceClientImpl(@RestContext String restContext,
                                       @Named("workspaceId") String workspaceId,
                                       AsyncRequestLoader loader,
                                       AsyncRequestFactory asyncRequestFactory) {
        this.loader = loader;
        this.asyncRequestFactory = asyncRequestFactory;
        PROJECT = restContext + "/project/" + workspaceId;
        PROJECTS_IN_SPECIFIC_WORKSPACE = restContext + "/project";
        MODULES = restContext + "/project/" + workspaceId + "/modules";
        ITEM = restContext + "/project/" + workspaceId + "/item";
        FILE = restContext + "/project/" + workspaceId + "/file";
        FOLDER = restContext + "/project/" + workspaceId + "/folder";
        COPY = restContext + "/project/" + workspaceId + "/copy";
        CLONE = restContext + "/vfs/" + workspaceId + "/v2/clone";
        MOVE = restContext + "/project/" + workspaceId + "/move";
        RENAME = restContext + "/project/" + workspaceId + "/rename";
        IMPORT_PROJECT = restContext + "/project/" + workspaceId + "/import";
        GET_CHILDREN = restContext + "/project/" + workspaceId + "/children";
        GET_TREE = restContext + "/project/" + workspaceId + "/tree";
        SEARCH = restContext + "/project/" + workspaceId + "/search";
        SWITCH_VISIBILITY = restContext + "/project/" + workspaceId + "/switch_visibility";
        ENVIRONMENTS = restContext + "/project/" + workspaceId + "/runner_environments";
        ESTIMATE = restContext + "/project/" + workspaceId + "/estimate";
    }

    @Override
    public void getProjects(AsyncRequestCallback<Array<ProjectReference>> callback) {
        final String requestUrl = PROJECT;
        asyncRequestFactory.createGetRequest(requestUrl)
                           .header(ACCEPT, MimeType.APPLICATION_JSON)
                           .loader(loader, "Getting projects...")
                           .send(callback);
    }

    @Override
    public void getProjectsInSpecificWorkspace(String wsId, AsyncRequestCallback<Array<ProjectReference>> callback) {
        final String requestUrl = PROJECTS_IN_SPECIFIC_WORKSPACE + "/" + wsId;
        asyncRequestFactory.createGetRequest(requestUrl)
                           .header(ACCEPT, MimeType.APPLICATION_JSON)
                           .loader(loader, "Getting projects...")
                           .send(callback);
    }

    @Override
    public void cloneProjectToCurrentWorkspace(String srcWorkspaceId, String srcProjectPath, String newNameForProject,
                                               AsyncRequestCallback<String> callback) {
        final String requestUrl = CLONE + "?srcVfsId=" + srcWorkspaceId +
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
        final String requestUrl = PROJECT + normalizePath(path);
        asyncRequestFactory.createGetRequest(requestUrl)
                           .header(ACCEPT, MimeType.APPLICATION_JSON)
                           .loader(loader, "Getting project...")
                           .send(callback);
    }

    @Override
    public void getItem(String path, AsyncRequestCallback<ItemReference> callback) {
        final String requestUrl = ITEM + normalizePath(path);
        asyncRequestFactory.createGetRequest(requestUrl)
                           .header(ACCEPT, MimeType.APPLICATION_JSON)
                           .loader(loader, "Getting item...")
                           .send(callback);
    }

    @Override
    public void createProject(String name, NewProject newProject, AsyncRequestCallback<ProjectDescriptor> callback) {
        final String requestUrl = PROJECT + "?name=" + name;
        asyncRequestFactory.createPostRequest(requestUrl, newProject)
                           .header(ACCEPT, MimeType.APPLICATION_JSON)
                           .loader(loader, "Creating project...")
                           .send(callback);
    }

    @Override
    public void estimateProject(String path, String projectType, AsyncRequestCallback<Map<String, List<String>>> callback) {
        final String requestUrl = ESTIMATE + normalizePath(path) + "?type=" + projectType;
        asyncRequestFactory.createGetRequest(requestUrl)
                           .header(ACCEPT, MimeType.APPLICATION_JSON)
                           .loader(loader, "Estimating project...")
                           .send(callback);
    }

    @Override
    public void getModules(String path, AsyncRequestCallback<Array<ProjectDescriptor>> callback) {
        final String requestUrl = MODULES + normalizePath(path);
        asyncRequestFactory.createGetRequest(requestUrl)
                           .header(ACCEPT, MimeType.APPLICATION_JSON)
                           .send(callback);
    }

    @Override
    public void createModule(String parentProjectPath, String name, NewProject newProject,
                             AsyncRequestCallback<ProjectDescriptor> callback) {
        final String requestUrl = PROJECT + normalizePath(parentProjectPath) + "?path=" + name;
        asyncRequestFactory.createPostRequest(requestUrl, newProject)
                           .header(ACCEPT, MimeType.APPLICATION_JSON)
                           .loader(loader, "Creating module...")
                           .send(callback);
    }

    @Override
    public void updateProject(String path, ProjectDescriptor descriptor, AsyncRequestCallback<ProjectDescriptor> callback) {
        final String requestUrl = PROJECT + normalizePath(path);
        asyncRequestFactory.createRequest(PUT, requestUrl, descriptor, false)
                           .header(CONTENT_TYPE, MimeType.APPLICATION_JSON)
                           .header(ACCEPT, MimeType.APPLICATION_JSON)
                           .loader(loader, "Updating project...")
                           .send(callback);
    }

    @Override
    public void updateProject(String path, ProjectUpdate descriptor, AsyncRequestCallback<ProjectDescriptor> callback) {
        final String requestUrl = PROJECT + normalizePath(path);
        asyncRequestFactory.createRequest(PUT, requestUrl, descriptor, false)
                           .header(CONTENT_TYPE, MimeType.APPLICATION_JSON)
                           .header(ACCEPT, MimeType.APPLICATION_JSON)
                           .loader(loader, "Updating project...")
                           .send(callback);
    }

    @Override
    public void createFile(String parentPath, String name, String content, String contentType,
                           AsyncRequestCallback<ItemReference> callback) {
        final String requestUrl = FILE + normalizePath(parentPath) + "?name=" + name;
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
        final String requestUrl = FILE + normalizePath(path);
        asyncRequestFactory.createGetRequest(requestUrl)
                           .loader(loader, "Loading file content...")
                           .send(callback);
    }

    @Override
    public void updateFile(String path, String content, String contentType, AsyncRequestCallback<Void> callback) {
        final String requestUrl = FILE + normalizePath(path);
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
//                           .loader(loader, "Updating file content...")
                           .send(callback);
    }

    @Override
    public void createFolder(String path, AsyncRequestCallback<ItemReference> callback) {
        final String requestUrl = FOLDER + normalizePath(path);
        asyncRequestFactory.createPostRequest(requestUrl, null)
                           .loader(loader, "Creating folder...")
                           .send(callback);
    }

    @Override
    public void delete(String path, AsyncRequestCallback<Void> callback) {
        final String requestUrl = PROJECT + normalizePath(path);
        asyncRequestFactory.createRequest(DELETE, requestUrl, null, false)
                           .loader(loader, "Deleting project...")
                           .send(callback);
    }

    @Override
    public void deleteModule(String path, String modulePath, AsyncRequestCallback<Void> callback) {
        final String requestUrl = PROJECT + normalizePath(path) + "?module=" + modulePath;
        asyncRequestFactory.createRequest(DELETE, requestUrl, null, false)
                           .loader(loader, "Deleting module...")
                           .send(callback);
    }

    @Override
    public void copy(String path, String newParentPath, AsyncRequestCallback<Void> callback) {
        final String requestUrl = COPY + normalizePath(path) + "?to=" + newParentPath;
        asyncRequestFactory.createPostRequest(requestUrl, null)
                           .loader(loader, "Copying item...")
                           .send(callback);
    }

    @Override
    public void move(String path, String newParentPath, AsyncRequestCallback<Void> callback) {
        final String requestUrl = MOVE + normalizePath(path) + "?to=" + newParentPath;
        asyncRequestFactory.createPostRequest(requestUrl, null)
                           .loader(loader, "Moving item...")
                           .send(callback);
    }

    @Override
    public void rename(String path, String newName, String newMediaType, AsyncRequestCallback<Void> callback) {
        String requestUrl = RENAME + normalizePath(path) + "?name=" + newName;
        if (newMediaType != null) {
            requestUrl += "&mediaType=" + newMediaType;
        }
        asyncRequestFactory.createPostRequest(requestUrl, null)
                           .loader(loader, "Renaming item...")
                           .send(callback);
    }

    @Override
    public void importProject(String path, boolean force, ImportProject importProject, AsyncRequestCallback<ImportResponse> callback) {
        final StringBuilder requestUrl = new StringBuilder(IMPORT_PROJECT);
        requestUrl.append(normalizePath(path));
        if (force) {
            requestUrl.append("?force=true");
        }
        asyncRequestFactory.createPostRequest(requestUrl.toString(), importProject, true)
                           .header(ACCEPT, MimeType.APPLICATION_JSON)
                           .loader(loader, "Importing sources into project...")
                           .send(callback);
    }

    @Override
    public void getChildren(String path, AsyncRequestCallback<Array<ItemReference>> callback) {
        final String requestUrl = GET_CHILDREN + normalizePath(path);
        asyncRequestFactory.createGetRequest(requestUrl)
                           .header(ACCEPT, MimeType.APPLICATION_JSON)
                           .send(callback);
    }

    @Override
    public void getTree(String path, int depth, AsyncRequestCallback<TreeElement> callback) {
        final String requestUrl = GET_TREE + normalizePath(path) + "?depth=" + depth;
        asyncRequestFactory.createGetRequest(requestUrl)
                           .header(ACCEPT, MimeType.APPLICATION_JSON)
                           .send(callback);
    }

    @Override
    public void search(QueryExpression expression, AsyncRequestCallback<Array<ItemReference>> callback) {
        final String requestUrl = SEARCH + normalizePath(expression.getPath());

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

    /** {@inheritDoc} */
    @Override
    public void switchVisibility(String path, String visibility, AsyncRequestCallback<Void> callback) {
        final String requestUrl = SWITCH_VISIBILITY + normalizePath(path) + "?visibility=" + visibility;
        asyncRequestFactory.createPostRequest(requestUrl, null)
                           .loader(loader, "Switching visibility...")
                           .send(callback);
    }

    /** {@inheritDoc} */
    /*@Override
    public void getRunnerEnvironments(String path, AsyncRequestCallback<RunnerEnvironmentTree> callback) {
        final String requestUrl = ENVIRONMENTS + normalizePath(path);
        asyncRequestFactory.createGetRequest(requestUrl)
                           .header(ACCEPT, MimeType.APPLICATION_JSON)
                           .loader(loader, "Getting project-scoped runner environments...")
                           .send(callback);
    }*/

    /**
     * Normalizes the path by adding a leading '/' if it doesn't exist.
     * Also escapes some special characters.
     *
     * See following javascript functions for details:
     *     escape() will not encode: @ * / +
     *     encodeURI() will not encode: ~ ! @ # $ & * ( ) = : / , ; ? + '
     *     encodeURIComponent() will not encode: ~ ! * ( ) '
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
