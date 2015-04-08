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

import org.eclipse.che.api.project.shared.*;

import java.util.Map;

/**
 * The description of project template.
 *
 * @author vitalka
 */
public class ProjectTemplateDescription {

    private final String              category;
    private final String              importerType;
    private final String              displayName;
    private final String              description;
    private final String              location;
    private final Map<String, String> parameters;
    private final Builders builders;
    private final Runners runners;

    /**
     * Create new ProjectTemplateDescription with default category eq @see defaultCategory.
     *
     * @param category
     *         category of this template. Categories maybe used for creation group of similar templates
     * @param importerType
     *         importer name like git, zip that maybe used fot import template to IDE
     * @param displayName
     *         display name of this template
     * @param description
     *         description of this template
     * @param location
     *         location of template, importer uses it when import templates to IDE
     * @param builders
     *         builders configuration for this template
     * @param runners
     *         runners configuration for this template
     */
    public ProjectTemplateDescription(String category,
                                      String importerType,
                                      String displayName,
                                      String description,
                                      String location,
                                      Map<String, String> parameters,
                                      Builders builders,
                                      Runners runners) {
        this.category = category;
        this.importerType = importerType;
        this.displayName = displayName;
        this.description = description;
        this.location = location;
        this.parameters = parameters;
        this.builders = builders;
        this.runners = runners;
    }

    /**
     * Create new ProjectTemplateDescription with default category eq @see defaultCategory.
     *
     * @param category
     *         category of this template. Categories maybe used for creation group of similar templates
     * @param importerType
     *         importer name like git, zip that maybe used fot import template to IDE
     * @param displayName
     *         display name of this template
     * @param description
     *         description of this template
     * @param location
     *         location of template, importer uses it when import templates to IDE
     */
    public ProjectTemplateDescription(String category,
                                      String importerType,
                                      String displayName,
                                      String description,
                                      String location,
                                      Map<String, String> parameters) {
        this(category, importerType, displayName, description, location, parameters, null, null);
    }

    /**
     * Create new ProjectTemplateDescription with default category eq @see defaultCategory.
     *
     * @param category
     *         category of this template. Categories maybe used for creation group of similar templates
     * @param importerType
     *         importer name like git, zip that maybe used fot import template to IDE
     * @param displayName
     *         display name of this template
     * @param description
     *         description of this template
     * @param location
     *         location of template, importer uses it when import templates to IDE
     */
    public ProjectTemplateDescription(String category, String importerType, String displayName, String description, String location) {
        this(category, importerType, displayName, description, location, null);
    }

//    /**
//     * Create new ProjectTemplateDescription with default category eq @see defaultCategory.
//     *
//     * @param importerType
//     *         importer name like git, zip that maybe used fot import template to IDE
//     * @param displayName
//     *         display name of this template
//     * @param description
//     *         description of this template
//     * @param location
//     *         location of template, importer uses it when import templates to IDE
//     */
//    public ProjectTemplateDescription(String importerType, String displayName, String description, String location) {
//        this(org.eclipse.che.api.project.shared.Constants.DEFAULT_TEMPLATE_CATEGORY, importerType, displayName, description, location);
//    }

    /**
     * Gets type of "importer" that can recognize sources template, sources located at specified {@code location}.
     *
     * @return type of "importer" that can recognize sources template
     */
    public String getImporterType() {
        return importerType;
    }

    /**
     * Gets display name of this project template.
     *
     * @return display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets location of this project template.
     *
     * @return location, e.g. path to the zip or git URL
     */
    public String getLocation() {
        return location;
    }

    /**
     * Gets parameters of this project template.
     *
     * @return parameters
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * Get description of this project template.
     *
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets category of this project template. Categories maybe used for creation group of similar templates.
     *
     * @return category
     */
    public String getCategory() {
        return category;
    }

    /**
     * Gets builders configuration for this template.
     *
     * @return builders configuration for this template
     */
    public Builders getBuilders() {
        return builders;
    }

    /**
     * Gets runners configuration for this template.
     *
     * @return runners configuration for this template
     */
    public Runners getRunners() {
        return runners;
    }

    @Override
    public String toString() {
        return "ProjectTemplateDescription{" +
               "category='" + category + '\'' +
               ", importerType='" + importerType + '\'' +
               ", displayName='" + displayName + '\'' +
               ", description='" + description + '\'' +
               ", location='" + location + '\'' +
               '}';
    }
}