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
package org.eclipse.che.ide.projecttype.wizard.categoriespage;

import com.google.inject.ImplementedBy;

import org.eclipse.che.api.project.shared.dto.ProjectTemplateDescriptor;
import org.eclipse.che.ide.api.mvp.View;
import org.eclipse.che.ide.api.project.type.ProjectTypeImpl;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Evgen Vidolob
 */
@ImplementedBy(CategoriesPageViewImpl.class)
public interface CategoriesPageView extends View<CategoriesPageView.ActionDelegate> {

    void selectProjectType(String projectTypeId);

    void setCategories(Map<String, Set<ProjectTypeImpl>> typesByCategory,
                       Map<String, Set<ProjectTemplateDescriptor>> templatesByCategory);

    void updateCategories(boolean includeTemplates);

    void reset();

    void resetName();

    void setConfigOptions(List<String> options);

    void setName(String name);

    void setDescription(String description);

    void removeNameError();

    void showNameError();

    void focusName();

    void setProjectTypes(Set<ProjectTypeImpl> availableProjectTypes);

    interface ActionDelegate {

        void projectNameChanged(String name);

        void projectDescriptionChanged(String projectDescriptionValue);

        void projectTemplateSelected(ProjectTemplateDescriptor template);

        void projectTypeSelected(ProjectTypeImpl typeDescriptor);
    }
}
