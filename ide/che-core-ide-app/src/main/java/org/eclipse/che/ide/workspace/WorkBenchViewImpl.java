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
package org.eclipse.che.ide.workspace;

import org.eclipse.che.ide.api.parts.WorkBenchView;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * General-purpose Perspective View
 *
 * @author Nikolay Zamosenchuk
 */
@Singleton
public class WorkBenchViewImpl extends LayoutPanel implements WorkBenchView<WorkBenchView.ActionDelegate> {

    interface WorkBenchViewImplUiBinder extends UiBinder<Widget, WorkBenchViewImpl> {
    }

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
    final WorkBenchResources res;

    @Inject
    public WorkBenchViewImpl(WorkBenchResources resources,
                             WorkBenchViewImplUiBinder uiBinder) {
        this.res = resources;
        resources.workBenchCss().ensureInjected();
        add(uiBinder.createAndBindUi(this));
    }

    /** {@inheritDoc} */
    @Override
    public void setDelegate(ActionDelegate delegate) {
        // do nothing
    }

    /** {@inheritDoc} */
    @Override
    public AcceptsOneWidget getEditorPanel() {
        return editorPanel;
    }

    /** {@inheritDoc} */
    @Override
    public AcceptsOneWidget getNavigationPanel() {
        return navPanel;
    }

    /** {@inheritDoc} */
    @Override
    public AcceptsOneWidget getInformationPanel() {
        return infoPanel;
    }

    /** {@inheritDoc} */
    @Override
    public AcceptsOneWidget getToolPanel() {
        return toolPanel;
    }

    /** {@inheritDoc} */
    @Override
    public void onResize() {
        editorPanel.onResize();
        super.onResize();
    }

}
