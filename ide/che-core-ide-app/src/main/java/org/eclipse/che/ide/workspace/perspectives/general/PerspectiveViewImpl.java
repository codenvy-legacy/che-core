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
package org.eclipse.che.ide.workspace.perspectives.general;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.eclipse.che.ide.api.parts.WorkBenchView;
import org.eclipse.che.ide.workspace.WorkBenchResources;

/**
 * General-purpose Perspective View
 *
 * @author Nikolay Zamosenchuk
 * @author Dmitry Shnurenko
 */
public class PerspectiveViewImpl extends LayoutPanel implements WorkBenchView<WorkBenchView.ActionDelegate> {

    interface PerspectiveViewImplUiBinder extends UiBinder<Widget, PerspectiveViewImpl> {
    }

    private static final PerspectiveViewImplUiBinder UI_BINDER = GWT.create(PerspectiveViewImplUiBinder.class);

    @UiField(provided = true)
    SplitLayoutPanel splitPanel = new SplitLayoutPanel(3);

    @UiField
    ScrollPanel editorPanel;
    @UiField
    SimplePanel navPanel;
    @UiField
    SimplePanel infoPanel;

    @UiField
    SimplePanel toolPanel;
    @UiField
    FlowPanel   rightPanel;
    @UiField
    FlowPanel   leftPanel;
    @UiField
    FlowPanel   bottomPanel;

    @UiField(provided = true)
    final WorkBenchResources resources;

    @Inject
    public PerspectiveViewImpl(WorkBenchResources resources) {
        this.resources = resources;
        resources.workBenchCss().ensureInjected();
        add(UI_BINDER.createAndBindUi(this));
    }

    /** {@inheritDoc} */
    @Override
    public void setDelegate(ActionDelegate delegate) {
        // do nothing
    }

    /** {@inheritDoc} */
    @Override
    public SimplePanel getEditorPanel() {
        return editorPanel;
    }

    /** {@inheritDoc} */
    @Override
    public SimplePanel getNavigationPanel() {
        return navPanel;
    }

    /** {@inheritDoc} */
    @Override
    public SimplePanel getInformationPanel() {
        return infoPanel;
    }

    /** {@inheritDoc} */
    @Override
    public SimplePanel getToolPanel() {
        return toolPanel;
    }

    /** Returns split panel. */
    public SplitLayoutPanel getSplitPanel() {
        return splitPanel;
    }

    /** Returns right panel.Outline tab is located on this panel. */
    public FlowPanel getRightPanel() {
        return rightPanel;
    }

    /** Returns left panel.Project explorer tab is located on this panel. */
    public FlowPanel getLeftPanel() {
        return leftPanel;
    }

    /**
     * Returns bottom panel. This panel can contains different tabs. When perspective is project, this panel contains Events and
     * Outputs tabs.
     */
    public FlowPanel getBottomPanel() {
        return bottomPanel;
    }

    /** {@inheritDoc} */
    @Override
    public void onResize() {
        editorPanel.onResize();
        super.onResize();
    }

}
