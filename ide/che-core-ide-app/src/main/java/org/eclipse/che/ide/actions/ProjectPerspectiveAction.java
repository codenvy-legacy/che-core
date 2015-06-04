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
package org.eclipse.che.ide.actions;

import com.google.inject.Inject;

import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.workspace.perspectives.general.PerspectiveManager;

import static org.eclipse.che.ide.workspace.perspectives.project.ProjectPerspective.PROJECT_PERSPECTIVE_ID;

/**
 * Special action which allows set project perspective.
 *
 * @author Dmitry Shnurenko
 */
public class ProjectPerspectiveAction extends Action {

    private final PerspectiveManager perspectiveManager;

    @Inject
    public ProjectPerspectiveAction(PerspectiveManager perspectiveManager, Resources resources, CoreLocalizationConstant locale) {
        //TODO need change icon
        super(locale.perspectiveActionDescription(), locale.perspectiveActionTooltip(), null, resources.closeProject());

        this.perspectiveManager = perspectiveManager;
    }

    /** {@inheritDoc} */
    @Override
    public void update(ActionEvent event) {
        String currentPerspectiveId = perspectiveManager.getPerspectiveId();

        boolean isProjectPerspective = currentPerspectiveId.equals(PROJECT_PERSPECTIVE_ID);

        event.getPresentation().setEnabled(!isProjectPerspective);
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent event) {
        event.getPresentation().setEnabled(false);

        perspectiveManager.setPerspectiveId(PROJECT_PERSPECTIVE_ID);
    }
}
