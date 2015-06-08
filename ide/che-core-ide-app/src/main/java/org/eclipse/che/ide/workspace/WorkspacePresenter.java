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
package org.eclipse.che.ide.workspace;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.eclipse.che.ide.api.constraints.Constraints;
import org.eclipse.che.ide.api.mvp.Presenter;
import org.eclipse.che.ide.api.parts.PartPresenter;
import org.eclipse.che.ide.api.parts.PartStack;
import org.eclipse.che.ide.api.parts.PartStackType;
import org.eclipse.che.ide.api.parts.WorkspaceAgent;
import org.eclipse.che.ide.menu.MainMenuPresenter;
import org.eclipse.che.ide.statuspanel.StatusPanelGroupPresenter;
import org.eclipse.che.ide.ui.toolbar.MainToolbar;
import org.eclipse.che.ide.ui.toolbar.ToolbarPresenter;

/**
 * Root Presenter that implements Workspace logic. Descendant Presenters are injected
 * via constructor and exposed to corresponding UI containers. It contains Menu,
 * Toolbar and WorkBench Presenter to expose their views into corresponding places and to
 * maintain their interactions.
 *
 * @author Nikolay Zamosenchuk
 */
@Singleton
public class WorkspacePresenter implements Presenter, WorkspaceView.ActionDelegate, WorkspaceAgent {

    private final WorkspaceView             view;
    private final MainMenuPresenter         mainMenu;
    private final StatusPanelGroupPresenter bottomMenu;
    private final ToolbarPresenter          toolbarPresenter;
    private       WorkBenchPresenter        workBenchPresenter;

    /**
     * Instantiates Presenter.
     *
     * @param view
     * @param mainMenu
     * @param bottomMenu
     * @param toolbarPresenter
     * @param genericPerspectiveProvider
     */
    @Inject
    protected WorkspacePresenter(WorkspaceView view,
                                 MainMenuPresenter mainMenu,
                                 StatusPanelGroupPresenter bottomMenu,
                                 @MainToolbar ToolbarPresenter toolbarPresenter,
                                 Provider<WorkBenchPresenter> genericPerspectiveProvider) {
        super();
        this.view = view;
        this.view.setDelegate(this);
        this.toolbarPresenter = toolbarPresenter;
        this.mainMenu = mainMenu;
        this.bottomMenu = bottomMenu;
        this.workBenchPresenter = genericPerspectiveProvider.get();
    }

    /** {@inheritDoc} */
    @Override
    public void go(AcceptsOneWidget container) {
        mainMenu.go(view.getMenuPanel());
        toolbarPresenter.go(view.getToolbarPanel());
        workBenchPresenter.go(view.getPerspectivePanel());
        bottomMenu.go(view.getStatusPanel());
        container.setWidget(view);
    }

    /** {@inheritDoc} */
    @Override
    public void setActivePart(PartPresenter part) {
        workBenchPresenter.setActivePart(part);
    }

    /** {@inheritDoc} */
    @Override
    public void openPart(PartPresenter part, PartStackType type) {
        openPart(part, type, null);
    }

    /** {@inheritDoc} */
    @Override
    public void openPart(PartPresenter part, PartStackType type, Constraints constraint) {
        workBenchPresenter.openPart(part, type, constraint);
    }

    /** {@inheritDoc} */
    @Override
    public void hidePart(PartPresenter part) {
        workBenchPresenter.hidePart(part);
    }

    /** {@inheritDoc} */
    @Override
    public void removePart(PartPresenter part) {
        workBenchPresenter.removePart(part);
    }

    /**
     * Retrieves the instance of the {@link org.eclipse.che.ide.api.parts.PartStack} for given {@link PartStackType}
     *
     * @param type one of the enumerated type {@link org.eclipse.che.ide.api.parts.PartStackType}
     * @return the part stack found, else null
     */
    public PartStack getPartStack(PartStackType type) {
        return workBenchPresenter.getPartStack(type);
    }

    /** {@inheritDoc} */
    @Override
    public void onUpdateClicked() {
        final String host = Window.Location.getParameter("h");
        final String port = Window.Location.getParameter("p");
        updateExtension(host, port);
    }

    /** Update already launched Codenvy extension. */
    private static native void updateExtension(String host, String port) /*-{
        $wnd.__gwt_bookmarklet_params = {server_url: 'http://' + host + ':' + port + '/', module_name: '_app'};
        var s = $doc.createElement('script');
        s.src = 'http://' + host + ':' + port + '/dev_mode_on.js';
        void($doc.getElementsByTagName('head')[0].appendChild(s));
    }-*/;

    /**
     * Sets whether 'Update extension' button is visible.
     *
     * @param visible
     *         <code>true</code> to show the button, <code>false</code> to hide it
     */
    public void setUpdateButtonVisibility(boolean visible) {
        view.setUpdateButtonVisibility(visible);
    }

    /**
     * Shows or hides status panel
     *
     * @param visible
     *         <code>true</code> to show the panel, <code>false</code> to hide it
     */
    public void setStatusPanelVisible(boolean visible) {
        view.setStatusPanelVisible(visible);
    }
}
