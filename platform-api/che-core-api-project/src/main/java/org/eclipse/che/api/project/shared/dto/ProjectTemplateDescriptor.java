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

/** @author Vitaly Parfonov */
@DTO
public interface ProjectTemplateDescriptor {

    /** Get project type of project template. */
    String getProjectType();

    /** Set project type of project template. */
    void setProjectType(String projectType);

    ProjectTemplateDescriptor withProjectType(String projectType);

    /** Get category of project template. */
    String getCategory();

    /** Set category of project template. */
    void setCategory(String category);

    ProjectTemplateDescriptor withCategory(String category);

    //

    ImportSourceDescriptor getSource();

    void setSource(ImportSourceDescriptor sources);

    ProjectTemplateDescriptor withSource(ImportSourceDescriptor sources);

    //

    /** Get display name of project template. */
    String getDisplayName();

    /** Set display name of project template. */
    void setDisplayName(String displayName);

    ProjectTemplateDescriptor withDisplayName(String displayName);

    //

    /** Get description of project template. */
    String getDescription();

    /** Set description of project template. */
    void setDescription(String description);

    ProjectTemplateDescriptor withDescription(String description);

    //

    String getRecipe();

    void setRecipe(String recipe);

    ProjectTemplateDescriptor withRecipe(String recipe);

    //

    /** Gets builder configurations. */
//    BuildersDescriptor getBuilders();
//
//    /** Sets builder configurations. */
//    void setBuilders(BuildersDescriptor builders);
//
//    ProjectTemplateDescriptor withBuilders(BuildersDescriptor builders);
//
//    //
//
//    /** Gets runner configurations. */
//    RunnersDescriptor getRunners();
//
//    /** Sets runner configurations. */
//    void setRunners(RunnersDescriptor runners);
//
//    ProjectTemplateDescriptor withRunners(RunnersDescriptor runners);
}
