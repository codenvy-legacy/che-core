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

import org.eclipse.che.api.vfs.shared.dto.Item;
import org.eclipse.che.api.vfs.shared.dto.ReplacementSet;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.AsyncRequestLoader;
import org.eclipse.che.ide.rest.RestContext;

import com.google.gwt.http.client.RequestBuilder;
import com.google.inject.name.Named;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import static org.eclipse.che.ide.MimeType.APPLICATION_JSON;
import static org.eclipse.che.ide.rest.HTTPHeader.ACCEPT;
import static org.eclipse.che.ide.rest.HTTPHeader.CONTENT_TYPE;

/**
 * Implementation for {@link VfsServiceClient}.
 *
 * @author Sergii Leschenko
 * @author Artem Zatsarynnyy
 */
public class VfsServiceClientImpl implements VfsServiceClient {
    private final String VFS;
    private final String FIND_REPLACE;
    private final String GET_ITEM_BY_PATH;

    private final AsyncRequestLoader  loader;
    private final AsyncRequestFactory asyncRequestFactory;

    @Inject
    public VfsServiceClientImpl(@RestContext String restContext,
                                @Named("workspaceId") String workspaceId,
                                AsyncRequestLoader loader,
                                AsyncRequestFactory asyncRequestFactory) {
        this.loader = loader;
        this.asyncRequestFactory = asyncRequestFactory;

        VFS = restContext + "/vfs/" + workspaceId + "/v2";
        FIND_REPLACE = VFS + "/replace";
        GET_ITEM_BY_PATH = VFS + "/itembypath";
    }

    @Override
    public void replaceInCurrentWorkspace(@Nonnull String projectPath,
                                          Array<ReplacementSet> replacementSets,
                                          AsyncRequestCallback<Void> callback) {
        String path = FIND_REPLACE + normalizePath(projectPath);

        asyncRequestFactory.createRequest(RequestBuilder.POST, path, replacementSets, false)
                           .header(CONTENT_TYPE, APPLICATION_JSON)
                           .loader(loader)
                           .send(callback);
    }

    @Override
    public void getItemByPath(@Nonnull String path, AsyncRequestCallback<Item> callback) {
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
