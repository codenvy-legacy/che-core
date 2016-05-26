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
package org.eclipse.che.api.project.shared.dto;

import org.eclipse.che.dto.shared.DTO;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.util.List;

/**
 * Reference to the IDE Project.
 *
 * @author andrew00x
 */
@DTO
@ApiModel(description = "Short information about project, it doesn't contain any project attributes.")
public interface ProjectReference {
    /** Gets name of project. */
    @ApiModelProperty(value = "Name of the project", position = 1)
    String getName();

    /** Sets name of project. */
    void setName(String name);

    ProjectReference withName(String name);

    //

    /** Gets path of project. */
    @ApiModelProperty(value = "Full path of the project", position = 2)
    String getPath();

    /** Sets path of project. */
    void setPath(String path);

    ProjectReference withPath(String path);

    //

    /** Gets unique ID of type of project. */
    @ApiModelProperty(value = "Unique ID of project's type", position = 3)
    String getType();

    /** Sets unique ID of type of project. */
    void setType(String type);

    ProjectReference withType(String id);

    //

    /** Gets display name of type of project. */
    @ApiModelProperty(value = "Display name of project's type", position = 4)
    String getTypeName();

    /** Sets display name of type of project. */
    void setTypeName(String typeName);

    ProjectReference withTypeName(String typeName);

    //

    /** Gets URL for getting detailed information about project. */
    @ApiModelProperty(value = "URL for getting detailed information about the project", position = 5)
    String getUrl();

    /** Sets URL for getting detailed information about project. */
    void setUrl(String url);

    ProjectReference withUrl(String url);

    //

    /** Gets URL for opening project in Codenvy IDE. */
    @ApiModelProperty(value = "URL for opening project in Codenvy IDE", position = 6)
    String getIdeUrl();

    /** Sets URL for opening project in Codenvy IDE. */
    void setIdeUrl(String url);

    ProjectReference withIdeUrl(String url);

    //

    /** Gets id of workspace this project belongs to. */
    @ApiModelProperty(value = "ID of workspace which the project belongs to", position = 7)
    String getWorkspaceId();

    /** Sets id of workspace this project belongs to. */
    void setWorkspaceId(String id);

    ProjectReference withWorkspaceId(String id);

    //

    /** Gets name of workspace this project belongs to. */
    @ApiModelProperty(value = "Name of workspace which the project belongs to", position = 8)
    String getWorkspaceName();

    /** Sets name of workspace this project belongs to. */
    void setWorkspaceName(String name);

    ProjectReference withWorkspaceName(String name);

    //

    /** Gets project visibility, e.g. private or public. */
    @ApiModelProperty(value = "Visibility of the project", allowableValues = "public,private", position = 9)
    String getVisibility();

    /** Set project visibility, e.g. private or public. */
    void setVisibility(String visibility);

    ProjectReference withVisibility(String visibility);

    //

    /** Gets creation date of project. */
    @ApiModelProperty(value = "Time that the project was created or -1 if creation time in unknown", dataType = "long", position = 10)
    long getCreationDate();

    /** Sets creation date of project. */
    void setCreationDate(long date);

    ProjectReference withCreationDate(long date);

    //

    /** Gets modification date of project. */
    @ApiModelProperty(value = "Time that the project was last modified or -1 if modification time date in unknown",
                      dataType = "long", position = 11)
    long getModificationDate();

    /** Sets modification date of project. */
    void setModificationDate(long date);

    ProjectReference withModificationDate(long date);

    //

    /** Gets optional description of project. */
    @ApiModelProperty(value = "Optional description of the project", position = 12)
    String getDescription();

    /** Sets optional description of project. */
    void setDescription(String description);

    ProjectReference withDescription(String description);

    //

    /** Provides information about project errors. If project doesn't have any error this field is empty. */
    @ApiModelProperty(value = "Optional information about project errors. If project doesn't have any error this field is empty",
                      position = 13)
    List<ProjectProblem> getProblems();

    /** @see #getProblems */
    void setProblems(List<ProjectProblem> problems);

    ProjectReference withProblems(List<ProjectProblem> problems);
}
