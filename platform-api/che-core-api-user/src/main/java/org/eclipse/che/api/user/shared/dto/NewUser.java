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
package org.eclipse.che.api.user.shared.dto;

import org.eclipse.che.dto.shared.DTO;

/**
 * Describes new user
 *
 * @author Eugene Voevodin
 */
@DTO
public interface NewUser {

    String getEmail();

    void setEmail(String email);

    NewUser withEmail(String email);

    String getPassword();

    void setPassword(String password);

    NewUser withPassword(String password);
}
