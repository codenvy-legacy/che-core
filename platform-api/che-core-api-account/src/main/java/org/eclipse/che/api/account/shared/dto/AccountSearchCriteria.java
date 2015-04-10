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
package org.eclipse.che.api.account.shared.dto;

import org.eclipse.che.dto.shared.DTO;

/**
 * @author Anatoliy Bazko
 */
@DTO
public interface AccountSearchCriteria {

    String getName();

    void setName(String name);

    String getId();

    void setId(String id);

    String getEmailOwner();

    void setEmailOwner(String emailOwner);

    String getSubscription();

    void setSubscription(String subscription);
}