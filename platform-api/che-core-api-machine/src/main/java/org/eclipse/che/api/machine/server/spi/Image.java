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

import org.eclipse.che.api.machine.server.MachineException;

/**
 * Provides instances of {@link org.eclipse.che.api.machine.server.spi.Instance} in implementation specific way.
 *
 * @author andrew00x
 */
public interface Image {
    ImageMetadata getMetadata() throws MachineException;

    Instance createInstance() throws MachineException;
}
