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

import org.eclipse.che.dto.shared.DTO;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by I053322 on 7/7/2016.
 */
@DTO
public class GitConflictException extends GitException {
    private List<String> conflictingPaths = new LinkedList<String>();

    public GitConflictException(String message) {
        super(message);
    }

    public GitConflictException(String message, List<String> conflictingPaths) {
        super(message);
        this.conflictingPaths = conflictingPaths;
    }

    public GitConflictException(Throwable cause) {
        super(cause);
    }

    public GitConflictException(String message, Throwable cause) {
        super(message, cause);
    }

    public GitConflictException(String message, List<String> conflictingPaths, Throwable cause) {
        super(message, cause);
        this.conflictingPaths = conflictingPaths;
    }

    public List<String> getConflictPaths(){
        return this.conflictingPaths;
    }

}
