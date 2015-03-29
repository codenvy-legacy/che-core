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
package org.eclipse.che.api.project.shared.dto;

import org.eclipse.che.dto.shared.DTO;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * @author andrew00x
 */
@DTO
public interface RunnerEnvironment {
    /**
     * Gets unique identifier of runner environment.
     */
    @Nonnull
    String getId();

    /** Sets unique identifier of runner environment. */
    void setId(@Nonnull String id);

    RunnerEnvironment withId(@Nonnull String id);

    /**
     * Gets runtime options of this runner environment. If {@code Map} contains mapping to empty string for some option it means that
     * environment doesn't provide any default value for this option.
     */
    @Nonnull
    Map<String, String> getOptions();

    /**
     * Sets runtime options of this runner environment.
     *
     * @see #getOptions()
     */
    void setOptions(Map<String, String> options);

    RunnerEnvironment withOptions(Map<String, String> options);

    /** Gets environment variables (runner type and(or) receipt specific). */
    /**
     * Gets environment variables of this runner environment. If {@code Map} contains mapping to empty string for some variable it means
     * that environment doesn't provide any default value for this variable.
     */
    @Nonnull
    Map<String, String> getVariables();

    /**
     * Sets environment variables of this runner environment.
     *
     * @see #getVariables()
     */
    void setVariables(Map<String, String> variables);

    RunnerEnvironment withVariables(Map<String, String> variables);

    /* =================================================================================================== */
    /* Following methods suitable for codenvy environment but looks useless for user defined environments. */
    /* Methods are defined here to be able merge user defined environments and codenvy environments in one */
    /* place on client side.                                                                               */
    /* =================================================================================================== */

    @Nullable
    String getDescription();

    void setDescription(@Nullable String description);

    @Nullable
    RunnerEnvironment withDescription(String description);

    @Nullable
    String getDisplayName();

    void setDisplayName(@Nullable String displayName);

    @Nonnull
    RunnerEnvironment withDisplayName(@Nullable String displayName);

}
