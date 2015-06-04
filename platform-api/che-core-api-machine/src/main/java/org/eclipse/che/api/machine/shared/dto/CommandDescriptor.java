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
import org.eclipse.che.api.machine.shared.ManagedCommand;
import org.eclipse.che.dto.shared.DTO;

/**
 * Description of command to execute
 *
 * @author andrew00x
 * @author Eugene Voevodin
 */
@DTO
public interface CommandDescriptor extends ManagedCommand, Hyperlinks {

    /**
     * Channel of websocket where command execution logs should be put
     */
    String getOutputChannel();

    void setOutputChannel(String outputChannel);

    CommandDescriptor withOutputChannel(String outputChannel);

    CommandDescriptor withId(String id);

    void setId(String id);

    CommandDescriptor withName(String name);

    void setName(String name);

    CommandDescriptor withCreator(String creator);

    void setCreator(String creator);

    CommandDescriptor withCommandLine(String commandLine);

    void setCommandLine(String commandLine);

    CommandDescriptor withWorkspaceId(String workspaceId);

    void setWorkspaceId(String workspaceId);

    CommandDescriptor withVisibility(String visibility);

    void setVisibility(String visibility);

    CommandDescriptor withType(String type);

    void setType(String type);

    CommandDescriptor withWorkingDir(String workingDir);

    void setWorkingDir(String workingDir);
}
