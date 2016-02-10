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
package org.eclipse.che.ide.projecttype;

import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.api.project.type.ProjectTypeImpl;
import org.eclipse.che.ide.api.project.type.ProjectTypeRegistry;

import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Vitaly Parfonov
 * @author Artem Zatsarynnyi
 */
public class ProjectTypeRegistryImpl implements ProjectTypeRegistry {

    private final Set<ProjectTypeImpl> types;

    public ProjectTypeRegistryImpl() {
        this.types = new HashSet<>();
    }

    @Nullable
    @Override
    public ProjectTypeImpl getProjectType(@NotNull String id) {
        for (ProjectTypeImpl type : types) {
            if (id.equals(type.getId())) {
                return type;
            }
        }
        return null;
    }

    @Override
    public Set<ProjectTypeImpl> getProjectTypes() {
        return types;
    }

    @Override
    public void register(ProjectTypeImpl projectType) {
        types.add(projectType);
    }
}
