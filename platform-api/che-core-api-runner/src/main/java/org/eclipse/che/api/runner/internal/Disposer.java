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
package org.eclipse.che.api.runner.internal;

/**
 * Implementation of this interface is used for registering in {@link Runner} and disposing the data
 * associated with application process. Disposers associated with {@link ApplicationProcess} and method
 * {@link #dispose()} called before remove {@link ApplicationProcess} from internal Runner's registry.
 * NOTE: Method {@link #dispose()} isn't called just after stopping of application. Runners may call method {@link #dispose()} at any time
 * after stopping of application.
 *
 * @author andrew00x
 * @see Runner#registerDisposer(ApplicationProcess, Disposer)
 */
public interface Disposer {
    /**
     * Disposes the data associated with {@link ApplicationProcess}.
     *
     * @see Runner#registerDisposer(ApplicationProcess, Disposer)
     */
    void dispose();
}
