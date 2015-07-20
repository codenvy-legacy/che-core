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
package org.eclipse.che.api.machine.shared.dto;

import org.eclipse.che.dto.shared.DTO;

/**
 * Describes information needed for machine creation
 *
 * @author Alexander Garagatyi
 */
@DTO
public interface MachineCreationMetadata {
    /**
     * Channel of websocket where machine logs should be put
     */
    String getOutputChannel();

    void setOutputChannel(String outputChannel);

    MachineCreationMetadata withOutputChannel(String outputChannel);

    String getDisplayName();

    void setDisplayName(String displayName);

    MachineCreationMetadata withDisplayName(String displayName);

    /** Get memory size (in megabytes) that is allocated for starting machine. */
    int getMemorySize();

    void setMemorySize(int mem);

    MachineCreationMetadata withMemorySize(int mem);
}
