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
package org.eclipse.che.ide.projecttype.wizard.recipespage;

import elemental.dom.Element;
import elemental.html.TableCellElement;
import elemental.html.TableElement;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.ui.list.SimpleList;
import org.eclipse.che.ide.util.dom.Elements;

import java.util.Collections;
import java.util.List;

/**
 * The implementation of {@link RecipesPageView}.
 *
 * @author Artem Zatsarynnyy
 */
@Singleton
public class RecipesPageViewImpl implements RecipesPageView {

    private static RecipesPageViewImplUiBinder uiBinder = GWT.create(RecipesPageViewImplUiBinder.class);

    private final DockLayoutPanel rootElement;

    @UiField
    ScrollPanel listPanel;
    @UiField(provided = true)
    Resources   res;

    private ActionDelegate     delegate;
    private SimpleList<String> list;

    @Inject
    protected RecipesPageViewImpl(Resources resources) {
        this.res = resources;

        rootElement = uiBinder.createAndBindUi(this);

        TableElement tableElement = Elements.createTableElement();
        tableElement.setAttribute("style", "width: 100%");

        final SimpleList.ListItemRenderer<String> listItemRenderer = new SimpleList.ListItemRenderer<String>() {
            @Override
            public void render(Element itemElement, String itemData) {
                TableCellElement label = Elements.createTDElement();
                label.setInnerHTML(itemData);
                itemElement.appendChild(label);
                UIObject.ensureDebugId((com.google.gwt.dom.client.Element)itemElement, "file-openProject-" + itemData);
            }

            @Override
            public Element createElement() {
                return Elements.createTRElement();
            }
        };

        final SimpleList.ListEventDelegate<String> listDelegate = new SimpleList.ListEventDelegate<String>() {
            public void onListItemClicked(Element itemElement, String itemData) {
                list.getSelectionModel().setSelectedItem(itemData);
                delegate.onRecipeSelected(itemData);
            }

            public void onListItemDoubleClicked(Element listItemBase, String itemData) {
                list.getSelectionModel().setSelectedItem(itemData);
                delegate.onRecipeSelected(itemData);
            }
        };

        list = SimpleList.create((SimpleList.View)tableElement, res.defaultSimpleListCss(), listItemRenderer, listDelegate);

        this.listPanel.add(list);
    }

    /** {@inheritDoc} */
    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public Widget asWidget() {
        return rootElement;
    }

    /** {@inheritDoc} */
    @Override
    public void setRecipes(List<String> recipes) {
        list.render(recipes);
    }

    @Override
    public void clearRecipes() {
        list.render(Collections.<String>emptyList());
    }

    /** {@inheritDoc} */
    @Override
    public void selectRecipe(String recipe) {
        list.getSelectionModel().setSelectedItem(recipe);
    }

    interface RecipesPageViewImplUiBinder extends UiBinder<DockLayoutPanel, RecipesPageViewImpl> {
    }
}