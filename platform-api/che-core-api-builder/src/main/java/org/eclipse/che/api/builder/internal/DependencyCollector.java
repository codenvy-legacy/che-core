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
package org.eclipse.che.api.builder.internal;

import org.eclipse.che.api.builder.dto.Dependency;
import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.dto.server.JsonArrayImpl;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Collects dependencies of project and writes it in JSON format.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public final class DependencyCollector {
    private final List<Dependency> dependencies;

    public DependencyCollector() {
        dependencies = new ArrayList<>();
    }

    public void addDependency(Dependency dependency) {
        dependencies.add(dependency);
    }

    public void writeJson(java.io.File jsonFile) throws IOException {
        try (Writer writer = Files.newBufferedWriter(jsonFile.toPath(), Charset.forName("UTF-8"))) {
            writeJson(writer);
        }
    }

    public void writeJson(Writer output) throws IOException {
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        output.write(dtoFactory.toJson(new JsonArrayImpl<>(dependencies)));
    }
}
