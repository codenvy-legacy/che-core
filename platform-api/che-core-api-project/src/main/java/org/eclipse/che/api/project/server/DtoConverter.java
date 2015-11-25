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
import org.eclipse.che.api.core.model.workspace.ModuleConfig;
import org.eclipse.che.api.core.model.workspace.ProjectConfig;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.core.util.LinksHelper;
import org.eclipse.che.api.project.server.type.Attribute;
import org.eclipse.che.api.project.server.type.BaseProjectType;
import org.eclipse.che.api.project.server.type.ProjectType;
import org.eclipse.che.api.project.server.type.ProjectTypeRegistry;
import org.eclipse.che.api.project.shared.dto.AttributeDescriptor;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.project.shared.dto.ProjectImporterDescriptor;
import org.eclipse.che.api.project.shared.dto.ProjectProblem;
import org.eclipse.che.api.project.shared.dto.ProjectReference;
import org.eclipse.che.api.project.shared.dto.ProjectTemplateDescriptor;
import org.eclipse.che.api.project.shared.dto.ProjectTypeDefinition;
import org.eclipse.che.api.vfs.shared.dto.AccessControlEntry;
import org.eclipse.che.api.vfs.shared.dto.Principal;
import org.eclipse.che.api.workspace.shared.dto.ModuleConfigDto;
import org.eclipse.che.api.workspace.shared.dto.SourceStorageDto;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.lang.ws.rs.ExtMediaType;
import org.eclipse.che.commons.user.User;
import org.eclipse.che.dto.server.DtoFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static javax.ws.rs.HttpMethod.DELETE;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.PUT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.che.api.project.server.Constants.LINK_REL_CHILDREN;
import static org.eclipse.che.api.project.server.Constants.LINK_REL_DELETE;
import static org.eclipse.che.api.project.server.Constants.LINK_REL_EXPORT_ZIP;
import static org.eclipse.che.api.project.server.Constants.LINK_REL_GET_CONTENT;
import static org.eclipse.che.api.project.server.Constants.LINK_REL_MODULES;
import static org.eclipse.che.api.project.server.Constants.LINK_REL_TREE;
import static org.eclipse.che.api.project.server.Constants.LINK_REL_UPDATE_CONTENT;
import static org.eclipse.che.api.project.server.Constants.LINK_REL_UPDATE_PROJECT;

/**
 * Helper methods for convert server essentials to DTO and back.
 *
 * @author andrew00x
 */
public class DtoConverter {

    private DtoConverter() {
    }

    public static ProjectTypeDefinition toTypeDefinition(ProjectType projectType) {
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        final ProjectTypeDefinition definition = dtoFactory.createDto(ProjectTypeDefinition.class)
                                                           .withId(projectType.getId())
                                                           .withDisplayName(projectType.getDisplayName())
                                                           .withPrimaryable(projectType.canBePrimary())
                                                           .withMixable(projectType.canBeMixin());

        final List<AttributeDescriptor> typeAttributes = new ArrayList<>();
        for (Attribute attr : projectType.getAttributes()) {
            List<String> valueList = null;
            try {
                if (attr.getValue() != null)
                    valueList = attr.getValue().getList();
            } catch (ValueStorageException ignored) {
            }

            typeAttributes.add(dtoFactory.createDto(AttributeDescriptor.class)
                                         .withName(attr.getName())
                                         .withDescription(attr.getDescription())
                                         .withRequired(attr.isRequired())
                                         .withVariable(attr.isVariable())
                                         .withValues(valueList));
        }
        definition.setAttributeDescriptors(typeAttributes);

        final List<String> parents = projectType.getParents().stream()
                                                .map(ProjectType::getId)
                                                .collect(Collectors.toList());
        definition.setParents(parents);

        return definition;
    }

    public static ProjectTemplateDescriptor toTemplateDescriptor(ProjectTemplateDescription projectTemplate, String projectType) {
        return toTemplateDescriptor(DtoFactory.getInstance(), projectTemplate, projectType);
    }

    private static ProjectTemplateDescriptor toTemplateDescriptor(DtoFactory dtoFactory,
                                                                  ProjectTemplateDescription projectTemplate,
                                                                  String projectType) {
        final SourceStorageDto importSource = dtoFactory.createDto(SourceStorageDto.class)
                                                        .withType(projectTemplate.getImporterType())
                                                        .withLocation(projectTemplate.getLocation())
                                                        .withParameters(projectTemplate.getParameters());
        final ProjectTemplateDescriptor dto = dtoFactory.createDto(ProjectTemplateDescriptor.class)
                                                        .withDisplayName(projectTemplate.getDisplayName())
                                                        .withSource(importSource)
                                                        .withCategory(projectTemplate.getCategory())
                                                        .withProjectType(projectType)
                                                        .withRecipe(projectTemplate.getRecipe())
                                                        .withDescription(projectTemplate.getDescription());
        return dto;
    }

    public static ProjectImporterDescriptor toImporterDescriptor(ProjectImporter importer) {
        return DtoFactory.getInstance().createDto(ProjectImporterDescriptor.class)
                         .withId(importer.getId())
                         .withInternal(importer.isInternal())
                         .withDescription(importer.getDescription() != null ? importer.getDescription() : "description not found")
                         .withCategory(importer.getCategory().getValue());
    }

    public static ItemReference toItemReference(FileEntry file, UriBuilder uriBuilder) throws ServerException {
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

    public static ItemReference toItemReference(FolderEntry folder,
                                                UriBuilder uriBuilder,
                                                ProjectManager projectManager) throws ServerException {
        return DtoFactory.getInstance().createDto(ItemReference.class)
                         .withName(folder.getName())
                         .withPath(folder.getPath())
                         .withType(projectManager.isProjectFolder(folder) ? "project"
                                                                          : projectManager.isModuleFolder(folder) ? "module" : "folder")
                         .withMediaType("text/directory")
                         .withAttributes(folder.getAttributes())
                         .withCreated(folder.getCreated())
                         .withModified(folder.getModified())
                         .withLinks(generateFolderLinks(folder, uriBuilder));
    }

    public static ProjectDescriptor toProjectDescriptor(Project project,
                                                        UriBuilder serviceUriBuilder,
                                                        ProjectTypeRegistry ptRegistry,
                                                        String wsId) throws InvalidValueException {
        final EnvironmentContext environmentContext = EnvironmentContext.getCurrent();
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        final ProjectDescriptor dto = dtoFactory.createDto(ProjectDescriptor.class);
        // Try to provide as much as possible information about project.
        // If get error then save information about error with 'problems' field in ProjectConfig.
        final String wsName = environmentContext.getWorkspaceName();
        final String name = project.getName();
        final String path = project.getPath();

        dto.withWorkspaceId(wsId).withName(name).withPath(path);

        ProjectConfig config = null;
        try {
            config = project.getConfig();
        } catch (ServerException | ValueStorageException | ProjectTypeConstraintException e) {
            dto.getProblems().add(createProjectProblem(dtoFactory, e));
            dto.withType(BaseProjectType.ID);
        }

        if (config != null) {
            dto.withDescription(config.getDescription());
//            dto.withRecipe(config.getSource().getLocation());
            String typeId = config.getType();
            dto.withType(typeId).withTypeName(ptRegistry.getProjectType(typeId).getDisplayName()).withMixins(config.getMixins());

            dto.withAttributes(config.getAttributes());

            List<ModuleConfigDto> modules = config.getModules().stream().map(DtoConverter::toModuleConfigDto).collect(Collectors.toList());

            dto.withModules(modules);
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

        try {
            dto.withContentRoot(project.getContentRoot());
        } catch (ServerException e) {
            dto.getProblems().add(createProjectProblem(dtoFactory, e));
        }

        if (serviceUriBuilder != null) {
            dto.withBaseUrl(serviceUriBuilder.clone().path(ProjectService.class, "getProject").build(wsId, path.substring(1)).toString())
               .withLinks(generateProjectLinks(project, serviceUriBuilder));
            if (wsName != null) {
                dto.withIdeUrl(serviceUriBuilder.clone().replacePath("ws").path(wsName).path(path).build().toString());
            }
        }

        return dto;
    }

    private static ModuleConfigDto toModuleConfigDto(ModuleConfig moduleConfig) {
        List<ModuleConfigDto> modules =
                moduleConfig.getModules().stream().map(DtoConverter::toModuleConfigDto).collect(Collectors.toList());

        return DtoFactory.getInstance()
                         .createDto(ModuleConfigDto.class)
                         .withName(moduleConfig.getName())
                         .withType(moduleConfig.getType())
                         .withPath(moduleConfig.getPath())
                         .withModules(modules)
                         .withAttributes(moduleConfig.getAttributes())
                         .withDescription(moduleConfig.getDescription())
                         .withMixins(moduleConfig.getMixins());
    }

    public static ProjectReference toProjectReference(Project project, UriBuilder uriBuilder) throws InvalidValueException {
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
            dto.withDescription(projectConfig.getDescription()).withType(projectConfig.getType());
        } catch (ServerException | ValueStorageException | ProjectTypeConstraintException e) {
            dto.withType(BaseProjectType.ID).withTypeName("blank");
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

    private static List<Link> generateProjectLinks(Project project, UriBuilder uriBuilder) {
        final List<Link> links = generateFolderLinks(project.getBaseFolder(), uriBuilder);
        final String relPath = project.getPath().substring(1);
        final String workspace = project.getWorkspace();
        links.add(
                LinksHelper.createLink(PUT,
                                       uriBuilder.clone().path(ProjectService.class, "updateProject").build(workspace, relPath).toString(),
                                       APPLICATION_JSON, APPLICATION_JSON, LINK_REL_UPDATE_PROJECT));
        return links;
    }

    private static List<Link> generateFolderLinks(FolderEntry folder, UriBuilder uriBuilder) {
        final List<Link> links = new LinkedList<>();
        final String workspace = folder.getWorkspace();
        final String relPath = folder.getPath().substring(1);
        //String method, String href, String produces, String rel
        links.add(LinksHelper.createLink(GET,
                                         uriBuilder.clone().path(ProjectService.class, "exportZip").build(workspace, relPath).toString(),
                                         ExtMediaType.APPLICATION_ZIP, LINK_REL_EXPORT_ZIP));
        links.add(LinksHelper.createLink(GET,
                                         uriBuilder.clone().path(ProjectService.class, "getChildren").build(workspace, relPath).toString(),
                                         APPLICATION_JSON, LINK_REL_CHILDREN));
        links.add(
                LinksHelper.createLink(GET, uriBuilder.clone().path(ProjectService.class, "getTree").build(workspace, relPath).toString(),
                                       null, APPLICATION_JSON, LINK_REL_TREE));
        links.add(LinksHelper.createLink(GET,
                                         uriBuilder.clone().path(ProjectService.class, "getModules").build(workspace, relPath).toString(),
                                         APPLICATION_JSON, LINK_REL_MODULES));
        links.add(LinksHelper.createLink(DELETE,
                                         uriBuilder.clone().path(ProjectService.class, "delete").build(workspace, relPath).toString(),
                                         LINK_REL_DELETE));
        return links;
    }

    private static List<Link> generateFileLinks(FileEntry file, UriBuilder uriBuilder) throws ServerException {
        final List<Link> links = new LinkedList<>();
        final String workspace = file.getWorkspace();
        final String relPath = file.getPath().substring(1);
        links.add(LinksHelper.createLink(GET, uriBuilder.clone().path(ProjectService.class, "getFile").build(workspace, relPath).toString(),
                                         null, file.getMediaType(), LINK_REL_GET_CONTENT));
        links.add(LinksHelper.createLink(PUT,
                                         uriBuilder.clone().path(ProjectService.class, "updateFile").build(workspace, relPath).toString(),
                                         MediaType.WILDCARD, null, LINK_REL_UPDATE_CONTENT));
        links.add(LinksHelper.createLink(DELETE,
                                         uriBuilder.clone().path(ProjectService.class, "delete").build(workspace, relPath).toString(),
                                         LINK_REL_DELETE));
        return links;
    }

    private static ProjectProblem createProjectProblem(DtoFactory dtoFactory, ApiException error) {
        return dtoFactory.createDto(ProjectProblem.class).withCode(1).withMessage(error.getMessage());
    }
}
