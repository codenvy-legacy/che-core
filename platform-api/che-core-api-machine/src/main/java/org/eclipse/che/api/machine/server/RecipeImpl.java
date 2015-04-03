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
package org.eclipse.che.api.machine.server;

import org.eclipse.che.api.machine.shared.Recipe;
import org.eclipse.che.api.machine.shared.RecipeId;
import org.eclipse.che.api.machine.shared.dto.RecipeDescriptor;

/**
 * @author andrew00x
 */
public class RecipeImpl implements Recipe {
    public static RecipeImpl fromDescriptor(RecipeDescriptor descriptor) {
        return new RecipeImpl(null, descriptor.getType(), descriptor.getScript());
    }

    private final RecipeId id;
    private final String   type;
    private final String   script;

    public RecipeImpl(RecipeId id, String type, String script) {
        this.id = id;
        this.type = type;
        this.script = script;
    }

    @Override
    public RecipeId getId() {
        return id;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getScript() {
        return script;
    }
}
