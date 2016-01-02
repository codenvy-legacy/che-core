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
package org.eclipse.che.ide.api.project;

import com.google.inject.Inject;

import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.ide.api.project.type.ProjectTypeRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author gazarenkov
 */
public class ProjectFactory {

    private ProjectTypeRegistry ptRegistry;

    @Inject
    public ProjectFactory(ProjectTypeRegistry ptRegistry) {
        this.ptRegistry = ptRegistry;
    }

    public ProjectImpl createProject(ProjectConfigDto configDto) {

        Set<String> types = new HashSet<>();
        types.add(configDto.getType());
        types.addAll(configDto.getMixins());

        List<String> typeNames = new ArrayList<>();
        typeNames.add(configDto.getType());
        typeNames.addAll(configDto.getMixins());

        for(String id : typeNames) {
            types.addAll(ptRegistry.getProjectType(id).getAncestors());
        }

        return new ProjectImpl(configDto, types);

    }

}
