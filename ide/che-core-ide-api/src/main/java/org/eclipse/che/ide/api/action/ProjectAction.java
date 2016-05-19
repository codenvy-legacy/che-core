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
package org.eclipse.che.ide.api.action;

import org.eclipse.che.ide.api.app.AppContext;
import com.google.inject.Inject;

import org.vectomatic.dom.svg.ui.SVGResource;

/**
 * Action that make sense only in project scope.
 * This action disables if current project null or user has read only rights.
 *
 * @author Evgen Vidolob
 */
public abstract class ProjectAction extends Action {

    protected AppContext appContext;

    protected ProjectAction() {
    }

    protected ProjectAction(String text) {
        super(text);
    }

    protected ProjectAction(String text, String description) {
        super(text, description);
    }

    protected ProjectAction(String text, String description, SVGResource svgIcon) {
        super(text, description, null, svgIcon);
    }

    @Override
    public final void update(ActionEvent e) {
        if (appContext.getCurrentProject() == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        if (appContext.getCurrentProject().isReadOnly()) {
            e.getPresentation().setVisible(true);
            e.getPresentation().setEnabled(false);
            return;
        }
        updateProjectAction(e);
    }

    protected abstract void updateProjectAction(ActionEvent e);

    @Inject
    private void injectAppContext(AppContext appContext) {
        this.appContext = appContext;
    }
}
