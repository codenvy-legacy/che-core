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
package org.eclipse.che.api.vfs.gwt.client;

import com.google.gwt.http.client.RequestBuilder;
import com.google.inject.name.Named;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.vfs.shared.dto.Item;
import org.eclipse.che.api.vfs.shared.dto.ReplacementSet;
import org.eclipse.che.api.workspace.gwt.client.event.StartWorkspaceEvent;
import org.eclipse.che.api.workspace.gwt.client.event.StartWorkspaceHandler;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.AsyncRequestLoader;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.List;

import static org.eclipse.che.ide.MimeType.APPLICATION_JSON;
import static org.eclipse.che.ide.rest.HTTPHeader.ACCEPT;
import static org.eclipse.che.ide.rest.HTTPHeader.CONTENT_TYPE;

/**
 * Implementation for {@link VfsServiceClient}.
 *
 * @author Sergii Leschenko
 * @author Artem Zatsarynnyi
 */
public class VfsServiceClientImpl implements VfsServiceClient, StartWorkspaceHandler {
    private final AsyncRequestLoader  loader;
    private final AsyncRequestFactory asyncRequestFactory;
    private final String              extPath;

    private String FIND_REPLACE;
    private String GET_ITEM_BY_PATH;

    @Inject
    public VfsServiceClientImpl(@Named("cheExtensionPath") String extPath,
                                EventBus eventBus,
                                AsyncRequestLoader loader,
                                AsyncRequestFactory asyncRequestFactory) {
        this.extPath = extPath;
        this.loader = loader;
        this.asyncRequestFactory = asyncRequestFactory;

        eventBus.addHandler(StartWorkspaceEvent.TYPE, this);
    }

    @Override
    public void onWorkspaceStarted(UsersWorkspaceDto workspace) {
        String VFS = extPath + "/vfs/" + workspace.getId() + "/v2";
        FIND_REPLACE = VFS + "/replace";
        GET_ITEM_BY_PATH = VFS + "/itembypath";
    }

    @Override
    public void replaceInCurrentWorkspace(@NotNull String projectPath,
                                          List<ReplacementSet> replacementSets,
                                          AsyncRequestCallback<Void> callback) {
        String path = FIND_REPLACE + normalizePath(projectPath);

        asyncRequestFactory.createRequest(RequestBuilder.POST, path, replacementSets, false)
                           .header(CONTENT_TYPE, APPLICATION_JSON)
                           .loader(loader)
                           .send(callback);
    }

    @Override
    public void getItemByPath(@NotNull String path, AsyncRequestCallback<Item> callback) {
        final String url = GET_ITEM_BY_PATH + normalizePath(path);

        asyncRequestFactory.createGetRequest(url)
                           .header(ACCEPT, APPLICATION_JSON)
                           .loader(loader)
                           .send(callback);
    }

    /**
     * Normalizes the path by adding a leading '/' if it doesn't exist.
     *
     * @param path
     *         path to normalize
     * @return normalized path
     */
    private String normalizePath(String path) {
        return path.startsWith("/") ? path : '/' + path;
    }
}
