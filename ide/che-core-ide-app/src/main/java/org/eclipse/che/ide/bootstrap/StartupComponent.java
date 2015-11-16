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
import org.eclipse.che.ide.util.loging.Log;

import javax.inject.Inject;

/**
 * @author Max Shaposhnik
 */
public class StartupComponent implements Component {

    private final DefaultWorkspaceComponent workspaceComponent;
    private final FactoryWorkspaceComponent factoryComponent;
    private final AppContext                appContext;


    @Inject
    public StartupComponent(DefaultWorkspaceComponent workspaceComponent, FactoryWorkspaceComponent factoryComponent,
                            AppContext appContext) {
        this.workspaceComponent = workspaceComponent;
        this.factoryComponent = factoryComponent;
        this.appContext = appContext;
    }

    @Override
    public void start(final Callback<Component, Exception> callback) {
        Log.info(StartupComponent.class, "Into startup.start();");
        String factoryParams = Config.getStartupParam("factory");
        if (factoryParams != null) {
            Log.info(StartupComponent.class, "Starting factory.");
            factoryComponent.start(callback);
        } else {
            Log.info(StartupComponent.class, "Starting default.");
            workspaceComponent.start(callback);
        }
    }
}

