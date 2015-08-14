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
package org.eclipse.che.api.local.storage;


import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;

/**
 * Factory for injection to LocalStorage stored file.
 *
 * @author Anton Korneta
 */
@Singleton
public class LocalStorageFactory {

    @Inject
    @Named("local.storage.path")
    private String pathToStorage;

    /**
     * @param fileName name of file in local storage.
     * @return instance of LocalStorage.
     * @throws IOException occurs when cannot create root storage directory.
     */
    public LocalStorage create(String fileName) throws IOException {
        return new LocalStorage(pathToStorage, fileName);
    }
}