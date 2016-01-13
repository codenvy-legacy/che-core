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
package org.eclipse.che.api.core.model.project;

import java.util.Map;

/**
 * Descriptor of  project sources' remote storage
 * For instance Git repository location
 *
 * @author gazarenkov
 */
public interface SourceStorage {

    /**
     * @return Type of storage, for example "git"
     */
    String getType();

    /**
     * @return Location, for example git repository URL
     */
    String getLocation();

    /**
     * @return optional parameters
     */
    Map<String, String> getParameters();
}
