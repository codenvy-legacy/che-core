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
import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * Template for NewSubscription
 *
 * @author Alexander Garagatyi
 */
@DTO
public interface NewSubscriptionTemplate {
    /* use object instead of primitive to avoid setting the default value on REST framework serialization/deserialization
     * that allow better validate data that was sent
    */

    @ApiModelProperty(value = "Account ID")
    String getAccountId();

    void setAccountId(String orgId);

    NewSubscriptionTemplate withAccountId(String orgId);

    @ApiModelProperty(value = "Plan ID")
    String getPlanId();

    void setPlanId(String id);

    NewSubscriptionTemplate withPlanId(String id);

    @ApiModelProperty(value = "Length of the trial")
    Integer getTrialDuration();

    void setTrialDuration(Integer trialDuration);

    NewSubscriptionTemplate withTrialDuration(Integer trialDuration);
}