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
package org.eclipse.che.api.workspace.shared.model;

import org.eclipse.che.api.user.shared.model.Membership;

import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @author gazarenkov
 */
public interface Workspace {
    String getId();

    String getName();

    void setName(String name);

    boolean isTemporary();

    Set<? extends Membership> getMembers();

    Map<String, String> getAttributes();

    List<? extends Command> getCommands();

    List<? extends ProjectConfig> getProjects();

    Environment getDefaultEnvironment();

    Map <String, ? extends Environment> getEnvironments();

}
