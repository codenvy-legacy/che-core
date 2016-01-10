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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author gazarenkov
 */
public class WorkspaceProjects {

    private ProjectTypeRegistry ptRegistry;
    private Map<String, ProjectImpl> projects = new HashMap<>();

    @Inject
    public WorkspaceProjects(ProjectTypeRegistry ptRegistry) {
        this.ptRegistry = ptRegistry;
    }

    /**
     * Initialises list of workspace projects
     * @param projectDtos
     */
    public void init(List<ProjectConfigDto> projectDtos) {

        for (ProjectConfigDto dto : projectDtos) {
            createProject(dto);
        }

    }

    public ProjectImpl createProject(ProjectConfigDto configDto) {

        Set<String> types = new HashSet<>();
        types.add(configDto.getType());
        types.addAll(configDto.getMixins());

        List<String> typeNames = new ArrayList<>();
        typeNames.add(configDto.getType());
        typeNames.addAll(configDto.getMixins());

        for (String id : typeNames) {
            types.addAll(ptRegistry.getProjectType(id).getAncestors());
        }

        ProjectImpl project = new ProjectImpl(configDto, types);
        projects.put(project.getName(), project);
        return project;

    }

    public ProjectImpl getProject(String name) {

        return projects.get(name);
    }

    /**
     * @param child - name of project to test
     * @return parent project or null if project is a root project
     */
    public ProjectImpl getParent(String child) {

        for(ProjectImpl parent : projects.values()) {
            for(ProjectImpl m : parent.getModules()) {
                if(m.getName().equals(child))
                    return parent;
            }
        }

        return null;

    }

    /**
     * @param child - name of project to test
     * @return true if "child" project has a parent
     */
    public boolean hasParent(String child) {
        return getParent(child) != null;
    }



}
