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
package org.eclipse.che.ide.statepersistance;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.actions.ShowHiddenFilesAction;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.part.explorer.project.ProjectExplorerPresenter;
import org.eclipse.che.ide.statepersistance.dto.ActionDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.eclipse.che.ide.actions.ShowHiddenFilesAction.SHOW_HIDDEN_FILES_PARAM_ID;

/**
 * @author Andrienko Alexander
 */
@Singleton
public class ShowHiddenFilesPersistenceComponent implements PersistenceComponent {

    private final AppContext               appContext;
    private final ActionManager            actionManager;
    private final ShowHiddenFilesAction    showHiddenFilesAction;
    private final DtoFactory               dtoFactory;
    private final ProjectExplorerPresenter projectExplorer;

    @Inject
    public ShowHiddenFilesPersistenceComponent(AppContext appContext,
                                               ActionManager actionManager,
                                               ShowHiddenFilesAction showHiddenFilesAction,
                                               DtoFactory dtoFactory,
                                               ProjectExplorerPresenter projectExplorer) {
        this.appContext = appContext;
        this.actionManager = actionManager;
        this.showHiddenFilesAction = showHiddenFilesAction;
        this.dtoFactory = dtoFactory;
        this.projectExplorer = projectExplorer;
    }

    @Override
    public List<ActionDescriptor> getActions(String projectPath) {
        List<ActionDescriptor> actions = new ArrayList<>();

        CurrentProject currentProject = appContext.getCurrentProject();

        if (currentProject == null) {
            return actions;
        }

        String actionId = actionManager.getId(showHiddenFilesAction);

        boolean isShowHiddenFiles = projectExplorer.isShowHiddenFiles();

        actions.add(dtoFactory.createDto(ActionDescriptor.class)
                              .withId(actionId)
                              .withParameters(Collections.singletonMap(SHOW_HIDDEN_FILES_PARAM_ID, String.valueOf(isShowHiddenFiles))));

        return actions;
    }
}
