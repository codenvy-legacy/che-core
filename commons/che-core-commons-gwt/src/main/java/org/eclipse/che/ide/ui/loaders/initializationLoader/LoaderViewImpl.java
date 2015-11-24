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
package org.eclipse.che.ide.ui.loaders.initializationLoader;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.vectomatic.dom.svg.ui.SVGResource;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link LoaderView}.
 *
 * @author Roman Nikitenko
 */
@Singleton
public class LoaderViewImpl implements LoaderView {

    private static final String LOADING = "LOADING:";

    @UiField(provided = true)
    Resources resources;
    @UiField
    FlowPanel iconPanel;
    @UiField
    FlowPanel expandHolder;
    @UiField
    FlowPanel operations;
    @UiField
    FlowPanel currentOperation;
    @UiField
    FlowPanel operationPanel;
    @UiField
    Label     status;

    DivElement progressBar;
    FlowPanel  rootElement;

    private List<HTML>     components;
    private ActionDelegate delegate;
    private LoaderCss      styles;

    @Inject
    public LoaderViewImpl(LoaderViewImplUiBinder uiBinder,
                          Resources resources) {
        this.resources = resources;

        styles = resources.css();
        styles.ensureInjected();

        rootElement = uiBinder.createAndBindUi(this);

        progressBar = Document.get().createDivElement();
        operationPanel.getElement().appendChild(progressBar);
        operationPanel.addDomHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                delegate.onExpanderClicked();
            }
        }, ClickEvent.getType());
        operations.setVisible(false);

        DivElement expander = Document.get().createDivElement();
        expander.appendChild(resources.expansionIcon().getSvg().getElement());
        expandHolder.getElement().appendChild(expander);
    }

    @Override
    public void setOperations(List<String> operations) {
        components = new ArrayList<>(operations.size());
        this.operations.clear();

        status.setText(LOADING);
        status.setStyleName(styles.inProgressStatusLabel());

        iconPanel.clear();
        iconPanel.getElement().appendChild((resources.loaderIcon().getSvg().getElement()));

        progressBar.addClassName(styles.progressBarInProgressStatus());
        setProgressBarState(0);

        for (String operation : operations) {
            HTML operationComponent = new HTML(operation);
            operationComponent.addStyleName(styles.waitStatus());
            this.components.add(operationComponent);
            this.operations.add(operationComponent);
        }
    }

    @Override
    public void setCurrentOperation(String operation) {
        currentOperation.clear();
        currentOperation.add(new HTML(operation));
    }

    @Override
    public void setErrorStatus(int index) {
        iconPanel.clear();
        HTML error = new HTML("!");
        error.addStyleName(styles.iconPanelErrorStatus());
        iconPanel.add(error);

        components.get(index).setStyleName(styles.completedStatus());
        progressBar.setClassName(styles.progressBarErrorStatus());
        status.setStyleName(styles.errorStatusLabel());
        setProgressBarState(100);
    }

    @Override
    public void setSuccessStatus(int index) {
        components.get(index).setStyleName(styles.completedStatus());
    }

    @Override
    public void setInProgressStatus(int index) {
        components.get(index).setStyleName(styles.inProgressStatus());
    }

    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void expandOperations() {
        operations.setVisible(true);
        resize();
    }

    @Override
    public void collapseOperations() {
        operations.setVisible(false);
    }

    @Override
    public void setProgressBarState(int percent) {
        progressBar.getStyle().setProperty("width", percent + "%");
    }

    @Override
    public Widget asWidget() {
        return rootElement;
    }

    private void resize() {
        if (!operations.isVisible()) {
            return;
        }

        int top = currentOperation.getElement().getAbsoluteTop();
        int left = currentOperation.getElement().getAbsoluteLeft();
        operations.getElement().getStyle().setPropertyPx("top", top + 27);
        operations.getElement().getStyle().setPropertyPx("left", left);
    }

    /** Styles for loader. */
    public interface LoaderCss extends CssResource {
        String errorStatusLabel();

        String inProgressStatusLabel();

        String progressBarErrorStatus();

        String progressBarInProgressStatus();

        String errorStatus();

        String completedStatus();

        String inProgressStatus();

        String waitStatus();

        String iconPanelErrorStatus();

        String statusPanel();

        String iconPanel();

        String operationPanel();

        String operations();

        String currentOperation();

        String expandedIcon();
    }

    /** Resources for loader. */
    public interface Resources extends ClientBundle {
        @Source({"Loader.css"})
        LoaderCss css();

        @Source("expansionIcon.svg")
        SVGResource expansionIcon();

        @Source("loaderIcon.svg")
        SVGResource loaderIcon();
    }

    interface LoaderViewImplUiBinder extends UiBinder<FlowPanel, LoaderViewImpl> {
    }
}
