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
package org.eclipse.che.ide.ui.smartTree.state;

import com.google.gwt.storage.client.Storage;

/**
 * @author Vlad Zhukovskiy
 */
public class HtmlStorageProvider extends AbstractStateProvider {
    private final Storage storage;

    public HtmlStorageProvider() {
        this.storage = Storage.getLocalStorageIfSupported();
    }

    @Override
    public void clear(String name) {
        storage.setItem(name, "");
    }

    @Override
    public String getValue(String name) {
        return storage.getItem(name);
    }

    @Override
    public void setValue(String name, String value) {
        storage.setItem(name, value);
    }
}
