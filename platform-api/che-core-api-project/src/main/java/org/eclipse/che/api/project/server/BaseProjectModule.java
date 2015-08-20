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
package org.eclipse.che.api.project.server;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

import org.eclipse.che.api.project.server.handlers.ProjectHandler;
import org.eclipse.che.api.project.server.watcher.WatcherService;

/**
 * Deploys project API components.
 *
 * @author andrew00x
 */
public class BaseProjectModule extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder.newSetBinder(binder(), ProjectImporter.class).addBinding().to(ZipProjectImporter.class);
        Multibinder.newSetBinder(binder(), ValueProviderFactory.class); /* empty binding */
        Multibinder.newSetBinder(binder(), ProjectHandler.class); /* empty binding */
        bind(ProjectService.class);
        bind(ProjectTypeService.class);
        bind(ProjectTemplateService.class);
        bind(ProjectImportersService.class);
        bind(ProjectTemplateDescriptionLoader.class);
        bind(ProjectTemplateRegistry.class);
        bind(WatcherService.class);
    }
}
