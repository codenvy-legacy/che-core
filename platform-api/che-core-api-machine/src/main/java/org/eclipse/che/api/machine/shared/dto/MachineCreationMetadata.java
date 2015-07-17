/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2014] Codenvy, S.A. 
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
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
