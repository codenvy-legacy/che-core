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
package org.eclipse.che.ide.part.editor;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The class contains business logic which allows define project to which file applies. It is necessary when we open files from
 * different projects.
 *
 * @author Dmitry Shnurenko
 */
@Singleton
public class FileMatcher {

    private final Map<String, ProjectDescriptor> matchedFiles;
    private final AppContext                     appContext;
    private final List<ProjectChangedListener>   listeners;

    @Inject
    public FileMatcher(AppContext appContext) {
        this.matchedFiles = new HashMap<>();
        this.listeners = new ArrayList<>();

        this.appContext = appContext;
    }

    /**
     * Matches current file to special project using path to file.
     *
     * @param pathToFile
     *         path to file which will be matched
     * @param currentProject
     *         project which will be matched to file
     */
    public void matchFileToProject(@Nonnull String pathToFile, @Nonnull ProjectDescriptor currentProject) {
        matchedFiles.put(pathToFile, currentProject);
    }

    /**
     * Sets special project descriptor for matched file using path to file.
     *
     * @param pathToFile
     *         path to file for which project will be set
     */
    public void setActualProjectForFile(@Nonnull String pathToFile) {
        ProjectDescriptor descriptor = matchedFiles.get(pathToFile);

        CurrentProject currentProject = appContext.getCurrentProject();

        if (descriptor == null || currentProject == null) {
            return;
        }

        currentProject.setProjectDescription(descriptor);

        notifyListeners(descriptor);
    }

    /**
     * Removes match for current file.
     *
     * @param pathToFile
     *         path to file for which match will be removed
     */
    public void removeMatch(@Nonnull String pathToFile) {
        matchedFiles.remove(pathToFile);
    }

    /**
     * Adds special listener which will action when we set descriptor for matched file.
     *
     * @param projectChangedListener
     *         listener which will be added
     */
    public void addListener(@Nonnull ProjectChangedListener projectChangedListener) {
        listeners.add(projectChangedListener);
    }

    /**
     * Notifies all listeners that project is changed.
     *
     * @param descriptor
     *         project which will be set
     */
    public void notifyListeners(@Nonnull ProjectDescriptor descriptor) {
        for (ProjectChangedListener listener : listeners) {
            listener.onProjectChanged(descriptor);
        }
    }

    public interface ProjectChangedListener {
        /**
         * Performs some actions when project is changed.
         *
         * @param descriptor
         *         project which will be set
         */
        void onProjectChanged(@Nonnull ProjectDescriptor descriptor);
    }
}
