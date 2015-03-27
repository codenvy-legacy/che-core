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
package org.eclipse.che.inject.lifecycle;

import org.eclipse.che.commons.schedule.Launcher;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

/**
 * Launch  method marked with @ScheduleCron @ScheduleDelay and @ScheduleRate annotations using  Launcher
 *
 * @author Sergii Kabashniuk
 */
public class ScheduleModule extends LifecycleModule {

    @Override
    protected void configure() {
        final Provider<Launcher> launcher = getProvider(Launcher.class);
        final Provider<Injector> injector = getProvider(Injector.class);
        bindListener(Matchers.any(), new TypeListener() {
            @Override
            public <T> void hear(final TypeLiteral<T> type, final TypeEncounter<T> encounter) {
                encounter.register(new ScheduleInjectionListener<T>(launcher, injector));
            }
        });
    }


}
