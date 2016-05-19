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
package org.eclipse.che.api.runner.internal;

import org.eclipse.che.inject.DynaModule;
import com.google.inject.AbstractModule;

/**
 * @author andrew00x
 */
@DynaModule
public class SetupDedicatedRunnerServerModule extends AbstractModule {
    @Override
    protected void configure() {
        requestStaticInjection(SlaveRunnerService.class);
    }
}
