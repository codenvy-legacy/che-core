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

/**
 * For display RunnerEnvironment in RunnerEnvironmentTree.
 *
 * @author andrew00x
 * @deprecated
 */
@DTO
public interface RunnerEnvironmentLeaf {
    RunnerEnvironment getEnvironment();

    void setEnvironment(RunnerEnvironment environment);

    RunnerEnvironmentLeaf withEnvironment(RunnerEnvironment environment);

    /** Display name of RunnerEnvironment. */
    String getDisplayName();

    void setDisplayName(String id);

    RunnerEnvironmentLeaf withDisplayName(String name);
}
