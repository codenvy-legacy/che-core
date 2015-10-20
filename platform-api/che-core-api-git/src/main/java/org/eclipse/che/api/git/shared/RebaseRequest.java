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
 * Request to add rebase branch
 *
 * @author Dror Cohen
 */
@DTO
public interface RebaseRequest {
    /** 
     *  sets the rebase rebase operation 
     *  can be on of these values:
     *  
     *  BEGIN(default) : begin a git rebase operation
     *  ABORT: abort an ongoing git rebase operation
     *  CONTINUE: continue with gir rebase operation after conflicts have been resolved
     *  SKIP: bypass the commit that causes the merge failure
     *  
     *  see https://git-scm.com/docs/git-rebase for more information
     *  
     * @param The rebase opreation to use
     */
    void setOperation(String operation);
    /** 
     * gets the rebase rebase operation
     * 
     *  @return the operation used in the git request 
     */
    String getOperation();
    /** 
     * set the rebase branch to use for rebase operation 
     * May be any valid commit, or an existing branch name.
     * Defaults to the configured upstream for the current branch.
     * 
     * see https://git-scm.com/docs/git-rebase for more information
     * 
     * @param The rebase branch to use
     */
    void setBranch(String branch);
    /** 
     * gets the branch used in rebase operation
     * 
     * @return the rebase branch 
     */
    String getBranch();
    
}
