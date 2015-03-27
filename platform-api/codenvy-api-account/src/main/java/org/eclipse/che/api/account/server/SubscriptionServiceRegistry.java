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
package org.eclipse.che.api.account.server;

import com.google.inject.Singleton;

import javax.inject.Inject;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores available subscription services
 *
 * @author Eugene Voevodin
 */
@Singleton
public class SubscriptionServiceRegistry {

    private final Map<String, SubscriptionService> services;

    @Inject
    public SubscriptionServiceRegistry(Set<SubscriptionService> services) {
        this.services = new ConcurrentHashMap<>();
        for (SubscriptionService service : services) {
            add(service);
        }
    }

    public void add(SubscriptionService service) {
        services.put(service.getServiceId(), service);
    }

    public SubscriptionService get(String serviceId) {
        if (serviceId == null) {
            return null;
        }
        return services.get(serviceId);
    }

    public SubscriptionService remove(String serviceId) {
        if (serviceId == null) {
            return null;
        }
        return services.remove(serviceId);
    }

    public Set<SubscriptionService> getAll() {
        return new LinkedHashSet<>(services.values());
    }

    public void clear() {
        services.clear();
    }
}