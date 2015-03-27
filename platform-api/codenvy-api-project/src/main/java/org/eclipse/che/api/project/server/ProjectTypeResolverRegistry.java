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
package org.eclipse.che.api.project.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Evgen Vidolob
 */
@Singleton
public class ProjectTypeResolverRegistry {

    private Set<ProjectTypeResolver> resolvers = new HashSet<>();

    @Inject
    public ProjectTypeResolverRegistry(Set<ProjectTypeResolver> resolvers) {
        for (ProjectTypeResolver resolver : resolvers) {
            register(resolver);
        }
    }

    public void register(ProjectTypeResolver resolver) {
        resolvers.add(resolver);
    }

    public void unregister(ProjectTypeResolver resolver) {
        resolvers.remove(resolver);
    }

    public Set<ProjectTypeResolver> getResolvers() {
        return new HashSet<>(resolvers);
    }
}
