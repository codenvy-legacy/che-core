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

import org.eclipse.che.api.core.rest.shared.dto.Hyperlinks;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.machine.shared.*;
import org.eclipse.che.dto.shared.DTO;

import java.util.List;

/**
 * Describes process created from {@link Command} in machine
 *
 * @author andrew00x
 */
@DTO
public interface ProcessDescriptor extends org.eclipse.che.api.machine.shared.Process, Hyperlinks {
    void setPid(int pid);

    ProcessDescriptor withPid(int pid);

    void setCommandLine(String commandLine);

    ProcessDescriptor withCommandLine(String commandLine);

    void setAlive(boolean isAlive);

    ProcessDescriptor withAlive(boolean isAlive);

    @Override
    ProcessDescriptor withLinks(List<Link> links);
}
