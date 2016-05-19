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
package org.eclipse.che.commons.schedule.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * Invoke given method of given object.
 *
 * @author Sergii Kabashniuk
 */
public class LoggedRunnable implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(LoggedRunnable.class);

    private final Object object;
    private final Method method;

    public LoggedRunnable(Object object, Method method) {
        this.object = object;
        this.method = method;
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        try {
            if (object instanceof Runnable && method.getName().equals("run") && method.getParameterTypes().length == 0) {
                LOG.debug("Invoking method run of class {} instance {}", object.getClass().getName(), object);

                ((Runnable)object).run();

                LOG.debug("Method of class {} instance {} complete  at {}  sec",
                          object.getClass().getName(),
                          object,
                          TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime));
            } else {
                try {
                    LOG.debug("Invoking run method of class {} instance {}", object.getClass().getName(), object);

                    method.invoke(object);

                    LOG.debug("Method of class {} instance {} complete  at {}  sec",
                              object.getClass().getName(),
                              object,
                              TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime));
                } catch (InvocationTargetException | IllegalAccessException e) {
                    LOG.error(e.getLocalizedMessage());
                }
            }
        } catch (Exception e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw e;
        }

    }
}
