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

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.commons.json.JsonHelper;
import org.eclipse.che.commons.json.JsonParseException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Part of project meta-data that is stored in file &lt;project folder&gt;/.codenvy/project.json.
 *
 * @author andrew00x
 */
public class ProjectJson {

    /**
     * Checks whether the Project's meta information is readable
     *
     * @param project
     *         project to check
     * @return true if project meta-information is readable (it exists, there are appropriate permissions etc)
     * otherwise returns false
     */
    public static boolean isReadable(Project project) {
        final VirtualFileEntry projectFile;
        try {
            projectFile = project.getBaseFolder().getChild(Constants.CODENVY_PROJECT_FILE_RELATIVE_PATH);
            if (projectFile == null || !projectFile.isFile()) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public static ProjectJson load(Project project) throws ServerException {
        final VirtualFileEntry projectFile;
        try {
            projectFile = project.getBaseFolder().getChild(Constants.CODENVY_PROJECT_FILE_RELATIVE_PATH);
        } catch (ForbiddenException e) {
            // If have access to the project then must have access to its meta-information. If don't have access then treat that as server error.
            throw new ServerException(e.getServiceError());
        }


        if (projectFile == null || !projectFile.isFile()) {
            return new ProjectJson();
        }
        try (InputStream inputStream = ((FileEntry)projectFile).getInputStream()) {

            ProjectJson json = load(inputStream);

            // possible if no content
            if(json == null)
                json = new ProjectJson();

            return json;
        } catch (IOException e) {
            throw new ServerException(e.getMessage(), e);
        }
    }

    public static ProjectJson load(InputStream inputStream) throws IOException {
        try {

            return JsonHelper.fromJson(inputStream, ProjectJson.class, null);
        } catch (JsonParseException e) {
            throw new IOException("Unable to parse the project's property file. " +
                                  "Check the project.json file for corruption or modification. Consider reloading the project. " +
                                  e.getMessage());
        }
    }

    public void save(Project project) throws ServerException {
        try {
            final FolderEntry baseFolder = project.getBaseFolder();
            VirtualFileEntry projectFile = baseFolder.getChild(Constants.CODENVY_PROJECT_FILE_RELATIVE_PATH);
            if (projectFile != null) {
                if (!projectFile.isFile()) {
                    throw new ServerException(String.format(
                            "Unable to save the project's attributes to the file system. Path %s/%s exists but is not a file.",
                            baseFolder.getPath(), Constants.CODENVY_PROJECT_FILE_RELATIVE_PATH));
                }
                ((FileEntry)projectFile).updateContent(JsonHelper.toJson(this).getBytes(), null);
            } else {
                VirtualFileEntry codenvyDir = baseFolder.getChild(Constants.CODENVY_DIR);
                if (codenvyDir == null) {
                    try {
                        codenvyDir = baseFolder.createFolder(Constants.CODENVY_DIR);
                    } catch (ConflictException e) {
                        // Already checked existence of folder ".codenvy".
                        throw new ServerException(e.getServiceError());
                    }
                } else if (!codenvyDir.isFolder()) {
                    throw new ServerException(String.format(
                            "Unable to save the project's attributes to the file system. Path %s/%s exists but is not a folder.",
                            baseFolder.getPath(), Constants.CODENVY_DIR));
                }
                try {
                    ((FolderEntry)codenvyDir).createFile(Constants.CODENVY_PROJECT_FILE, JsonHelper.toJson(this).getBytes(), null);
                } catch (ConflictException e) {
                    // Already checked existence of file ".codenvy/project.json".
                    throw new ServerException(e.getServiceError());
                }
            }
        } catch (ForbiddenException e) {
            // If have access to the project then must have access to its meta-information. If don't have access then treat that as server error.
            throw new ServerException(e.getServiceError());
        }
    }

    private String                    type;
//    private Builders builders;
//    private Runners runners;
    private String                    description;
    private Map<String, List<String>> attributes;
    private List <String> mixinTypes;

    public ProjectJson() {
    }

    public ProjectJson(String type, Map<String, List<String>> attributes, String description) {
        this.type = type;
//        this.builders = builders;
//        this.runners = runners;
        this.description = description;
        this.attributes = attributes;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ProjectJson withType(String type) {
        this.type = type;
        return this;
    }

//    public Builders getBuilders() {
//        return builders;
//    }
//
//    public ProjectJson withBuilders(Builders builders) {
//        this.builders = builders;
//        return this;
//    }
//
//    public void setBuilders(Builders builders) {
//        this.builders = builders;
//    }
//
//    public Runners getRunners() {
//        return runners;
//    }
//
//    public ProjectJson withRunners(Runners runners) {
//        this.runners = runners;
//        return this;
//    }
//
//    public void setRunners(Runners runners) {
//        this.runners = runners;
//    }

    public Map<String, List<String>> getAttributes() {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        return attributes;
    }

    public void setAttributes(Map<String, List<String>> attributes) {
        this.attributes = attributes;
    }

    public ProjectJson withAttributes(Map<String, List<String>> attributes) {
        this.attributes = attributes;
        return this;
    }

    public String getAttributeValue(String name) {
        if (attributes != null) {
            final List<String> value = attributes.get(name);
            if (value != null && !value.isEmpty()) {
                return value.get(0);
            }
        }
        return null;
    }

    public List<String> getAttributeValues(String name) {
        if (attributes != null) {
            final List<String> value = attributes.get(name);
            if (value != null) {
                return new ArrayList<>(value);
            }
        }
        return null;
    }

    public void removeAttribute(String name) {
        if (attributes != null) {
            attributes.remove(name);
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ProjectJson withDescription(String description) {
        this.description = description;
        return this;
    }

    public List<String> getMixinTypes() {
        return mixinTypes;
    }

    public void setMixinTypes(List <String> mixinTypes) {
        this.mixinTypes = mixinTypes;
    }

    public ProjectJson withMixinTypes(List <String> mixinTypes) {
        this.mixinTypes = mixinTypes;
        return this;
    }
}
