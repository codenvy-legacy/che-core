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

import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.workspace.perspectives.general.PerspectiveType;

import static org.eclipse.che.ide.workspace.perspectives.general.Perspective.Type.MACHINE;
import static org.eclipse.che.ide.workspace.perspectives.general.Perspective.Type.PROJECT;

/**
 * Special action which allows change state of perspective.
 *
 * @author Dmitry Shnurenko
 */
public class ChangePerspectiveAction extends Action {

    private final PerspectiveType perspectiveType;

    private boolean isProject;

    @Inject
    public ChangePerspectiveAction(PerspectiveType perspectiveType, Resources resources) {
        //TODO need change icon
        super("Change perspective", "Change perspective", null, resources.closeProject());

        this.perspectiveType = perspectiveType;

        isProject = true;
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent e) {
        isProject = !isProject;

        perspectiveType.setType(isProject ? PROJECT : MACHINE);
    }
}
