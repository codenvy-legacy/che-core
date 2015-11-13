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

package org.eclipse.che.ide.bootstrap;


import com.google.gwt.core.client.Callback;

import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.core.Component;
import org.eclipse.che.ide.util.Config;

import javax.inject.Inject;

/**
 * @author Max Shaposhnik
 */
public class StartupComponent implements Component {

    private final WorkspaceComponent workspaceComponent;
    private final FactoryComponent   factoryComponent;
    private final AppContext         appContext;


    @Inject
    public StartupComponent(WorkspaceComponent workspaceComponent, FactoryComponent factoryComponent, AppContext appContext) {
        this.workspaceComponent = workspaceComponent;
        this.factoryComponent = factoryComponent;
        this.appContext = appContext;
    }

    @Override
    public void start(final Callback<Component, Exception> callback) {
        String factoryParams = Config.getStartupParam("factory");
        if (factoryParams != null) {
            factoryComponent.start(callback);
        } else {
            workspaceComponent.start(callback);
        }
    }
}

