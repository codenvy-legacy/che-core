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
package org.eclipse.che.ide.bootstrap;

import com.google.gwt.core.client.Callback;
import com.google.gwt.user.client.Window;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;


import org.eclipse.che.api.factory.gwt.client.FactoryServiceClient;
import org.eclipse.che.api.factory.shared.dto.Factory;
import org.eclipse.che.ide.core.Component;
import org.eclipse.che.ide.workspace.BrowserQueryFieldRenderer;

/**
 * Retrieves specified factory, and creates and/or starts workspace configured in it.
 *
 * @author Max Shaposhnik
 */
@Singleton
public class FactoryWorkspaceComponent implements Component {
    private static final String FACTORY_ID_ATTRIBUTE = "factoryId";

    private final FactoryServiceClient      factoryServiceClient;
    private final BrowserQueryFieldRenderer browserQueryFieldRenderer;
    private       Factory                   factory;


    @Inject
    public FactoryWorkspaceComponent(FactoryServiceClient factoryServiceClient,
                                     BrowserQueryFieldRenderer browserQueryFieldRenderer) {
        this.browserQueryFieldRenderer = browserQueryFieldRenderer;
        this.factoryServiceClient = factoryServiceClient;
    }

    @Override
    public void start(Callback<Component, Exception> callback) {
        String factoryParams = browserQueryFieldRenderer.getParameterFromURLByName("factory");
        Window.open("/dashboard/#/load-factory/" + factoryParams, "_self", "");
    }
}

