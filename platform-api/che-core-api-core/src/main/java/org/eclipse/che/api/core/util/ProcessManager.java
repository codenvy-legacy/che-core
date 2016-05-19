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
package org.eclipse.che.api.core.util;

/** @author andrew00x */
abstract class ProcessManager {
    static ProcessManager newInstance() {
        if (SystemInfo.isUnix()) {
            return new UnixProcessManager();
        }
        return new DefaultProcessManager();
    }

    abstract void kill(Process process);

    abstract boolean isAlive(Process process);

    abstract int system(String command);
}
