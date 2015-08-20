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
 * Git branch description.
 *
 * @author andrew00x
 */
@DTO
public interface Branch {
    /** @return full name of branch, e.g. 'refs/heads/master' */
    String getName();

    /** @return <code>true</code> if branch is checked out and false otherwise */
    boolean isActive();

    /** @return display name of branch, e.g. 'refs/heads/master' -> 'master' */
    String getDisplayName();

    /** @return <code>true</code> if branch is a remote branch */
    boolean isRemote();
    
    Branch withName(String name);
    
    Branch withDisplayName(String displayName);

    Branch withActive(boolean isActive);
    
    Branch withRemote(boolean isRemote);
}