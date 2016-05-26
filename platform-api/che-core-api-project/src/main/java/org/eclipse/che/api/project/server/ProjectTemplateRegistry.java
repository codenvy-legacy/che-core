/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.project.server;

import org.eclipse.che.api.project.shared.dto.ProjectTemplateDescriptor;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Vitaly Parfonov
 */
@Singleton
public class ProjectTemplateRegistry {

    private final  Map<String, List<ProjectTemplateDescriptor>> templates = new ConcurrentHashMap<>();

    public void register(String projectTypeId, ProjectTemplateDescriptor template) {
        List<ProjectTemplateDescriptor> templateList = templates.get(projectTypeId);
        if (templateList == null) {
            templates.put(projectTypeId, templateList = new CopyOnWriteArrayList<>());
        }
        templateList.add(template);
    }

    public void register(String projectTypeId, List<ProjectTemplateDescriptor> templates) {
        List<ProjectTemplateDescriptor> templateList = this.templates.get(projectTypeId);
        if (templateList == null) {
            this.templates.put(projectTypeId, new CopyOnWriteArrayList<>(templates));
        } else {
            templateList.addAll(templates);
        }
    }

    public List<ProjectTemplateDescriptor> getTemplates(String projectType) {
        return templates.get(projectType);
    }

    public List<ProjectTemplateDescriptor> getAllTemplates() {
        List<ProjectTemplateDescriptor> allTemplates = new ArrayList<>();
        for (Map.Entry<String, List<ProjectTemplateDescriptor>> entry : templates.entrySet()) {
            allTemplates.addAll(entry.getValue());
        }
        return allTemplates;
    }
}
