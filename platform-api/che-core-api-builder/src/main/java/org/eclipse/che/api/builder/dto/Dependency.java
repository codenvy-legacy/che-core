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
package org.eclipse.che.api.builder.dto;

import org.eclipse.che.dto.shared.DTO;

/**
 * Describes project dependency.
 *
 * @author <a href="mailto:aparfonov@codenvy.com">Andrey Parfonov</a>
 */
@DTO
public interface Dependency {
    /**
     * Full name of project dependency. Typically name should provide information about name of library including a version number.
     * Different build system may sub-classes of this class to provide more details about dependency.
     *
     * @return name of project dependency
     */
    String getFullName();

    Dependency withFullName(String name);

    /**
     * Set name of project dependency.
     *
     * @param name
     *         name of project dependency
     * @see #getFullName()
     */
    void setFullName(String name);
}
