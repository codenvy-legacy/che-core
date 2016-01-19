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
package org.eclipse.che.api.builder.internal;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

/** @author andrew00x */
public class BuilderModule extends AbstractModule {
    @Override
    protected void configure() {
        // Initialize empty set of Builders.
        Multibinder.newSetBinder(binder(), Builder.class);
        bind(BuilderRegistryPlugin.class).asEagerSingleton();
        bindConstant().annotatedWith(Names.named("org.everrest.security")).to("false");
    }
}
