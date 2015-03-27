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
package org.eclipse.che.api.account.server.dao;

import org.eclipse.che.api.account.shared.dto.BillingCycleType;
import org.eclipse.che.api.account.shared.dto.SubscriptionState;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Describes subscription - link between {@link org.eclipse.che.api.account.server.SubscriptionService} and
 * {@link Account}
 *
 * @author Eugene Voevodin
 * @author Alexander Garagatyi
 */
public class Subscription {
    private String              id;
    private String              accountId;
    private String              serviceId;
    private String              planId;
    private Map<String, String> properties;
    private SubscriptionState   state;
    private Date                startDate;
    private Date                endDate;
    private Date                trialStartDate;
    private Date                trialEndDate;
    private Boolean             usePaymentSystem;
    private Date                billingStartDate;
    private Date                billingEndDate;
    private Date                nextBillingDate;
    private Integer             billingCycle;
    private BillingCycleType    billingCycleType;
    private Integer             billingContractTerm;
    private String              description;

    public Subscription() {
    }

    public Subscription(Subscription other) {
        this.id = other.id;
        this.accountId = other.accountId;
        this.serviceId = other.serviceId;
        this.planId = other.planId;
        this.properties = new HashMap<>(other.getProperties());
        this.state = other.state;
        this.startDate = other.startDate;
        this.endDate = other.endDate;
        this.trialStartDate = other.trialStartDate;
        this.trialEndDate = other.trialEndDate;
        this.usePaymentSystem = other.usePaymentSystem;
        this.billingStartDate = other.billingStartDate;
        this.billingEndDate = other.billingEndDate;
        this.nextBillingDate = other.nextBillingDate;
        this.billingCycle = other.billingCycle;
        this.billingCycleType = other.billingCycleType;
        this.billingContractTerm = other.billingContractTerm;
        this.description = other.description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Subscription withId(String id) {
        this.id = id;
        return this;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public Subscription withAccountId(String accountId) {
        this.accountId = accountId;
        return this;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public Subscription withServiceId(String serviceId) {
        this.serviceId = serviceId;
        return this;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public Subscription withPlanId(String planId) {
        this.planId = planId;
        return this;
    }

    public Map<String, String> getProperties() {
        if (properties == null) {
            properties = new HashMap<>();
        }
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public Subscription withProperties(Map<String, String> properties) {
        this.properties = properties;
        return this;
    }

    public SubscriptionState getState() {
        return state;
    }

    public void setState(SubscriptionState state) {
        this.state = state;
    }

    public Subscription withState(SubscriptionState state) {
        this.state = state;
        return this;
    }

    public Boolean getUsePaymentSystem() {
        return usePaymentSystem;
    }

    public void setUsePaymentSystem(Boolean usePaymentSystem) {
        this.usePaymentSystem = usePaymentSystem;
    }

    public Subscription withUsePaymentSystem(Boolean usePaymentSystem) {
        this.usePaymentSystem = usePaymentSystem;
        return this;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Subscription withStartDate(Date startDate) {
        this.startDate = startDate;
        return this;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Subscription withEndDate(Date endDate) {
        this.endDate = endDate;
        return this;
    }

    public Date getTrialStartDate() {
        return trialStartDate;
    }

    public void setTrialStartDate(Date trialStartDate) {
        this.trialStartDate = trialStartDate;
    }

    public Subscription withTrialStartDate(Date trialStartDate) {
        this.trialStartDate = trialStartDate;
        return this;
    }

    public Date getTrialEndDate() {
        return trialEndDate;
    }

    public void setTrialEndDate(Date trialEndDate) {
        this.trialEndDate = trialEndDate;
    }

    public Subscription withTrialEndDate(Date trialEndDate) {
        this.trialEndDate = trialEndDate;
        return this;
    }

    public Integer getBillingCycle() {
        return billingCycle;
    }

    public void setBillingCycle(Integer billingCycle) {
        this.billingCycle = billingCycle;
    }

    public Subscription withBillingCycle(Integer billingCycle) {
        this.billingCycle = billingCycle;
        return this;
    }

    public BillingCycleType getBillingCycleType() {
        return billingCycleType;
    }

    public void setBillingCycleType(BillingCycleType billingCycleType) {
        this.billingCycleType = billingCycleType;
    }

    public Subscription withBillingCycleType(BillingCycleType billingCycleType) {
        this.billingCycleType = billingCycleType;
        return this;
    }

    public Integer getBillingContractTerm() {
        return billingContractTerm;
    }

    public void setBillingContractTerm(Integer billingContractTerm) {
        this.billingContractTerm = billingContractTerm;
    }

    public Subscription withBillingContractTerm(Integer billingContractTerm) {
        this.billingContractTerm = billingContractTerm;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Subscription withDescription(String description) {
        this.description = description;
        return this;
    }

    public Date getBillingStartDate() {
        return billingStartDate;
    }

    public void setBillingStartDate(Date billingStartDate) {
        this.billingStartDate = billingStartDate;
    }

    public Subscription withBillingStartDate(Date billingStartDate) {
        this.billingStartDate = billingStartDate;
        return this;
    }

    public Date getBillingEndDate() {
        return billingEndDate;
    }

    public void setBillingEndDate(Date billingEndDate) {
        this.billingEndDate = billingEndDate;
    }

    public Subscription withBillingEndDate(Date billingEndDate) {
        this.billingEndDate = billingEndDate;
        return this;
    }

    public Date getNextBillingDate() {
        return nextBillingDate;
    }

    public void setNextBillingDate(Date nextBillingDate) {
        this.nextBillingDate = nextBillingDate;
    }

    public Subscription withNextBillingDate(Date nextBillingDate) {
        this.nextBillingDate = nextBillingDate;
        return this;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(id);
        hash = 31 * hash + Objects.hashCode(accountId);
        hash = 31 * hash + Objects.hashCode(serviceId);
        hash = 31 * hash + Objects.hashCode(planId);
        hash = 31 * hash + Objects.hashCode(getProperties());
        hash = 31 * hash + Objects.hashCode(state);
        hash = 31 * hash + Objects.hashCode(startDate);
        hash = 31 * hash + Objects.hashCode(endDate);
        hash = 31 * hash + Objects.hashCode(trialStartDate);
        hash = 31 * hash + Objects.hashCode(trialEndDate);
        hash = 31 * hash + Objects.hashCode(usePaymentSystem);
        hash = 31 * hash + Objects.hashCode(billingStartDate);
        hash = 31 * hash + Objects.hashCode(billingEndDate);
        hash = 31 * hash + Objects.hashCode(nextBillingDate);
        hash = 31 * hash + Objects.hashCode(billingCycle);
        hash = 31 * hash + Objects.hashCode(billingCycleType);
        hash = 31 * hash + Objects.hashCode(billingContractTerm);
        hash = 31 * hash + Objects.hashCode(description);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Subscription)) {
            return false;
        }
        final Subscription other = (Subscription)obj;
        return Objects.equals(id, other.id) &&
               Objects.equals(accountId, other.accountId) &&
               Objects.equals(serviceId, other.serviceId) &&
               Objects.equals(planId, other.planId) &&
               Objects.equals(getProperties(), other.getProperties()) &&
               Objects.equals(state, other.state) &&
               Objects.equals(startDate, other.startDate) &&
               Objects.equals(endDate, other.endDate) &&
               Objects.equals(trialStartDate, other.trialStartDate) &&
               Objects.equals(trialEndDate, other.trialEndDate) &&
               Objects.equals(usePaymentSystem, other.usePaymentSystem) &&
               Objects.equals(billingStartDate, other.billingStartDate) &&
               Objects.equals(billingEndDate, other.billingEndDate) &&
               Objects.equals(nextBillingDate, other.nextBillingDate) &&
               Objects.equals(billingCycle, other.billingCycle) &&
               Objects.equals(billingCycleType, other.billingCycleType) &&
               Objects.equals(billingContractTerm, other.billingContractTerm) &&
               Objects.equals(description, other.description);
    }
}