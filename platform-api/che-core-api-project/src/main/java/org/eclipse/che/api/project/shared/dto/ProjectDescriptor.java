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
package org.eclipse.che.api.project.shared.dto;

import org.eclipse.che.api.core.rest.shared.dto.Hyperlinks;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.dto.shared.DTO;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.util.List;
import java.util.Map;

//TODO remove workspace name from descriptor

/**
 * Data transfer object (DTO) for org.eclipse.che.api.project.shared.ProjectDescription.
 *
 * @author andrew00x
 */
@DTO
public interface ProjectDescriptor extends Hyperlinks {
    /** Gets name of project. */
    @ApiModelProperty(value = "Project name", position = 1)
    String getName();

    /** Sets name of project. */
    void setName(String name);

    ProjectDescriptor withName(String name);

    //

    /** Gets path of project. */
    @ApiModelProperty(value = "Project path", position = 2)
    String getPath();

    /** Sets path of project. */
    void setPath(String path);

    ProjectDescriptor withPath(String path);

    //

    /** Gets unique id of type of project. */
    @ApiModelProperty(value = "Project type ID", position = 3)
    String getType();

    /** Sets unique id of type of project. */
    void setType(String type);

    ProjectDescriptor withType(String type);

    //

    /** Gets display name of project type. */
    @ApiModelProperty(value = "Name of a project type", position = 4)
    String getTypeName();

    /** Sets display name of project type. */
    void setTypeName(String name);

    ProjectDescriptor withTypeName(String name);


    @ApiModelProperty(value = "Mixins of current project", position = 12)
    List<String> getMixins();

    /** Sets permissions of current user on this project. */
    void setMixins(List<String> mixins);

    ProjectDescriptor withMixins(List<String> mixins);



    /** Gets id of workspace which projects belongs to. */
    @ApiModelProperty(value = "Workspace ID", position = 5)
    String getWorkspaceId();

    /** Sets id of workspace which projects belongs to. */
    void setWorkspaceId(String workspaceId);

    ProjectDescriptor withWorkspaceId(String workspaceId);

    //

    /** Gets name of workspace this project belongs to. */
    @ApiModelProperty(value = "Name of workspace which the project belongs to", position = 6)
    String getWorkspaceName();

    /** Sets name of workspace this project belongs to. */
    void setWorkspaceName(String name);

    ProjectDescriptor withWorkspaceName(String name);

    //

    /** Gets attributes of this project. */
    @ApiModelProperty(value = "Project attributes", position = 11)
    Map<String, List<String>> getAttributes();

    /** Sets attributes of this project. */
    void setAttributes(Map<String, List<String>> attributes);

    ProjectDescriptor withAttributes(Map<String, List<String>> attributes);

    //

    /** Gets project visibility, e.g. private or public. */
    @ApiModelProperty(value = "Project privacy. Projects are public by default", allowableValues = "private,public", position = 7)
    String getVisibility();

    /** Sets project visibility, e.g. private or public. */
    void setVisibility(String visibility);

    ProjectDescriptor withVisibility(String visibility);

    //

    /** Gets optional description of project. */
    @ApiModelProperty(value = "Project description", position = 8)
    String getDescription();

    /** Sets optional description of project. */
    void setDescription(String description);

    ProjectDescriptor withDescription(String description);

    //

    /** Gets creation date of project in unix format. */
    @ApiModelProperty(value = "Creation date in UNIX Epoch format", dataType = "long", position = 9)
    long getCreationDate();

    /** Sets creation date of project in unix format. */
    void setCreationDate(long date);

    ProjectDescriptor withCreationDate(long date);

    //

    /** Gets most recent modification date of project in unix format. */
    @ApiModelProperty(value = "Most recent modification date in UNIX Epoch format", dataType = "long", position = 10)
    long getModificationDate();

    /** Sets most recent modification date of project in unix format. */
    void setModificationDate(long date);

    ProjectDescriptor withModificationDate(long date);

    //

    /** Gets permissions of current user on this project. Current user is user who retrieved this object. */
    @ApiModelProperty(value = "Permissions of current user on the project", position = 12)
    List<String> getPermissions();

    /** Sets permissions of current user on this project. */
    void setPermissions(List<String> permissions);

    ProjectDescriptor withPermissions(List<String> permissions);

    //

    /** Gets builder configurations. */
    @ApiModelProperty(value = "Builders configuration for the project", position = 13)
    BuildersDescriptor getBuilders();

    /** Sets builder configurations. */
    void setBuilders(BuildersDescriptor builders);

    ProjectDescriptor withBuilders(BuildersDescriptor builders);

    //

    /** Gets runner configurations. */
    @ApiModelProperty(value = "Runners configuration for the project", position = 14)
    RunnersDescriptor getRunners();

    /** Sets runner configurations. */
    void setRunners(RunnersDescriptor runners);

    ProjectDescriptor withRunners(RunnersDescriptor runners);

    //

    /** Gets URL for getting this description of project. */
    @ApiModelProperty(value = "Project URL", position = 15)
    String getBaseUrl();

    /** Sets URL for getting this description of project. */
    void setBaseUrl(String url);

    ProjectDescriptor withBaseUrl(String url);

    //

    /** Gets URL for opening project in Codenvy IDE. */
    @ApiModelProperty(value = "URL for opening project in Codenvy IDE", position = 16)
    String getIdeUrl();

    /** Sets URL for opening project in Codenvy IDE. */
    void setIdeUrl(String url);

    ProjectDescriptor withIdeUrl(String url);

    //

    /** Provides information about project errors. If project doesn't have any error this field is empty. */
    @ApiModelProperty(value = "Optional information about project errors. If project doesn't have any error this field is empty",
                      position = 17)
    List<ProjectProblem> getProblems();

    /** @see #getProblems */
    void setProblems(List<ProjectProblem> problems);

    ProjectDescriptor withProblems(List<ProjectProblem> problems);

    //

    ProjectDescriptor withLinks(List<Link> links);
}
