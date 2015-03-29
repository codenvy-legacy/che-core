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

import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.dto.shared.DTO;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.util.List;
import java.util.Map;

/**
 * Describes subscription - a link between {@link org.eclipse.che.api.account.server.SubscriptionService} and {@link
 * org.eclipse.che.api.account.server.dao.Account}
 *
 * @author Eugene Voevodin
 * @author Alexander Garagatyi
 */
@DTO
public interface SubscriptionDescriptor {
    /* use object instead of primitive to avoid setting the default value on REST framework serialization/deserialization
     * that allow better validate data that was sent
    */

    @ApiModelProperty(value = "Unique subscription ID")
    String getId();

    void setId(String id);

    SubscriptionDescriptor withId(String id);

    @ApiModelProperty(value = "Account ID")
    String getAccountId();

    void setAccountId(String orgId);

    SubscriptionDescriptor withAccountId(String orgId);

    @ApiModelProperty(value = "Service ID")
    String getServiceId();

    void setServiceId(String id);

    SubscriptionDescriptor withServiceId(String id);

    @ApiModelProperty(value = "Plan ID that includes service, duration, package and RAM amount")
    String getPlanId();

    void setPlanId(String planId);

    SubscriptionDescriptor withPlanId(String planId);

    @ApiModelProperty(value = "Properties of the subscription")
    Map<String, String> getProperties();

    void setProperties(Map<String, String> properties);

    SubscriptionDescriptor withProperties(Map<String, String> properties);

    @ApiModelProperty(value = "Subscription state")
    SubscriptionState getState();

    void setState(SubscriptionState state);

    SubscriptionDescriptor withState(SubscriptionState state);

    @ApiModelProperty(value = "Date when subscription starts")
    String getStartDate();

    void setStartDate(String startDate);

    SubscriptionDescriptor withStartDate(String startDate);

    @ApiModelProperty(value = "Date when subscription ends")
    String getEndDate();

    void setEndDate(String endDate);

    SubscriptionDescriptor withEndDate(String endDate);

    @ApiModelProperty(value = "Date when trial starts")
    String getTrialStartDate();

    void setTrialStartDate(String trialStartDate);

    SubscriptionDescriptor withTrialStartDate(String trialStartDate);

    @ApiModelProperty(value = "Date when trial ends")
    String getTrialEndDate();

    void setTrialEndDate(String trialEndDate);

    SubscriptionDescriptor withTrialEndDate(String trialEndDate);

    @ApiModelProperty(value = "Is payment system used")
    Boolean getUsePaymentSystem();

    void setUsePaymentSystem(Boolean usePaymentSystem);

    SubscriptionDescriptor withUsePaymentSystem(Boolean usePaymentSystem);

    @ApiModelProperty(value = "Date when billing starts")
    String getBillingStartDate();

    void setBillingStartDate(String billingStartDate);

    SubscriptionDescriptor withBillingStartDate(String billingStartDate);

    @ApiModelProperty(value = "Date when billing ends")
    String getBillingEndDate();

    void setBillingEndDate(String billingEndDate);

    SubscriptionDescriptor withBillingEndDate(String billingEndDate);

    @ApiModelProperty(value = "Next date of billing")
    String getNextBillingDate();

    void setNextBillingDate(String nextBillingDate);

    SubscriptionDescriptor withNextBillingDate(String nextBillingDate);

    @ApiModelProperty(value = "Billing timeout")
    Integer getBillingCycle();

    void setBillingCycle(Integer billingCycle);

    SubscriptionDescriptor withBillingCycle(Integer billingCycle);

    @ApiModelProperty(value = "Type of the billing")
    BillingCycleType getBillingCycleType();

    void setBillingCycleType(BillingCycleType billingCycleType);

    SubscriptionDescriptor withBillingCycleType(BillingCycleType billingCycleType);

    Integer getBillingContractTerm();

    void setBillingContractTerm(Integer BillingContractTerm);

    SubscriptionDescriptor withBillingContractTerm(Integer BillingContractTerm);

    @ApiModelProperty(value = "Description of the subscription")
    String getDescription();

    void setDescription(String description);

    SubscriptionDescriptor withDescription(String description);

    void setLinks(List<Link> links);

    List<Link> getLinks();

    SubscriptionDescriptor withLinks(List<Link> links);
}