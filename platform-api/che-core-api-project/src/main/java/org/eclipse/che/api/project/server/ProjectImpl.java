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
package org.eclipse.che.api.project.server;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.project.SourceStorage;
import org.eclipse.che.api.core.model.project.type.Attribute;
import org.eclipse.che.api.core.model.project.type.Value;
import org.eclipse.che.api.core.model.workspace.ProjectConfig;
import org.eclipse.che.api.project.server.type.AttributeValue;
import org.eclipse.che.api.project.server.type.BaseProjectType;
import org.eclipse.che.api.project.server.type.ProjectTypeDef;
import org.eclipse.che.api.project.server.type.Variable;
import org.eclipse.che.api.vfs.Path;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Internal Project implementation
 * It is supposed that it is object always consistent
 *
 * @author gazarenkov
 */
public class ProjectImpl {

    private final List<Problem> problems = new ArrayList<>();
    private final FolderEntry   folder;
    private final ProjectConfig config;
    private final ProjectTypes  types;
    private final Map<String, Value> attributes = new HashMap<>();
    private final Set<String>        modules    = new HashSet<>();
    private final NewProjectManager manager;
    private       boolean           updated;


    /**
     * Either root folder or config can be null, in this case Project is configured with problem
     *
     * @param folder
     *         - root local folder
     * @param config
     *         - project configuration in workspace
     * @param updated
     *         - if this object was updated, i.e. no more synchronized with workspace master
     * @param manager
     */
    public ProjectImpl(FolderEntry folder, ProjectConfig config, boolean updated, NewProjectManager manager)
            throws NotFoundException, ProjectTypeConstraintException, ServerException, InvalidValueException,
                   ValueStorageException {

        this.folder = folder;
        this.config = (config == null) ? new NullConfig(folder.getPath()) : config;
        this.updated = updated;
        this.manager = manager;

        if (folder == null || folder.isFile())
            problems.add(new Problem(10, "No project locally " + this.config.getPath()));
        if (config == null)
            problems.add(new Problem(11, "No project configured in workspace " + this.config.getPath()));

        this.types = new ProjectTypes(this.config, manager);

        // init transient (implicit, like git) project types.
        // TODO should we do that in constructor?
        types.addTransient();

        // initialize attributes
        initAttributes();

        // initialize modules
        for (ProjectConfig pc : this.config.getModules()) {
            this.modules.add(pc.getPath());
        }

    }

    private void initAttributes() throws InvalidValueException, ValueStorageException, ProjectTypeConstraintException {

        // we take only defined attributes, others ignored
        for (Map.Entry<String, Attribute> entry : types.getAttributeDefs().entrySet()) {

            Attribute definition = entry.getValue();
            String name = entry.getKey();
            AttributeValue value = new AttributeValue(config.getAttributes().get(name));

            if (!definition.isVariable()) {
                // constant, value always assumed as stated in definition
                this.attributes.put(name, definition.getValue());

            } else {
                // variable
                final Variable variable = (Variable)definition;
                final ValueProviderFactory valueProviderFactory = variable.getValueProviderFactory();

                if (valueProviderFactory != null) {
                    if (updated) {

                        // to update
                        valueProviderFactory.newInstance(folder).setValues(name, value.getList());

                    } else {

                        // to get from outside
                        value = new AttributeValue(valueProviderFactory.newInstance(folder).getValues(name));
                    }
                }

                if (value == null && variable.isRequired()) {
                    throw new ProjectTypeConstraintException("Required attribute value is initialized with null value " + variable.getId());
                }

                this.attributes.put(name, value);

            }

        }

    }

    public NewProjectManager getManager() {
        return this.manager;
    }

    public String getPath() {
        return manager.absolutizePath(config.getPath());
    }

    public String getName() {
        return config.getName();
    }

    public String getDescription() {
        return config.getDescription();
    }

    public SourceStorage getSource() {
        return config.getSource();
    }

    public ProjectTypeDef getType() {
        return types.getPrimary();
    }

    public Map<String, ProjectTypeDef> getMixins() {
        return types.getMixins();
    }

    public Map<String, ProjectTypeDef> getTypes() {
        return types.getAll();
    }

//    public Map<String, Attribute> getAttributeDefs() {
//        return types.getAttributeDefs();
//    }

    public Map<String, Value> getAttributes() {
        return attributes;
    }

//    public Map<String, Value> getPersistableAttributes() {
//        return null;
//    }


    public ProjectImpl getParent() {
        return manager.getProject(Path.of(getPath()).getParent().toString());
    }

    public Set<String> getModules() {
        return this.modules;
    }

    public void addModule(String modulePath) throws NotFoundException, InvalidValueException {

        if(manager.getProject(modulePath) == null)
            throw new NotFoundException("Module not found "+modulePath);

        if(modulePath.equals(getPath()))
            throw new InvalidValueException("Can not add myself as a module " + modulePath);

        if(!modules.contains(modulePath))
            this.modules.add(modulePath);

    }

    public void deleteModule(String modulePath) {
        this.modules.remove(modulePath);
    }



    public boolean isSynced() {
        return !this.updated;
    }

    public void setSync() {
        this.updated = false;
    }


    /**
     * @return root folder or null
     */
    public FolderEntry getBaseFolder() {
        return folder;
    }

    /**
     * @return project config or null
     */
//    public ProjectConfig getConfig() {
//        return config;
//    }

    /**
     * @return problems in case if root or config is null (project is not synced)
     */
    public List<Problem> getProblems() {
        return problems;
    }


    public Map<String, List <String>> getPersistableAttributes() {
        Map<String, List <String>> attrs = new HashMap<>();
        for(HashMap.Entry<String, Value> entry : getAttributes().entrySet()) {

            Attribute def = types.getAttributeDefs().get(entry.getKey());
            // not provided
            if(def != null &&
               ((def.isVariable() && ((Variable) def).getValueProviderFactory() == null) ||
               !def.isVariable()))
                attrs.put(entry.getKey(), entry.getValue().getList());
        }
        return attrs;
    }


    public class Problem {
        private Problem(int code, String message) {
            this.code = code;
            this.message = message;
        }

        int    code;
        String message;
    }

    public static class NullConfig implements ProjectConfig {

        private Path path;

        public NullConfig(Path path) {
            this.path = path;
        }

        @Override
        public String getName() {
            return path.getName();
        }

        @Override
        public String getPath() {
            return path.toString();
        }

        @Override
        public String getDescription() {
            return "";
        }

        @Override
        public String getType() {
            return BaseProjectType.ID;
        }

        @Override
        public List<String> getMixins() {
            return new ArrayList<>();
        }

        @Override
        public Map<String, List<String>> getAttributes() {
            return new HashMap<>();
        }

        @Override
        public List<? extends ProjectConfig> getModules() {
            return new ArrayList<>();
        }

        @Override
        public SourceStorage getSource() {
            return null;
        }

        @Override
        public String getContentRoot() {
            return null;
        }
    }

}
