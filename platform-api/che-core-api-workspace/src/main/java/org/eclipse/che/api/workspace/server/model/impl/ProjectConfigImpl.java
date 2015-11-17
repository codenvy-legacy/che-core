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
package org.eclipse.che.api.workspace.server.model.impl;


import org.eclipse.che.api.core.model.project.SourceStorage;
import org.eclipse.che.api.core.model.workspace.ProjectConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.toMap;

//TODO move?

/**
 * Data object for {@link ProjectConfig}.
 *
 * @author Eugene Voevodin
 */
public class ProjectConfigImpl extends ModuleConfigImpl implements ProjectConfig {


    private SourceStorageImpl         storage;

    public ProjectConfigImpl() {
    }

    public ProjectConfigImpl(ProjectConfig projectCfg) {
        super(projectCfg);
        if (projectCfg.getSource() != null) {
            storage = new SourceStorageImpl(projectCfg.getSource().getType(),
                                            projectCfg.getSource().getLocation(),
                                            projectCfg.getSource().getParameters());
        }
    }

    @Override
    public SourceStorage getSource() {
        return storage;
    }

    public void setStorage(SourceStorageImpl sourceStorage) {
        this.storage = sourceStorage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProjectConfigImpl)) return false;
        if (!super.equals(o)) return false;
        final ProjectConfigImpl other = (ProjectConfigImpl)o;
        return Objects.equals(storage, other.storage);
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = hash * 31 + Objects.hashCode(storage);
        return hash;
    }

    @Override
    public String toString() {
        return "ProjectConfigImpl{" +
               "name='" + super.getName() + '\'' +
               ", path='" + super.getPath() + '\'' +
               ", description='" + super.getDescription() + '\'' +
               ", type='" + super.getType() + '\'' +
               ", mixinTypes=" + super.getMixinTypes() +
               ", attributes=" + super.getAttributes() +
               ", storage=" + storage +
               '}';
    }
}
