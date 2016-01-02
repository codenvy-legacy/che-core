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

import org.eclipse.che.api.core.model.project.SourceStorage;
import org.eclipse.che.api.core.model.project.fs.Folder;
import org.eclipse.che.api.core.model.workspace.Project;
import org.eclipse.che.api.core.model.workspace.ProjectConfig;
import org.eclipse.che.api.core.model.workspace.ProjectProblem;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of Project
 * @author gazarenkov
 */
public class ProjectImpl implements Project {

    private ProjectConfigDto     configDto;
    private Set<String> types;

    public ProjectImpl(ProjectConfigDto configDto, Set<String> types) {
        this.configDto = configDto;
        this.types = types;
    }

    @Override
    public Folder getRootFolder() {
        // TODO after new VFS
        return null;
    }

    @Override
    public String getName() {
        return configDto.getName();
    }

    @Override
    public String getPath() {
        return configDto.getPath();
    }

    @Override
    public String getDescription() {
        return configDto.getDescription();
    }

    @Override
    public String getType() {
        return configDto.getType();
    }

    @Override
    public List<String> getMixins() {
        return configDto.getMixins();
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        return configDto.getAttributes();
    }

    @Override
    public List<? extends ProjectConfig> getModules() {
        return null;
    }

    public List<ProjectProblem> getProblems() {
        return null;
    }

    @Override
    public SourceStorage getSource() {
        return configDto.getSource();
    }


    public List<Link> getLinks() {
        return configDto.getLinks();
    }


    // TODO remove it from the model
    public String getContentRoot() {

        return null;
    }

    /**
     * Whether this Project has the type which is subtype of incoming typeId
     * @param typeId
     * @return true if so
     */
    public boolean isTypeOf(String typeId) {
        return types.contains(typeId);
    }

}
