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
package org.eclipse.che.ide.workspace.perspectives.project;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.parts.EditorPartStack;
import org.eclipse.che.ide.api.parts.OutlinePart;
import org.eclipse.che.ide.api.parts.PartStack;
import org.eclipse.che.ide.api.parts.ProjectExplorerPart;
import org.eclipse.che.ide.workspace.PartStackPresenterFactory;
import org.eclipse.che.ide.workspace.PartStackViewFactory;
import org.eclipse.che.ide.workspace.WorkBenchControllerFactory;
import org.eclipse.che.ide.workspace.perspectives.general.AbstractPerspective;
import org.eclipse.che.ide.workspace.perspectives.general.PerspectiveViewImpl;

import javax.validation.constraints.NotNull;

import static org.eclipse.che.ide.api.parts.PartStackType.EDITING;
import static org.eclipse.che.ide.api.parts.PartStackType.INFORMATION;
import static org.eclipse.che.ide.api.parts.PartStackType.NAVIGATION;
import static org.eclipse.che.ide.api.parts.PartStackType.TOOLING;


/**
 * General-purpose, displaying all the PartStacks in a default manner:
 * Navigation at the left side;
 * Tooling at the right side;
 * Information at the bottom of the page;
 * Editors in the center.
 *
 * @author Nikolay Zamosenchuk
 * @author Dmitry Shnurenko
 */
@Singleton
public class ProjectPerspective extends AbstractPerspective {

    public final static String PROJECT_PERSPECTIVE_ID = "Project Perspective";

    @Inject
    public ProjectPerspective(PerspectiveViewImpl view,
                              EditorPartStack editorPartStackPresenter,
                              PartStackPresenterFactory stackPresenterFactory,
                              PartStackViewFactory partViewFactory,
                              WorkBenchControllerFactory controllerFactory,
                              OutlinePart outlinePart,
                              ProjectExplorerPart projectExplorerPart,
                              NotificationManager notificationManager) {
        super(PROJECT_PERSPECTIVE_ID, view, stackPresenterFactory, partViewFactory, controllerFactory);

        notificationManager.addRule(PROJECT_PERSPECTIVE_ID);

        partStacks.put(EDITING, editorPartStackPresenter);

        addPart(outlinePart, TOOLING);
        addPart(notificationManager, INFORMATION);
        addPart(projectExplorerPart, NAVIGATION);

        setActivePart(projectExplorerPart);
    }

    /** {@inheritDoc} */
    @Override
    public void go(@NotNull AcceptsOneWidget container) {
        PartStack navigatorPanel = getPartStack(NAVIGATION);
        PartStack editorPanel = getPartStack(EDITING);
        PartStack toolPanel = getPartStack(TOOLING);
        PartStack infoPanel = getPartStack(INFORMATION);

        if (navigatorPanel == null || editorPanel == null || toolPanel == null || infoPanel == null) {
            return;
        }

        navigatorPanel.go(view.getNavigationPanel());
        editorPanel.go(view.getEditorPanel());
        toolPanel.go(view.getToolPanel());
        infoPanel.go(view.getInformationPanel());

        openActivePart(INFORMATION);

        container.setWidget(view);
    }
}
