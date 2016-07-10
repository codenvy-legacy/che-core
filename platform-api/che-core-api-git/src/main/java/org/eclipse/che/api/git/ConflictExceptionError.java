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
package org.eclipse.che.api.git;

import org.eclipse.che.api.core.rest.shared.dto.ServiceError;
import org.eclipse.che.dto.shared.DTO;

import java.util.List;

/**
 * Created by I053322 on 7/10/2016.
 */
@DTO
public interface ConflictExceptionError  {
    /**
     * Get conflict path.
     *
     * @return conflict Paths
     */
    List<String> getConflictingPaths();

    ConflictExceptionError withConflictingPaths(List<String> conflictPath);

    /**
     * Set conflict path.
     *
     * @param conflictPath
     *         error message
     */
    void setConflictingPaths(List<String> conflictPath);

    /**
     * Get error message.
     *
     * @return error message
     */
    String getMessage();

    ConflictExceptionError withMessage(String message);

    /**
     * Set error message.
     *
     * @param message
     *         error message
     */
    void setMessage(String message);


}
