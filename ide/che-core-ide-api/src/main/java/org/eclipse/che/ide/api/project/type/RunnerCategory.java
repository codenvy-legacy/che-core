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
package org.eclipse.che.ide.api.project.type;

import javax.validation.constraints.NotNull;

/**
 * The class contains values of runner categories.
 *
 * @author Dmitry Shnurenko
 */
public enum RunnerCategory {
    CPP("cpp"),
    GO("go"),
    JAVA("java"),
    JAVASCRIPT("javascript"),
    PHP("php"),
    PYTHON("python"),
    RUBY("ruby");

    private final String type;

    RunnerCategory(@NotNull String type) {
        this.type = type;
    }

    @NotNull
    @Override
    public String toString() {
        return type;
    }
}
