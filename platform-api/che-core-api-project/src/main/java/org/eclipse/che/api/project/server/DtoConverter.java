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

import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.core.util.LinksHelper;
import org.eclipse.che.api.project.server.type.AttributeValue;
import org.eclipse.che.api.project.server.type.ProjectType;
import org.eclipse.che.api.project.shared.Builders;
import org.eclipse.che.api.project.shared.Runners;
import org.eclipse.che.api.project.shared.dto.AttributeDescriptor;
import org.eclipse.che.api.project.shared.dto.BuilderConfiguration;
import org.eclipse.che.api.project.shared.dto.BuildersDescriptor;
import org.eclipse.che.api.project.shared.dto.ImportSourceDescriptor;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.project.shared.dto.ProjectImporterDescriptor;
import org.eclipse.che.api.project.shared.dto.ProjectProblem;
import org.eclipse.che.api.project.shared.dto.ProjectReference;
import org.eclipse.che.api.project.shared.dto.ProjectTemplateDescriptor;
import org.eclipse.che.api.project.shared.dto.ProjectTypeDefinition;
import org.eclipse.che.api.project.shared.dto.ProjectUpdate;
import org.eclipse.che.api.project.shared.dto.RunnerConfiguration;
import org.eclipse.che.api.project.shared.dto.RunnersDescriptor;

import org.eclipse.che.api.project.server.type.Attribute;
import org.eclipse.che.api.project.server.type.BaseProjectType;
import org.eclipse.che.api.project.server.type.ProjectTypeRegistry;
import org.eclipse.che.api.vfs.shared.dto.AccessControlEntry;
import org.eclipse.che.api.vfs.shared.dto.Principal;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.user.User;
import org.eclipse.che.dto.server.DtoFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.util.*;

/**
 * Helper methods for convert server essentials to DTO and back.
 *
 * @author andrew00x
 */
public class DtoConverter {

    /*================================ Method for conversion from DTO. ===============================*/

    private DtoConverter() { //converter
    }

    public static ProjectTemplateDescription fromDto(ProjectTemplateDescriptor dto) {
        final String category = dto.getCategory();
        final ImportSourceDescriptor importSource = dto.getSource();
        final BuildersDescriptor builders = dto.getBuilders();
        final RunnersDescriptor runners = dto.getRunners();
        return new ProjectTemplateDescription(
                category == null ? org.eclipse.che.api.project.shared.Constants.DEFAULT_TEMPLATE_CATEGORY : category,
                importSource == null ? null : importSource.getType(),
                dto.getDisplayName(),
                dto.getDescription(),
                importSource == null ? null : importSource.getLocation(),
                importSource == null ? null : importSource.getParameters(),
                builders == null ? null : fromDto(builders),
                runners == null ? null : fromDto(runners));
    }


    public static ProjectTemplateDescriptor toDto(ProjectTemplateDescription templateDescription) {
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        ImportSourceDescriptor sources = dtoFactory.createDto(ImportSourceDescriptor.class)
                                                   .withLocation(templateDescription.getLocation())
                                                   .withType(templateDescription.getImporterType());

        return dtoFactory.createDto(ProjectTemplateDescriptor.class).withDescription(templateDescription.getDescription())
                                                             .withBuilders(toDto(templateDescription.getBuilders()))
                                                             .withCategory(templateDescription.getCategory())
                                                             .withDisplayName(templateDescription.getDisplayName())
                                                             .withRunners(toDto(templateDescription.getRunners()))
                                                             .withSource(sources);
    }

    public static ProjectConfig fromDto2(ProjectUpdate dto, ProjectTypeRegistry typeRegistry) throws ServerException,
                                                                                                     ProjectTypeConstraintException,
                                                                                                     InvalidValueException,
                                                                                                     ValueStorageException {

        if(dto.getType() == null)
            throw new InvalidValueException("Invalid Project definition. Primary project type is not defined.");

        if(typeRegistry.getProjectType(dto.getType()) == null)
            throw new ProjectTypeConstraintException("Primary project type is not registered "+dto.getType());

        // primary
        final Set<ProjectType> validTypes = new HashSet<>();
        validTypes.add(typeRegistry.getProjectType(dto.getType()));

        // mixins
        final List<String> validMixins = new ArrayList<>();
        for(String typeId : dto.getMixinTypes()) {
            ProjectType mixinType = typeRegistry.getProjectType(typeId);
            if (mixinType != null) {  // otherwise just ignore
                validTypes.add(mixinType);
                validMixins.add(typeId);
            }
        }

        // attributes
        final Map<String, List<String>> updateAttributes = dto.getAttributes();
        final HashMap<String, AttributeValue> attributes = new HashMap<>(updateAttributes.size());
        for (Map.Entry<String, List<String>> entry : updateAttributes.entrySet()) {

            for(ProjectType projectType : validTypes) {
                Attribute attr = projectType.getAttribute(entry.getKey());
                if (attr != null) {
                    attributes.put(attr.getName(), new AttributeValue(entry.getValue()));

                }
            }
        }

        return new ProjectConfig(dto.getDescription(), dto.getType(), attributes,
                fromDto(dto.getRunners()), fromDto(dto.getBuilders()), validMixins);

    }



    /*================================ Methods for conversion to DTO. ===============================*/

    public static Builders fromDto(BuildersDescriptor dto) {
        if (dto == null)
            return null;
        if (dto.getConfigs() == null) {
            return null;
        }
        final Builders builders = new Builders(dto.getDefault());
        for (Map.Entry<String, BuilderConfiguration> e : dto.getConfigs().entrySet()) {
            final BuilderConfiguration config = e.getValue();
            if (config != null) {
                builders.getConfigs().put(e.getKey(), new Builders.Config(config.getOptions(), config.getTargets()));
            }
        }
        return builders;

    }

    public static Runners fromDto(RunnersDescriptor dto) {
        if(dto == null)
            return null;
        if (dto.getConfigs() == null) {
            return null;
        }
        final Runners runners = new Runners(dto.getDefault());
        for (Map.Entry<String, RunnerConfiguration> e : dto.getConfigs().entrySet()) {
            final RunnerConfiguration config = e.getValue();
            if (config != null) {
                runners.getConfigs().put(e.getKey(), new Runners.Config(config.getRam(), config.getOptions(), config.getVariables()));
            }
        }
        return runners;
    }


    public static ProjectTypeDefinition toTypeDescriptor2(ProjectType projectType) {

        final DtoFactory dtoFactory = DtoFactory.getInstance();
        final ProjectTypeDefinition definition = dtoFactory.createDto(ProjectTypeDefinition.class)
                .withId(projectType.getId())
                .withDisplayName(projectType.getDisplayName())
                .withRunnerCategories(projectType.getRunnerCategories())
                .withDefaultRunner(projectType.getDefaultRunner())
                .withDefaultBuilder(projectType.getDefaultBuilder())
                .withPrimaryable(projectType.canBePrimary())
                .withMixable(projectType.canBeMixin());

        final List<AttributeDescriptor> typeAttributes = new ArrayList<>();
        for (Attribute attr : projectType.getAttributes()) {

            List <String> valueList = null;

            try {
                if(attr.getValue() != null)
                  valueList = attr.getValue().getList();
            } catch (ValueStorageException e) {
            }

            typeAttributes.add(dtoFactory.createDto(AttributeDescriptor.class)
                                         .withName(attr.getName())
                                         .withDescription(attr.getDescription())
                                         .withRequired(attr.isRequired())
                                         .withVariable(attr.isVariable())
                                         .withValues(valueList));
        }
        definition.setAttributeDescriptors(typeAttributes);

        final List<String> parents = new ArrayList<>();
        for(ProjectType parent : projectType.getParents()) {
            parents.add(parent.getId());
        }
        definition.setParents(parents);

        return definition;
    }

    public static ProjectTemplateDescriptor toTemplateDescriptor(ProjectTemplateDescription projectTemplate, String projectType) {
        return toTemplateDescriptor(DtoFactory.getInstance(), projectTemplate, projectType);
    }

    private static ProjectTemplateDescriptor toTemplateDescriptor(DtoFactory dtoFactory, ProjectTemplateDescription projectTemplate, String projectType) {
        final ImportSourceDescriptor importSource = dtoFactory.createDto(ImportSourceDescriptor.class)
                                                              .withType(projectTemplate.getImporterType())
                                                              .withLocation(projectTemplate.getLocation())
                                                              .withParameters(projectTemplate.getParameters());
        final Builders builders = projectTemplate.getBuilders();
        final Runners runners = projectTemplate.getRunners();
        final ProjectTemplateDescriptor dto = dtoFactory.createDto(ProjectTemplateDescriptor.class)
                                                        .withDisplayName(projectTemplate.getDisplayName())
                                                        .withSource(importSource)
                                                        .withCategory(projectTemplate.getCategory())
                                                        .withProjectType(projectType)
                                                        .withDescription(projectTemplate.getDescription());
        if (builders != null) {
            dto.withBuilders(toDto(dtoFactory, builders));
        }
        if (runners != null) {
            dto.withRunners(toDto(dtoFactory, runners));
        }
        return dto;
    }

    public static ProjectImporterDescriptor toImporterDescriptor(ProjectImporter importer) {
        return DtoFactory.getInstance().createDto(ProjectImporterDescriptor.class)
                         .withId(importer.getId())
                         .withInternal(importer.isInternal())
                         .withDescription(importer.getDescription() != null ? importer.getDescription() : "description not found")
                         .withCategory(importer.getCategory().getValue());
    }

    public static ItemReference toItemReferenceDto(FileEntry file, UriBuilder uriBuilder) throws ServerException {
        return DtoFactory.getInstance().createDto(ItemReference.class)
                         .withName(file.getName())
                         .withPath(file.getPath())
                         .withType("file")
                         .withMediaType(file.getMediaType())
                         .withAttributes(file.getAttributes())
                         .withCreated(file.getCreated())
                         .withModified(file.getModified())
                         .withContentLength(file.getVirtualFile().getLength())
                         .withLinks(generateFileLinks(file, uriBuilder));
    }

    public static ItemReference toItemReferenceDto(FolderEntry folder, UriBuilder uriBuilder) throws ServerException {
        return DtoFactory.getInstance().createDto(ItemReference.class)
                         .withName(folder.getName())
                         .withPath(folder.getPath())
                         .withType(folder.isProjectFolder() ? "project" : "folder")
                         .withMediaType("text/directory")
                         .withAttributes(folder.getAttributes())
                         .withCreated(folder.getCreated())
                         .withModified(folder.getModified())
                         .withLinks(generateFolderLinks(folder, uriBuilder));
    }


    public static ProjectDescriptor toDescriptorDto2(Project project, UriBuilder uriBuilder, ProjectTypeRegistry ptRegistry)
    throws InvalidValueException {
        final EnvironmentContext environmentContext = EnvironmentContext.getCurrent();
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        final ProjectDescriptor dto = dtoFactory.createDto(ProjectDescriptor.class);
        // Try to provide as much as possible information about project.
        // If get error then save information about error with 'problems' field in ProjectConfig.
        final String wsId = project.getWorkspace();
        final String wsName = environmentContext.getWorkspaceName();
        final String name = project.getName();
        final String path = project.getPath();
        dto.withWorkspaceId(wsId).withWorkspaceName(wsName).withName(name).withPath(path);

        ProjectConfig config = null;
        try {
            config = project.getConfig();
        } catch (ServerException | ValueStorageException | ProjectTypeConstraintException e) {
            dto.getProblems().add(createProjectProblem(dtoFactory, e));
            dto.withType(BaseProjectType.ID);
        }

        if (config != null) {
            dto.withDescription(config.getDescription());
            String typeId = config.getTypeId();
            dto.withType(typeId)
               .withTypeName(ptRegistry.getProjectType(typeId).getDisplayName());

            dto.withMixins(config.getMixinTypes());

            final Map<String, AttributeValue> attributes = config.getAttributes();

            final Map<String, List<String>> attributesMap = new LinkedHashMap<>(attributes.size());
            if (!attributes.isEmpty()) {

               for (String attrName : attributes.keySet()) {
                 attributesMap.put(attrName, attributes.get(attrName).getList());
                }
            }
            dto.withAttributes(attributesMap);


            final Builders builders = config.getBuilders();
            if (builders != null) {
                dto.withBuilders(toDto(dtoFactory, builders));
            }
            final Runners runners = config.getRunners();
            if (runners != null) {
                dto.withRunners(toDto(dtoFactory, runners));
            }
        }

        final User currentUser = environmentContext.getUser();
        List<AccessControlEntry> acl = null;
        try {
            acl = project.getPermissions();
        } catch (ServerException e) {
            dto.getProblems().add(createProjectProblem(dtoFactory, e));
        }
        if (acl != null) {
            final List<String> permissions = new LinkedList<>();
            if (acl.isEmpty()) {
                // there is no any restriction at all
                permissions.add("all");
            } else {
                for (AccessControlEntry accessControlEntry : acl) {
                    final Principal principal = accessControlEntry.getPrincipal();
                    if ((Principal.Type.USER == principal.getType() && currentUser.getId().equals(principal.getName()))
                            || (Principal.Type.USER == principal.getType() && "any".equals(principal.getName()))
                            || (Principal.Type.GROUP == principal.getType() && currentUser.isMemberOf(principal.getName()))) {

                        permissions.addAll(accessControlEntry.getPermissions());
                    }
                }
            }
            dto.withPermissions(permissions);
        }

        try {
            dto.withCreationDate(project.getCreationDate());
        } catch (ServerException e) {
            dto.getProblems().add(createProjectProblem(dtoFactory, e));
        }

        try {
            dto.withModificationDate(project.getModificationDate());
        } catch (ServerException e) {
            dto.getProblems().add(createProjectProblem(dtoFactory, e));
        }

        try {
            dto.withVisibility(project.getVisibility());
        } catch (ServerException e) {
            dto.getProblems().add(createProjectProblem(dtoFactory, e));
        }

        if (uriBuilder != null) {
            dto.withBaseUrl(uriBuilder.clone().path(ProjectService.class, "getProject").build(wsId, path.substring(1)).toString())
               .withLinks(generateProjectLinks(project, uriBuilder));
            if (wsName != null) {
                dto.withIdeUrl(uriBuilder.clone().replacePath("ws").path(wsName).path(path).build().toString());
            }
        }

        return dto;
    }


    public static BuildersDescriptor toDto(Builders builders) {
        return toDto(DtoFactory.getInstance(), builders);
    }

    private static BuildersDescriptor toDto(DtoFactory dtoFactory, Builders builders) {
        BuildersDescriptor dto = dtoFactory.createDto(BuildersDescriptor.class).withDefault(builders.getDefault());
        final Map<String, Builders.Config> configs = builders.getConfigs();
        Map<String, BuilderConfiguration> configsDto = new LinkedHashMap<>(configs.size());
        for (Map.Entry<String, Builders.Config> e : configs.entrySet()) {
            final Builders.Config config = e.getValue();
            if (config != null) {
                configsDto.put(e.getKey(), dtoFactory.createDto(BuilderConfiguration.class)
                                                     .withOptions(config.getOptions())
                                                     .withTargets(config.getTargets())
                              );
            }
        }
        dto.withConfigs(configsDto);
        return dto;
    }

    public static RunnersDescriptor toDto(Runners runners) {
        return toDto(DtoFactory.getInstance(), runners);
    }

    private static RunnersDescriptor toDto(DtoFactory dtoFactory, Runners runners) {
        final RunnersDescriptor dto = dtoFactory.createDto(RunnersDescriptor.class).withDefault(runners.getDefault());
        final Map<String, Runners.Config> configs = runners.getConfigs();
        Map<String, RunnerConfiguration> configsDto = new LinkedHashMap<>(configs.size());
        for (Map.Entry<String, Runners.Config> e : configs.entrySet()) {
            final Runners.Config config = e.getValue();
            if (config != null) {
                configsDto.put(e.getKey(), dtoFactory.createDto(RunnerConfiguration.class)
                                                     .withRam(config.getRam())
                                                     .withOptions(config.getOptions())
                                                     .withVariables(config.getVariables())
                              );
            }
        }
        dto.withConfigs(configsDto);
        return dto;
    }

    private static List<Link> generateProjectLinks(Project project, UriBuilder uriBuilder) {
        final List<Link> links = generateFolderLinks(project.getBaseFolder(), uriBuilder);
        final String relPath = project.getPath().substring(1);
        final String workspace = project.getWorkspace();
        links.add(
                LinksHelper.createLink("PUT",
                                       uriBuilder.clone().path(ProjectService.class, "updateProject").build(workspace, relPath).toString(),
                                       MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON, Constants.LINK_REL_UPDATE_PROJECT));
        links.add(
                LinksHelper.createLink("GET",
                                       uriBuilder.clone().path(ProjectService.class, "getRunnerEnvironments").build(workspace, relPath)
                                                 .toString(),
                                       MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON, Constants.LINK_REL_GET_RUNNER_ENVIRONMENTS));
        return links;
    }

    private static List<Link> generateFolderLinks(FolderEntry folder, UriBuilder uriBuilder) {
        final List<Link> links = new LinkedList<>();
        final String workspace = folder.getWorkspace();
        final String relPath = folder.getPath().substring(1);
        //String method, String href, String produces, String rel
        links.add(LinksHelper.createLink("GET",
                                         uriBuilder.clone().path(ProjectService.class, "exportZip").build(workspace, relPath).toString(),
                                         "application/zip", Constants.LINK_REL_EXPORT_ZIP));
        links.add(LinksHelper.createLink("GET",
                                         uriBuilder.clone().path(ProjectService.class, "getChildren").build(workspace, relPath).toString(),
                                         MediaType.APPLICATION_JSON, Constants.LINK_REL_CHILDREN));
        links.add(
                LinksHelper.createLink("GET", uriBuilder.clone().path(ProjectService.class, "getTree").build(workspace, relPath).toString(),
                                       null, MediaType.APPLICATION_JSON, Constants.LINK_REL_TREE));
        links.add(LinksHelper.createLink("GET",
                                         uriBuilder.clone().path(ProjectService.class, "getModules").build(workspace, relPath).toString(),
                                         MediaType.APPLICATION_JSON, Constants.LINK_REL_MODULES));
        links.add(LinksHelper.createLink("DELETE",
                                         uriBuilder.clone().path(ProjectService.class, "delete").build(workspace, relPath).toString(),
                                         Constants.LINK_REL_DELETE));
        return links;
    }

    private static List<Link> generateFileLinks(FileEntry file, UriBuilder uriBuilder) throws ServerException {
        final List<Link> links = new LinkedList<>();
        final String workspace = file.getWorkspace();
        final String relPath = file.getPath().substring(1);
        links.add(
                LinksHelper.createLink("GET", uriBuilder.clone().path(ProjectService.class, "getFile").build(workspace, relPath).toString(),
                                       null, file.getMediaType(), Constants.LINK_REL_GET_CONTENT));
        links.add(LinksHelper.createLink("PUT",
                                         uriBuilder.clone().path(ProjectService.class, "updateFile").build(workspace, relPath).toString(),
                                         MediaType.WILDCARD, null, Constants.LINK_REL_UPDATE_CONTENT));
        links.add(LinksHelper.createLink("DELETE",
                                         uriBuilder.clone().path(ProjectService.class, "delete").build(workspace, relPath).toString(),
                                         Constants.LINK_REL_DELETE));
        return links;
    }



    public static ProjectReference toReferenceDto2(Project project, UriBuilder uriBuilder) throws InvalidValueException {
        final EnvironmentContext environmentContext = EnvironmentContext.getCurrent();
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        final ProjectReference dto = dtoFactory.createDto(ProjectReference.class);
        final String wsId = project.getWorkspace();
        final String wsName = environmentContext.getWorkspaceName();
        final String name = project.getName();
        final String path = project.getPath();
        dto.withName(name).withPath(path).withWorkspaceId(wsId).withWorkspaceName(wsName);
        dto.withWorkspaceId(wsId).withWorkspaceName(wsName).withName(name).withPath(path);

        try {


            final ProjectConfig projectConfig = project.getConfig();
            dto.withDescription(projectConfig.getDescription()).withType(projectConfig.getTypeId());

        } catch (ServerException | ValueStorageException | ProjectTypeConstraintException e) {
            dto.withType("blank").withTypeName("blank");
            dto.getProblems().add(createProjectProblem(dtoFactory, e));
        }

        try {
            dto.withCreationDate(project.getCreationDate());
        } catch (ServerException e) {
            dto.getProblems().add(createProjectProblem(dtoFactory, e));
        }

        try {
            dto.withModificationDate(project.getModificationDate());
        } catch (ServerException e) {
            dto.getProblems().add(createProjectProblem(dtoFactory, e));
        }

        try {
            dto.withVisibility(project.getVisibility());
        } catch (ServerException e) {
            dto.getProblems().add(createProjectProblem(dtoFactory, e));
        }

        dto.withUrl(uriBuilder.clone().path(ProjectService.class, "getProject").build(wsId, name).toString());
        if (wsName != null) {
            dto.withIdeUrl(uriBuilder.clone().replacePath("ws").path(wsName).path(path).build().toString());
        }
        return dto;
    }

    private static ProjectProblem createProjectProblem(DtoFactory dtoFactory, ApiException error) {
        // TODO: setup error code
        return dtoFactory.createDto(ProjectProblem.class).withCode(1).withMessage(error.getMessage());
    }
}
