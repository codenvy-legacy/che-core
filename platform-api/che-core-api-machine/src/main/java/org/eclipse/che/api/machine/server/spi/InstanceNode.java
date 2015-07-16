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
package org.eclipse.che.api.machine.server.spi;

/**
 * Represents server where machine is launched
 *
 * @author Alexander Garagatyi
 */
public interface InstanceNode {
    /**
     * Get path of folder on machine node with workspace fs
     */
    String getProjectsFolder();

    /**
     * Host of the server where machine is launched
     */
    String getHost();
}
