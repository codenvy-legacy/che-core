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
package org.eclipse.che.api.git.shared;

import org.eclipse.che.dto.shared.DTO;

/**
 * Request to checkout a branch to the working tree.
 *
 * @author andrew00x
 */
@DTO
public interface BranchCheckoutRequest extends GitRequest {
    /** @return name of branch to checkout */
    String getName();
    
    void setName(String name);
    
    BranchCheckoutRequest withName(String name);

    /** @return name of a commit at which to start the new branch. If <code>null</code> the HEAD will be used */
    String getStartPoint();
    
    void setStartPoint(String startPoint);
    
    BranchCheckoutRequest withStartPoint(String startPoint);

    /**
     * @return if <code>true</code> then create a new branch named {@link #name} and start it at {@link #startPoint} or to the HEAD if
     *         {@link #startPoint} is not set. If <code>false</code> and there is no branch with name {@link #name} corresponding exception
     *         will be thrown
     */
    boolean isCreateNew();
    
    void setCreateNew(boolean isCreateNew);
    
    BranchCheckoutRequest withCreateNew(boolean isCreateNew);

    /** @return name of branch that will be tracked */
    String getTrackBranch();

    void setTrackBranch(String trackBranch);

    BranchCheckoutRequest withTrackBranch(String trackBranch);
}
