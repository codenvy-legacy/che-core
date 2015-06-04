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

import org.eclipse.che.api.machine.shared.ManagedCommand;
import org.eclipse.che.dto.shared.DTO;

/**
 * @author Eugene Voevodin
 */
@DTO
public interface CommandUpdate extends ManagedCommand {

    void setId(String id);

    CommandUpdate withId(String id);

    void setName(String name);

    CommandUpdate withName(String name);

    void setCommandLine(String commandLine);

    CommandUpdate withCommandLine(String commandLine);

    void setVisibility(String visibility);

    CommandUpdate withVisibility(String visibility);

    void setWorkingDir(String workingDir);

    CommandUpdate withWorkingDir(String workingDir);
}
