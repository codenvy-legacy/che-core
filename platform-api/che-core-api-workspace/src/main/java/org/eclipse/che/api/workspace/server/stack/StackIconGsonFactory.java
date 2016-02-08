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
package org.eclipse.che.api.workspace.server.stack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;

/**
 * Gson factory for local {@link org.eclipse.che.api.workspace.server.stack.image.StackIcon} storage.
 * This class generate {@link Gson} for serialization and deserialization StackIcon
 * @see org.eclipse.che.api.workspace.server.dao.StackIconDao - local stack icon storage
 *
 * @author Alexander Andrienko
 */
public class StackIconGsonFactory {

    private final Gson gson;

    @Inject
    public StackIconGsonFactory() {
        gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
                                .setPrettyPrinting()
                                .create();
    }

    /**
     * Returns {@link Gson} for serialization and deserialization {@link org.eclipse.che.api.workspace.server.stack.image.StackIcon}
     */
    public Gson getGson() {
        return gson;
    }
}
