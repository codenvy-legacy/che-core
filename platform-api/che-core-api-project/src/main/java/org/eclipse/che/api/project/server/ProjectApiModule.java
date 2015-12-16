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

import org.eclipse.che.api.core.model.project.type.ProjectType;
import org.eclipse.che.api.project.server.type.BaseProjectType;
import org.eclipse.che.api.vfs.server.SystemVirtualFilePathFilter;

/**
 * @author gazarenkov
 */
public class ProjectApiModule extends AbstractModule {

    @Override
    protected void configure() {
        Multibinder<ProjectType> projectTypesMultibinder = Multibinder.newSetBinder(binder(), ProjectType.class);
        projectTypesMultibinder.addBinding().to(BaseProjectType.class);
        Multibinder.newSetBinder(binder(), SystemVirtualFilePathFilter.class).addBinding().to(ProjectMiscPathFilter.class);
    }
}
