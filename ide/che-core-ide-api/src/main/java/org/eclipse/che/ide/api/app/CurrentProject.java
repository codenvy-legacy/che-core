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
package org.eclipse.che.ide.api.app;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;

import java.util.List;

/**
 * Describe current state of project.
 *
 * @author Vitaly Parfonov
 * @author Valeriy Svydenko
 * @author Dmitry Shnurenko
 */
public class CurrentProject {

    private ProjectDescriptor projectDescription;
    private ProjectDescriptor rootProject;

    public CurrentProject(ProjectDescriptor projectDescription) {
        this.projectDescription = projectDescription;
        this.rootProject = projectDescription;
    }

    public ProjectDescriptor getProjectDescription() {
        return projectDescription;
    }

    public void setProjectDescription(ProjectDescriptor projectDescription) {
        this.projectDescription = projectDescription;
    }

    public ProjectDescriptor getRootProject() {
        return rootProject;
    }

    public void setRootProject(ProjectDescriptor rootProject) {
        this.rootProject = rootProject;
    }

    /**
     * Get value of attribute <code>name</code>.
     *
     * @param attributeName
     *         attribute name
     * @return value of attribute with specified name or <code>null</code> if attribute does not exists
     */
    public String getAttributeValue(String attributeName) {
        List<String> attributeValues = getAttributeValues(attributeName);
        if (attributeValues != null && !attributeValues.isEmpty()) {
            return attributeValues.get(0);
        }
        return null;
    }

    /**
     * Get all attributes which exists in project descriptor.
     *
     * @param attributeName
     *         attribute name
     * @return {@link List} of attribute values or <code>null</code> if attribute does not exists
     * @see #getAttributeValue(String)
     */
    public List<String> getAttributeValues(String attributeName) {
        return projectDescription.getAttributes().get(attributeName);
    }

    /**
     * Indicate that current user has only read rights for this project.
     *
     * @return true if user can only read this project, false otherwise
     */
    public boolean isReadOnly() {
        return projectDescription.getPermissions() != null && projectDescription.getPermissions().size() == 1
               && "read".equalsIgnoreCase(projectDescription.getPermissions().get(0));
    }
}
