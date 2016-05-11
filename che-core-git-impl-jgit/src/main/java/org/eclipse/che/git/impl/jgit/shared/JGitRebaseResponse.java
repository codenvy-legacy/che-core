/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - API 
 *   SAP           - initial implementation
 *******************************************************************************/
package org.eclipse.che.git.impl.jgit.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.che.api.git.shared.RebaseResponse;
import org.eclipse.jgit.api.RebaseResult;

public class JGitRebaseResponse implements RebaseResponse {

    private RebaseStatus status;
    private List<String> failed;
    private List<String> conflicts;

    public JGitRebaseResponse(RebaseResult result) {
        switch (result.getStatus()) {
        case ABORTED:
            status = RebaseStatus.ABORTED;
            break;
        case CONFLICTS:
            status = RebaseStatus.CONFLICTING;
            break;
        case UP_TO_DATE:
            status = RebaseStatus.ALREADY_UP_TO_DATE;
            break;
        case FAST_FORWARD:
            // return RebaseStatus.FAST_FORWARD;
        case NOTHING_TO_COMMIT:
        case OK:
            status = RebaseStatus.OK;
            break;
        case STOPPED:
            status = RebaseStatus.STOPPED;
            break;
        case UNCOMMITTED_CHANGES:
            status = RebaseStatus.UNCOMMITTED_CHANGES;
            break;
        default:
            status = RebaseStatus.FAILED;
        }

        conflicts = result.getConflicts() != null ? result.getConflicts() : Collections.emptyList();
        failed = result.getFailingPaths() != null ? new ArrayList<>(result.getFailingPaths().keySet()) : Collections.emptyList();
    }

    @Override
    public RebaseStatus getStatus() {
        return status;
    }

    @Override
    public List<String> getConflicts() {
        return conflicts;
    }

    @Override
    public List<String> getFailed() {
        return failed;
    }
}
