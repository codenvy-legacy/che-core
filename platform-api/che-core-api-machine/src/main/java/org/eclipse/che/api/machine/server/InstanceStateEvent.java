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
package org.eclipse.che.api.machine.server;

/**
 * Describe instance state change.
 *
 * @author Alexander Garagatyi
 */
public class InstanceStateEvent {
    /**
     * Type of state change of a machine instance.<br>
     * Consider that machine implementation may or may not support each state change type.
     */
    public enum Type {
        DIE,
        OOM
    }

    private String machineId;
    private Type   type;

    public InstanceStateEvent(String machineId, Type type) {
        this.machineId = machineId;
        this.type = type;
    }

    public String getMachineId() {
        return machineId;
    }

    public Type getType() {
        return type;
    }
}
