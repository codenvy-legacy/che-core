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
package org.eclipse.che.api.project.shared.dto;

import org.eclipse.che.dto.shared.DTO;

import java.util.List;
import java.util.Map;

/**
 * Data transfer object (DTO) for org.eclipse.che.api.project.shared.ProjectTypeDescription.
 *
 * @author andrew00x
 */
@DTO
public interface ProjectTypeDescriptor {
    /** Get unique ID of type of project. */
    String getType();

    /** Set unique ID of type of project. */
    void setType(String type);

    ProjectTypeDescriptor withType(String type);

    /** Get display name of type of project. */
    String getTypeName();

    /** Set display name of type of project. */
    void setTypeName(String name);

    ProjectTypeDescriptor withTypeName(String name);

    /** Get project type category. */
    String getTypeCategory();

    /** Set project type category. */
    void setTypeCategory(String category);

    ProjectTypeDescriptor withTypeCategory(String category);

    List<AttributeDescriptor> getAttributeDescriptors();

    void setAttributeDescriptors(List<AttributeDescriptor> attributeDescriptors);

    ProjectTypeDescriptor withAttributeDescriptors(List<AttributeDescriptor> attributeDescriptors);

    List<ProjectTemplateDescriptor> getTemplates();

    void setTemplates(List<ProjectTemplateDescriptor> templates);

    ProjectTypeDescriptor withTemplates(List<ProjectTemplateDescriptor> templates);

    Map<String, String> getIconRegistry();

    void setIconRegistry(Map<String, String> iconRegistry);

    ProjectTypeDescriptor withIconRegistry(Map<String, String> iconRegistry);

    /** Gets builder configurations. */
    BuildersDescriptor getBuilders();

    void setBuilders(BuildersDescriptor builders);

    ProjectTypeDescriptor withBuilders(BuildersDescriptor builders);

    /** Gets runner configurations. */
    RunnersDescriptor getRunners();

    void setRunners(RunnersDescriptor runners);

    ProjectTypeDescriptor withRunners(RunnersDescriptor runners);
}
