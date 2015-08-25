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
package org.eclipse.che.api.core.model.machine;

/**
 * Recipe to create new {@link org.eclipse.che.api.machine.server.spi.Instance}.
 *
 * @author Eugene Voevodin
 */
public interface Recipe {

    /**
     * Returns recipe type (i.e. 'Dockerfile')
     */
    String getType();

    /**
     * Returns recipe script, which is used to instantiate new {@link org.eclipse.che.api.machine.server.spi.Instance}
     */
    String getScript();


}