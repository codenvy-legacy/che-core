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
package org.eclipse.che.api.machine.server.spi;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.machine.server.MachineException;
import org.eclipse.che.api.machine.shared.ProjectBinding;

import java.io.File;
import java.util.List;

/**
 * Representation of machine instance in implementation specific way.
 *
 * @author gazarenkov
 */
public interface Instance {

    InstanceMetadata getMetadata() throws MachineException;

    InstanceProcess getProcess(int pid) throws NotFoundException, MachineException;

    List<InstanceProcess> getProcesses() throws MachineException;

    InstanceProcess createProcess(String commandLine) throws MachineException;

    ImageKey saveToImage(String owner, String label) throws MachineException;

    void destroy() throws MachineException;

    /**
     * Binds project to machine instance
     *
     * @param workspaceId workspace where project is placed
     * @param project project that should be bound to machine instance
     */
    void bindProject(String workspaceId, ProjectBinding project) throws MachineException;

    /**
     * Unbinds project from machine instance
     *
     * @param workspaceId workspace where project is placed
     * @param project project that should be unbound from machine instance
     */
    void unbindProject(String workspaceId, ProjectBinding project) throws MachineException;
}
