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
package org.eclipse.che.ide.projecttype;

import org.eclipse.che.api.project.shared.dto.ProjectTemplateDescriptor;
import org.eclipse.che.ide.api.project.type.ProjectTemplateRegistry;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation for {@link ProjectTemplateRegistry}.
 *
 * @author Artem Zatsarynnyy
 */
public class ProjectTemplateRegistryImpl implements ProjectTemplateRegistry {
    private final Map<String, List<ProjectTemplateDescriptor>> templateDescriptors;

    public ProjectTemplateRegistryImpl() {
        templateDescriptors = new HashMap<>();
    }

    @Override
    public void register(@Nonnull ProjectTemplateDescriptor descriptor) {
        final String projectTypeId = descriptor.getProjectType();
        List<ProjectTemplateDescriptor> templates = templateDescriptors.get(projectTypeId);
        if (templates == null) {
            templates = new ArrayList<>();
            templates.add(descriptor);
            templateDescriptors.put(projectTypeId, templates);
        }
        templates.add(descriptor);
    }

    @Nonnull
    @Override
    public List<ProjectTemplateDescriptor> getTemplateDescriptors(@Nonnull String projectTypeId) {
        List<ProjectTemplateDescriptor> templateDescriptors = this.templateDescriptors.get(projectTypeId);
        if (templateDescriptors != null) {
            return templateDescriptors;
        }
        return new ArrayList<>();
    }
}
