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
package org.eclipse.che.api.factory.gwt.client;

import org.eclipse.che.api.factory.dto.Factory;
import org.eclipse.che.ide.rest.AsyncRequestCallback;

import javax.annotation.Nonnull;

/**
 * Client for IDE3 Factory service.
 *
 * @author Vladyslav Zhukovskii
 */
public interface FactoryServiceClient {
    /**
     * Get valid JSON factory object based on input factory ID or raw query string which represent non encoded factory URL.
     *
     * @param raw
     *         factory ID or query string which represents factory non encoded version
     * @param callback
     *         callback which return valid JSON object of factory or exception if occurred
     *
     */
    void getFactory(@Nonnull String raw, @Nonnull AsyncRequestCallback<Factory> callback);
    
    /**
     * @param factoryId Factory's id
     * @param type snippent's type (markdown, html, etc)
     * @param callback callback which returns snippet of the factory or exception if occurred
     */
    void getFactorySnippet(@Nonnull String factoryId, @Nonnull String type, @Nonnull AsyncRequestCallback<String> callback);


    /**
     * Retrieves factory object prototype for given project with it's attributes. It's not the stored factory object.
     * @param workspaceId workspace id
     * @param path project path
     * @param callback callback which returns snippet of the factory or exception if occurred
     */
    void getFactoryJson(@Nonnull String workspaceId, @Nonnull String path, @Nonnull AsyncRequestCallback<Factory> callback);
}
