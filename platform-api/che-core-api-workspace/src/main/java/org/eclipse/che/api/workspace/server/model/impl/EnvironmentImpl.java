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

import org.eclipse.che.api.core.model.machine.Recipe;
import org.eclipse.che.api.core.model.workspace.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

//TODO move?

/**
 * Data object for {@link Environment}.
 *
 * @author Eugene Voevodin
 */
public class EnvironmentImpl implements Environment {

    private String                  name;
    private Recipe                  recipe;
    private List<MachineConfigImpl> machineConfigs;

    @Override
    public String getName() {
        return name;
    }

    public EnvironmentImpl setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public Recipe getRecipe() {
        return recipe;
    }

    public EnvironmentImpl setRecipe(Recipe recipe) {
        this.recipe = recipe;
        return this;
    }

    @Override
    public List<MachineConfigImpl> getMachineConfigs() {
        if (machineConfigs == null) {
            machineConfigs = new ArrayList<>();
        }
        return machineConfigs;
    }

    public EnvironmentImpl setMachineConfigs(List<MachineConfigImpl> machineConfigs) {
        this.machineConfigs = machineConfigs;
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof EnvironmentImpl)) return false;
        final EnvironmentImpl other = (EnvironmentImpl)obj;
        return Objects.equals(name, other.name) &&
               Objects.equals(recipe, other.recipe) &&
               getMachineConfigs().equals(other.getMachineConfigs());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = hash * 31 + Objects.hashCode(name);
        hash = hash * 31 + Objects.hashCode(recipe);
        hash = hash * 31 + getMachineConfigs().hashCode();
        return hash;
    }

    @Override
    public String toString() {
        return "EnvironmentImpl{" +
               "name='" + name + '\'' +
               ", recipe=" + recipe +
               ", machineConfigs=" + machineConfigs +
               '}';
    }
}
