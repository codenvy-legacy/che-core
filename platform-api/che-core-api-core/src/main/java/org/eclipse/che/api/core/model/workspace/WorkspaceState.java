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
package org.eclipse.che.api.core.model.workspace;

/**
 * Defines workspace state.
 *
 * @author Alexander Garagatyi
 */
public interface WorkspaceState {
    enum WorkspaceStatus {
        STARTING, RUNNING, STOPPED
    }

    /**
     * Returns true if this workspace is temporary otherwise returns false.
     */
    boolean isTemporary();

    /**
     * Returns workspace identifier.
     */
    String getId();

    /**
     * Returns workspace owner(users identifier).
     */
    String getOwner();

    /**
     * Returns workspace name.
     */
    String getName();

    /**
     * Returns status of current workspace.
     */
    WorkspaceStatus getStatus();
}
