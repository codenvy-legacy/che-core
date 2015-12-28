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
import org.eclipse.che.api.project.server.type.ProjectTypeDef;
import org.eclipse.che.api.project.shared.dto.ProjectTemplateDescriptor;
import org.eclipse.che.api.workspace.shared.dto.SourceStorageDto;
import org.eclipse.che.dto.server.DtoFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

/**
 * Reads project template descriptions that may be described in separate json-files for every project type. This file should be named as
 * &lt;project_type_id&gt;.json.
 *
 * @author Artem Zatsarynnyy
 */
@Singleton
public class ProjectTemplateDescriptionLoader {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectTemplateRegistry.class);

    /**
     * Describe path to the dir where to locate json file that describes templates for project types.
     * Json file must have name like: "projectTypeId".json (e.g, maven.json, python.json and so on)
     */
    @Inject(optional = true)
    @Named("project.template_descriptions_dir")
    private String templateDescriptionsDir;

    /**
     * Describe value to the dir where templates sources are located.
     * If in ImportSourceDescriptor.location is set in the path ${project.template_location_dir}
     * it will replaced with value that is set in configuration
     */
    @Inject(optional = true)
    @Named("project.template_location_dir")
    private String templateLocationDir;

    @Inject
    private Set<ProjectTypeDef> projectTypes;

    @Inject
    private ProjectTemplateRegistry templateRegistry;

    @Inject
    public ProjectTemplateDescriptionLoader() {
    }

    protected ProjectTemplateDescriptionLoader(String templateDescriptionsDir,
                                               String templateLocationDir,
                                               Set<ProjectTypeDef> projectTypes,
                                               ProjectTemplateRegistry templateRegistry) {
        this.templateDescriptionsDir = templateDescriptionsDir;
        this.templateLocationDir = templateLocationDir;
        this.projectTypes = projectTypes;
        this.templateRegistry = templateRegistry;
    }

    @PostConstruct
    public void start() {
        if (templateDescriptionsDir == null || !Files.exists(Paths.get(templateDescriptionsDir)) ||
            !Files.isDirectory(Paths.get(templateDescriptionsDir))) {
            LOG.warn("ProjectTemplateDescriptionLoader",
                     "The configuration of project templates descriptors wasn't found or some problem with configuration was found.");
            for (ProjectTypeDef projectType : projectTypes) {
                load(projectType.getId());
            }
        } else {
            Path dirPath = Paths.get(templateDescriptionsDir);
            for (ProjectTypeDef projectType : projectTypes) {
                load(dirPath, projectType.getId());
            }
        }
    }

    private void load(@NotNull Path dirPath, @NotNull String projectTypeId) {
        try {
            Path tmplConf = Paths.get(dirPath.toString() + "/" + projectTypeId + ".json");
            if (tmplConf != null && Files.exists(tmplConf)) {
                try (InputStream inputStream = Files.newInputStream(tmplConf)) {
                    resolveTemplate(projectTypeId, inputStream);
                }
            }
        } catch (IOException e) {
            LOG.debug(String.format("Can't load information about project templates for %s project type", projectTypeId), e);
        }
    }

    /**
     * Load project template descriptions for the specified project type.
     *
     * @param projectTypeId
     *         id of the project type for which templates should be loaded
     */
    private void load(String projectTypeId) {
        try {
            final URL url = Thread.currentThread().getContextClassLoader().getResource(projectTypeId + ".json");
            if (url != null) {
                try (InputStream inputStream = url.openStream()) {
                    resolveTemplate(projectTypeId, inputStream);
                }
            }
        } catch (IOException e) {
            LOG.debug(String.format("Can't load information about project templates for %s project type", projectTypeId), e);
        }
    }

    private void resolveTemplate(String projectTypeId, InputStream stream) throws IOException {
        final List<ProjectTemplateDescriptor> templates;
        templates = DtoFactory.getInstance().createListDtoFromJson(stream, ProjectTemplateDescriptor.class);
        for (ProjectTemplateDescriptor template : templates) {
            template.setProjectType(projectTypeId);
            SourceStorageDto templateSource = template.getSource();
            String location = templateSource.getLocation();
            if (location.contains("${project.template_location_dir}") && templateLocationDir != null) {
                templateSource.setLocation(location.replace("${project.template_location_dir}", templateLocationDir));
            }
        }
        templateRegistry.register(projectTypeId, templates);
    }
}
