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
package org.eclipse.che.ide.core.problemDialog;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.ui.dialogs.ConfirmCallback;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.ui.dialogs.choice.ChoiceDialog;
import org.eclipse.che.ide.ui.window.Window;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * The implementation for the problem dialog component.
 *
 * @author Roman Nikitenko.
 */
public class ProjectProblemDialogViewImpl extends Window implements ProjectProblemDialogView {

    @UiField
    ListBox     typesBox;
    @UiField
    SimplePanel messagePanel;

    Button configureButton;
    Button openAsIsButton;
    Button openAsButton;
    Widget widget;

    private       ActionDelegate               delegate;
    private final DialogFactory                dialogFactory;
    private final CoreLocalizationConstant     localizedConstant;
    private final ProjectProblemDialogUiBinder projectProblemDialogUiBinder;

    @Inject
    public ProjectProblemDialogViewImpl(DialogFactory dialogFactory,
                                        CoreLocalizationConstant localizedConstant,
                                        ProjectProblemDialogUiBinder projectProblemDialogUiBinder) {
        this.dialogFactory = dialogFactory;
        this.localizedConstant = localizedConstant;
        this.projectProblemDialogUiBinder = projectProblemDialogUiBinder;
    }

    @Override
    public void showDialog(@Nullable List<String> estimatedTypes) {
        if (estimatedTypes == null || estimatedTypes.isEmpty()) {
            ChoiceDialog problemDialog = dialogFactory.createChoiceDialog(localizedConstant.projectProblemDialogTitle(),
                                                                          localizedConstant.projectProblemNotDetermineProjectTypeMessage(),
                                                                          localizedConstant.projectProblemConfigureButtonTitle(),
                                                                          localizedConstant.projectProblemOpenAsIsButtonTitle(),
                                                                          getConfigureButtonHandler(),
                                                                          getOpenAsIsButtonHandler());
            problemDialog.show();
            return;
        }

        if (estimatedTypes.size() == 1) {
            String projectType = estimatedTypes.get(0);
            ChoiceDialog problemDialog = dialogFactory.createChoiceDialog(localizedConstant.projectProblemDialogTitle(),
                                                                          localizedConstant.projectProblemOpenAsMessage(projectType),
                                                                          localizedConstant.projectProblemConfigureButtonTitle(),
                                                                          localizedConstant.projectProblemOpenAsIsButtonTitle(),
                                                                          localizedConstant.projectProblemOpenAsButtonTitle(projectType),
                                                                          getConfigureButtonHandler(),
                                                                          getOpenAsIsButtonHandler(),
                                                                          getOpenAsButtonHandler());
            problemDialog.show();
            return;
        }
        createWindow();
        fillData(estimatedTypes);
        show();
    }

    @Override
    public void setMessage(String message) {
        messagePanel.clear();
        messagePanel.getElement().setInnerHTML(message);
    }

    private void createWindow() {
        if (widget != null) {
            typesBox.clear();
            return;
        }

        setTitle(localizedConstant.projectProblemDialogTitle());

        widget = projectProblemDialogUiBinder.createAndBindUi(this);
        setWidget(widget);

        messagePanel.addStyleName(resources.centerPanelCss().label());

        configureButton = createButton(localizedConstant.projectProblemConfigureButtonTitle(), "ask-dialog-first", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                delegate.onConfigure();
                onClose();
            }
        });
        openAsIsButton = createButton(localizedConstant.projectProblemOpenAsIsButtonTitle(), "ask-dialog-second", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                delegate.onOpenAsIs();
                onClose();
            }
        });
        openAsButton = createButton("", "ask-dialog-third", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                delegate.onOpenAs();
                onClose();
            }
        });

        configureButton.addStyleName(resources.centerPanelCss().blueButton());
        openAsIsButton.addStyleName(resources.centerPanelCss().button());
        openAsButton.addStyleName(resources.centerPanelCss().button());

        getFooter().add(configureButton);
        getFooter().add(openAsIsButton);
        getFooter().add(openAsButton);
    }

    private void fillData(final @NotNull List<String> estimatedTypes) {
        for (String estimatedType : estimatedTypes) {
            typesBox.addItem(estimatedType);
        }
        typesBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                String selectedProjectType = estimatedTypes.get(typesBox.getSelectedIndex());
                delegate.onSelectedTypeChanged(selectedProjectType);
            }
        });

        String projectType = estimatedTypes.get(0);
        setMessage(localizedConstant.projectProblemPressingButtonsMessage(projectType));
        openAsButton.setText(localizedConstant.projectProblemOpenAsButtonTitle(projectType));
    }

    private ConfirmCallback getConfigureButtonHandler() {
        return new ConfirmCallback() {
            @Override
            public void accepted() {
                delegate.onConfigure();
            }
        };
    }

    private ConfirmCallback getOpenAsIsButtonHandler() {
        return new ConfirmCallback() {
            @Override
            public void accepted() {
                delegate.onOpenAsIs();
            }
        };
    }

    private ConfirmCallback getOpenAsButtonHandler() {
        return new ConfirmCallback() {
            @Override
            public void accepted() {
                delegate.onOpenAs();
            }
        };
    }

    @Override
    public void setOpenAsButtonTitle(String title) {
        openAsButton.setText(title);
    }

    @Override
    public int getSelectedTypeIndex() {
        return typesBox.getSelectedIndex();
    }

    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    protected void onEnterClicked() {
        delegate.onEnterClicked();
    }

    interface ProjectProblemDialogUiBinder extends UiBinder<Widget, ProjectProblemDialogViewImpl> {
    }
}
