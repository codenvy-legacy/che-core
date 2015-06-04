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
package org.eclipse.che.api.machine.shared.dto.event;

import org.eclipse.che.api.core.notification.EventOrigin;
import org.eclipse.che.dto.shared.DTO;

/**
 * Describes event about state of machine
 *
 * @author Alexander Garagatyi
 */
@EventOrigin("machine")
@DTO
public interface MachineStateEvent {
    enum EventType {
        CREATING,
        RUNNING,
        DESTROYING,
        DESTROYED,
        ERROR
    }

    EventType getEventType();

    void setEventType(EventType eventType);

    MachineStateEvent withEventType(EventType eventType);

    String getMachineId();

    void setMachineId(String machineId);

    MachineStateEvent withMachineId(String machineId);

    String getError();

    void setError(String error);

    MachineStateEvent withError(String error);
}
