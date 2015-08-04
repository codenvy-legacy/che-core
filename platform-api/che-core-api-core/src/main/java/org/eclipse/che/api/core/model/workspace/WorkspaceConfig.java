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
package org.eclipse.che.api.core.model.workspace;

import org.eclipse.che.api.core.model.machine.Command;

import java.util.List;
import java.util.Map;

/**
 * @author gazarenkov
 */
public interface WorkspaceConfig {

    String getName();

    String getDescription();

    String getDefaultEnvironment();

    List<? extends Command> getCommands();

    List<? extends ProjectConfig> getProjects();

    Map<String, ? extends Environment> getEnvironments();

    Environment getEnvironment(String envId);

    Map<String, String> getAttributes();
}
