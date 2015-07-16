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
package org.eclipse.che.ide.settings;

import elemental.html.TableElement;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.settings.common.SettingsPagePresenter;
import org.eclipse.che.ide.ui.list.CategoriesList;
import org.eclipse.che.ide.ui.list.Category;
import org.eclipse.che.ide.ui.list.Category.CategoryEventDelegate;
import org.eclipse.che.ide.ui.list.CategoryRenderer;
import org.eclipse.che.ide.ui.window.Window;
import org.eclipse.che.ide.util.dom.Elements;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Dmitry Shnurenko
 */
@Singleton
public class SettingsViewImpl extends Window implements SettingsView {
    interface SettingsViewImplUiBinder extends UiBinder<Widget, SettingsViewImpl> {
    }

    private static final SettingsViewImplUiBinder UI_BINDER = GWT.create(SettingsViewImplUiBinder.class);

    private final CategoryEventDelegate<SettingsPagePresenter> categoryPageDelegate;
    private final CategoryRenderer<SettingsPagePresenter>      settingsRenderer;
    private final CategoriesList                               list;
    private final CoreLocalizationConstant                     locale;

    @UiField
    SimplePanel settingsGroup;
    @UiField
    SimplePanel contentPanel;

    @UiField(provided = true)
    org.eclipse.che.ide.Resources res;

    private ActionDelegate delegate;

    private Button btnSave;

    @Inject
    public SettingsViewImpl(final org.eclipse.che.ide.Resources res, CoreLocalizationConstant locale) {
        this.locale = locale;
        this.res = res;

        this.setTitle(locale.projectSettingsTitle());

        this.setWidget(UI_BINDER.createAndBindUi(this));

        TableElement tableElement = Elements.createTableElement();
        tableElement.setAttribute("style", "width: 100%");
        list = new CategoriesList(res);
        settingsGroup.add(list);
        createButtons(res);

        categoryPageDelegate = new CategoryEventDelegate<SettingsPagePresenter>() {
            @Override
            public void onListItemClicked(Element listItemBase, SettingsPagePresenter itemData) {
                delegate.onSettingsGroupSelected(itemData);
            }
        };

        settingsRenderer = new CategoryRenderer<SettingsPagePresenter>() {
            @Override
            public void renderElement(Element element, SettingsPagePresenter presenter) {
                element.setInnerText(presenter.getTitle());
            }

            @Override
            public SpanElement renderCategory(Category<SettingsPagePresenter> category) {
                SpanElement spanElement = Document.get().createSpanElement();
                spanElement.setClassName(res.defaultCategoriesListCss().headerText());
                spanElement.setInnerText(category.getTitle());
                return spanElement;
            }
        };
    }

    private void createButtons(@Nonnull final org.eclipse.che.ide.Resources resources) {
        btnSave = createButton(locale.save(), "window-settings-storeChanges", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                delegate.onSaveClicked();
            }
        });

        btnSave.addStyleName(resources.wizardCss().button());
        btnSave.addStyleName(resources.wizardCss().rightButton());
        btnSave.addStyleName(resources.wizardCss().buttonPrimary());
        getFooter().add(btnSave);

        Button btnRefresh = createButton(locale.refresh(), "window-settings-refresh", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                delegate.onRefreshClicked();
            }
        });
        btnRefresh.addStyleName(resources.wizardCss().button());
        btnRefresh.addStyleName(resources.wizardCss().buttonSuccess());
        getFooter().add(btnRefresh);

        Button btnClose = createButton(locale.close(), "window-settings-close", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                delegate.onCloseClicked();
            }
        });
        btnClose.addStyleName(resources.wizardCss().button());
        getFooter().add(btnClose);
    }

    /** {@inheritDoc} */
    @Override
    public void selectSettingGroup(@Nonnull SettingsPagePresenter presenter) {
        list.selectElement(presenter);
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        hide();
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public AcceptsOneWidget getContentPanel() {
        return contentPanel;
    }

    /** {@inheritDoc} */
    @Override
    public void enableSaveButton(boolean enabled) {
        btnSave.setEnabled(enabled);
    }

    /** {@inheritDoc} */
    @Override
    public void setSettings(@Nonnull Map<String, Set<SettingsPagePresenter>> preferences) {
        List<Category<?>> categoriesList = new ArrayList<>();
        for (String name : preferences.keySet()) {
            Set<SettingsPagePresenter> data = preferences.get(name);

            Category<SettingsPagePresenter> category = new Category<>(name, settingsRenderer, data, categoryPageDelegate);
            categoriesList.add(category);
        }

        list.render(categoriesList);
    }

    /** {@inheritDoc} */
    @Override
    public void setDelegate(@Nonnull ActionDelegate delegate) {
        this.delegate = delegate;
    }
}