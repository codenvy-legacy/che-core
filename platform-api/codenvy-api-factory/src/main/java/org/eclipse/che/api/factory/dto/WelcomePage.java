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
package org.eclipse.che.api.factory.dto;

import org.eclipse.che.dto.shared.DTO;

/**
 * Welcome page which user can specified to show when factory accepted.
 * To show custom information applied only for this factory url.
 * Contains two configuration for authenticated users and non authenticated.
 */
@DTO
@Deprecated
public interface WelcomePage {

    /**
     * @return
     */
    @Deprecated
    WelcomeConfiguration getAuthenticated();
    @Deprecated
    void setAuthenticated(WelcomeConfiguration authenticated);
    @Deprecated
    WelcomePage withAuthenticated(WelcomeConfiguration authenticated);

    /**
     * @return
     */
    @Deprecated
    WelcomeConfiguration getNonauthenticated();
    @Deprecated
    void setNonauthenticated(WelcomeConfiguration nonauthenticated);
    @Deprecated
    WelcomePage withNonauthenticated(WelcomeConfiguration nonauthenticated);
}
