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

import java.util.Map;

/**
 * Describe implementation specific properties of machine instance
 *
 * @author andrew00x
 * @author Alexander Garagatyi
 */
public interface InstanceMetadata {

    // TODO add more generic info

    /**
     * Returns instance specific properties
     */
    Map<String, String> getProperties();

    /** Serializes this {@code InstanceMetadata} to JSON format. */
    String toJson();
}
