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
import org.eclipse.che.api.core.model.workspace.ProjectProblem;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of Project
 * @author gazarenkov
 */
public class ProjectImpl implements Project {

    private Set<String> types;
    private Map<String, List<String>> attributes;
    private SourceStorage sourceStorage;
    private String name;
    private String path;
    private String description;
    private String type;
    private List<String> mixins;
    private List<ProjectImpl> modules;

    ProjectImpl(ProjectConfigDto configDto, Set<String> types) {

        this.types = types;
        this.name = configDto.getName();
        this.path = configDto.getPath();
        this.description = configDto.getDescription();
        this.type = configDto.getType();

        if(configDto.getMixins() == null)
            this.mixins = new ArrayList<>();
        else
            this.mixins = configDto.getMixins();

        if(configDto.getAttributes() == null)
            this.attributes = new HashMap<>();
        else
            this.attributes = configDto.getAttributes();
    }


    @Override
    public Folder getRootFolder() {
        // TODO after new VFS
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public List<String> getMixins() {
        return mixins;
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    @Override
    public List<ProjectImpl> getModules() {
        return null;
    }

    public List<ProjectProblem> getProblems() {
        return null;
    }

    @Override
    public SourceStorage getSource() {
        return sourceStorage;
    }


    public List<Link> getLinks() {
        return null;
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

    /**
     * Whether the Project contains particular attribute and it's value equals to particular value
     * @param attrName - attribute name
     * @param value - value to compare, if null the method checks only attribute presence
     * @return true if so and false if there are no such attribute or it is not equal to value
     */
    public boolean isAttrEqual(String attrName, String value) {

        List<String> values = getAttributes().get(attrName);
        if(values != null) {

            if(value == null)
                return true;

            for(String v : values) {
                if(v.equals(value))
                    return true;
            }
        }

        return false;

    }


}
