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
package org.eclipse.che.ide.createworkspace;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.machine.shared.dto.recipe.RecipeDescriptor;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.createworkspace.tagentry.TagEntry;
import org.eclipse.che.ide.ui.window.Window;

import java.util.ArrayList;
import java.util.List;

/**
 * The class contains business logic which allows to set up special parameters for creating user workspaces.
 *
 * @author Dmitry Shnurenko
 */
@Singleton
class CreateWorkspaceViewImpl extends Window implements CreateWorkSpaceView, TagEntry.ActionDelegate {

    interface CreateWorkspaceViewImplUiBinder extends UiBinder<Widget, CreateWorkspaceViewImpl> {
    }

    private static final CreateWorkspaceViewImplUiBinder UI_BINDER = GWT.create(CreateWorkspaceViewImplUiBinder.class);

    private static final int BORDER_WIDTH = 1;

    static final String RECIPE_URL = "https://raw.githubusercontent.com/codenvy/dockerfiles/master/base/jdk8_maven3_tomcat8/Dockerfile";

    private final TagEntryFactory   tagFactory;
    private final PopupPanel        popupPanel;
    private final FlowPanel         tagsPanel;
    private final HidePopupCallBack hidePopupCallBack;

    @UiField(provided = true)
    final CoreLocalizationConstant locale;

    @UiField
    TextBox wsName;
    @UiField
    TextBox recipeURL;
    @UiField
    Label   recipeUrlError;
    @UiField
    TextBox tags;
    @UiField
    Label   tagsError;
    @UiField
    Label   nameError;

    private ActionDelegate delegate;
    private Button         createButton;

    @Inject
    public CreateWorkspaceViewImpl(CoreLocalizationConstant locale,
                                   org.eclipse.che.ide.Resources resources,
                                   TagEntryFactory tagFactory,
                                   FlowPanel tagsPanel,
                                   final PopupPanel popupPanel) {
        this.locale = locale;
        this.tagFactory = tagFactory;

        this.tagsPanel = tagsPanel;
        this.tagsPanel.setStyleName(resources.coreCss().tagsPanel());

        this.popupPanel = popupPanel;
        this.popupPanel.setStyleName(resources.coreCss().createWsTagsPopup());
        this.popupPanel.addDomHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                popupPanel.hide();
            }
        }, ClickEvent.getType());

        this.hidePopupCallBack = new HidePopupCallBack() {
            @Override
            public void hidePopup() {
                popupPanel.hide();
            }
        };

        setWidget(UI_BINDER.createAndBindUi(this));

        setTitle(locale.createWsTitle());

        hideCrossButton();
        setHideOnEscapeEnabled(false);

        recipeURL.setText(RECIPE_URL);
        wsName.setText(locale.createWsDefaultName());

        createButton = createButton(locale.createWsButton(), "create-workspace-button", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                delegate.onCreateButtonClicked();
            }
        });

        addButtonToFooter(createButton);
    }

    /** {@inheritDoc} */
    @Override
    public void setWorkspaceName(String name) {
        wsName.setText(name);
    }

    /** {@inheritDoc} */
    @Override
    public String getRecipeUrl() {
        return recipeURL.getText();
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getTags() {
        List<String> tagList = new ArrayList<>();

        for (String tag : tags.getValue().split(" ")) {
            if (!tag.isEmpty()) {
                tagList.add(tag.trim());
            }
        }

        return tagList;
    }

    /** {@inheritDoc} */
    @Override
    public String getWorkspaceName() {
        return wsName.getText();
    }

    /** {@inheritDoc} */
    @Override
    public void showRecipes(List<RecipeDescriptor> recipes) {
        tagsPanel.clear();

        for (RecipeDescriptor descriptor : recipes) {
            TagEntry tag = tagFactory.create(descriptor);
            tag.setDelegate(this);

            tag.setStyles();

            tagsPanel.add(tag);
        }

        popupPanel.setWidget(tagsPanel);

        int xPanelCoordinate = tags.getAbsoluteLeft() + BORDER_WIDTH;
        int yPanelCoordinate = tags.getAbsoluteTop() + tags.getOffsetHeight();

        popupPanel.setPopupPosition(xPanelCoordinate, yPanelCoordinate);
        popupPanel.show();
    }

    /** {@inheritDoc} */
    @Override
    public void onTagClicked(TagEntry tag) {
        RecipeDescriptor descriptor = tag.getDescriptor();

        String recipeUrl = descriptor.getLink("get recipe script").getHref();

        recipeURL.setValue(recipeUrl);
        recipeURL.setTitle(recipeUrl);

        delegate.onRecipeUrlChanged();
    }

    /** {@inheritDoc} */
    @Override
    public void setVisibleUrlError(boolean visible) {
        recipeUrlError.setVisible(visible);
    }

    /** {@inheritDoc} */
    @Override
    public void setVisibleTagsError(boolean visible) {
        tagsError.setVisible(visible);
    }

    /** {@inheritDoc} */
    @Override
    public void setEnableCreateButton(boolean enable) {
        createButton.setEnabled(enable);
    }

    /** {@inheritDoc} */
    @Override
    public void setVisibleNameError(boolean visible) {
        nameError.setVisible(visible);
    }

    @UiHandler("tags")
    public void onTagsChanged(@SuppressWarnings("UnusedParameters") KeyUpEvent event) {
        String tag = tags.getText();

        tagsError.setVisible(!tag.isEmpty());

        delegate.onTagsChanged(hidePopupCallBack);
    }

    @UiHandler("recipeURL")
    public void onRecipeUrlChanged(@SuppressWarnings("UnusedParameters") KeyUpEvent event) {
        delegate.onRecipeUrlChanged();
    }

    @UiHandler("wsName")
    public void onWorkspaceNameChanged(@SuppressWarnings("UnusedParameters") KeyUpEvent event) {
        delegate.onNameChanged(wsName.getText());
    }

    /** {@inheritDoc} */
    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }
}