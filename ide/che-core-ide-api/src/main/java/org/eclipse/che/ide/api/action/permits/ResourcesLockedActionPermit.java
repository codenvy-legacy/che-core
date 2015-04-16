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
package org.eclipse.che.ide.api.action.permits;

/**
 * Interface for check if the action is allowed.
 *
 * @author Oleksii Orel
 */
public interface ResourcesLockedActionPermit {

    /** return allowed status for the action. */
    boolean isAllowed();

    /** return account lock status. */
    boolean isAccountLocked();

    /** return workspace lock status. */
    boolean isWorkspaceLocked();
}
