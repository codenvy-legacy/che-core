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

import com.wordnik.swagger.annotations.ApiModelProperty;

import org.eclipse.che.dto.shared.DTO;

import java.util.List;

/**
 * @author Sergii Leschenko
 */
@DTO
public interface SubscriptionResourcesUsed extends UsedAccountResources {
    @ApiModelProperty(value = "Reference of subscription that provides resources")
    SubscriptionReference getSubscriptionReference();

    void setSubscriptionReference(SubscriptionReference subscriptionReference);

    SubscriptionResourcesUsed withSubscriptionReference(SubscriptionReference subscriptionReference);

    @Override
    SubscriptionResourcesUsed withUsed(List<WorkspaceResources> used);
}
