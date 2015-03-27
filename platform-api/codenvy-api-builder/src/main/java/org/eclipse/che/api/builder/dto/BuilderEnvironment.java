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
package org.eclipse.che.api.builder.dto;

import org.eclipse.che.dto.shared.DTO;

import java.util.Map;

/**
 * @author andrew00x
 */
@DTO
public interface BuilderEnvironment {
    /**
     * Get unique id of BuilderEnvironment.
     *
     * @return unique id of BuilderEnvironment
     */
    String getId();

    void setId(String id);

    BuilderEnvironment withId(String name);

    /** Display name of BuilderEnvironment. */
    String getDisplayName();

    void setDisplayName(String id);

    BuilderEnvironment withDisplayName(String name);

    /**
     * Get description of BuilderEnvironment.
     *
     * @return description of BuilderEnvironment
     */
    String getDescription();

    void setDescription(String description);

    BuilderEnvironment withDescription(String description);

    boolean getIsDefault();

    void setIsDefault(boolean isDefault);

    BuilderEnvironment withIsDefault(boolean isDefault);

    /** Properties of this build environment, e.g. version of build system, some environment variables, etc. */
    Map<String, String> getProperties();

    void setProperties(Map<String, String> properties);

    BuilderEnvironment withProperties(Map<String, String> properties);
}
