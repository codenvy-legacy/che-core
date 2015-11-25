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
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.core.Component;
import org.eclipse.che.ide.util.Config;
import org.eclipse.che.ide.util.loging.Log;


/**
 * Checks startup params and calls appropriate factory or default workspace component.
 *
 * @author Max Shaposhnik
 */
public class StartupComponent implements Component {

    private final Provider<DefaultWorkspaceComponent> workspaceComponentProvider;
    private final Provider<FactoryWorkspaceComponent> factoryComponentProvider;
    private final AppContext                          appContext;


    @Inject
    public StartupComponent(Provider<DefaultWorkspaceComponent> workspaceComponentProvider,
                            Provider<FactoryWorkspaceComponent> factoryComponentProvider,
                            AppContext appContext) {
        this.workspaceComponentProvider = workspaceComponentProvider;
        this.factoryComponentProvider = factoryComponentProvider;
        this.appContext = appContext;
    }

    @Override
    public void start(final Callback<Component, Exception> callback) {
        String factoryParams = Config.getStartupParam("factory");
        if (factoryParams != null) {
            Log.info(StartupComponent.class, "Starting factory workspace component");
            factoryComponentProvider.get().start(callback);
        } else {
            Log.info(StartupComponent.class, "Starting default workspace component");
            workspaceComponentProvider.get().start(callback);
        }
    }
}

