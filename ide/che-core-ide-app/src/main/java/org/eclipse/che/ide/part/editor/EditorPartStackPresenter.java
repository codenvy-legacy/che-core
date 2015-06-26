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
package org.eclipse.che.ide.part.editor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.ide.api.constraints.Constraints;
import org.eclipse.che.ide.api.editor.EditorWithErrors;
import org.eclipse.che.ide.api.editor.EditorWithErrors.EditorState;
import org.eclipse.che.ide.api.event.ProjectActionEvent;
import org.eclipse.che.ide.api.event.ProjectActionHandler;
import org.eclipse.che.ide.api.parts.EditorPartStack;
import org.eclipse.che.ide.api.parts.PartPresenter;
import org.eclipse.che.ide.api.parts.PartStackView.TabItem;
import org.eclipse.che.ide.api.parts.PropertyListener;
import org.eclipse.che.ide.client.inject.factories.TabItemFactory;
import org.eclipse.che.ide.part.PartStackPresenter;
import org.eclipse.che.ide.part.PartsComparator;
import org.eclipse.che.ide.part.widgets.editortab.EditorTab;
import org.eclipse.che.ide.part.widgets.listtab.ListButton;
import org.eclipse.che.ide.part.widgets.listtab.item.ListItem;
import org.eclipse.che.ide.part.widgets.listtab.item.ListItemWidget;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.eclipse.che.ide.api.editor.EditorWithErrors.EditorState.ERROR;
import static org.eclipse.che.ide.api.editor.EditorWithErrors.EditorState.WARNING;

/**
 * EditorPartStackPresenter is a special PartStackPresenter that is shared among all
 * Perspectives and used to display Editors.
 *
 * @author Nikolay Zamosenchuk
 * @author Stéphane Daviet
 * @author Dmitry Shnurenko
 */
@Singleton
public class EditorPartStackPresenter extends PartStackPresenter implements EditorPartStack,
                                                                            EditorTab.ActionDelegate,
                                                                            ListButton.ActionDelegate,
                                                                            ListItem.ActionDelegate {
    private final ListButton             listButton;
    private final Map<ListItem, TabItem> items;

    @Inject
    public EditorPartStackPresenter(final EditorPartStackView view,
                                    PartsComparator partsComparator,
                                    EventBus eventBus,
                                    TabItemFactory tabItemFactory,
                                    PartStackEventHandler partStackEventHandler,
                                    ListButton listButton) {
        super(eventBus, partStackEventHandler, tabItemFactory, partsComparator, view, null);

        this.listButton = listButton;
        this.listButton.setDelegate(this);

        this.view.setDelegate(this);
        view.setListButton(listButton);

        this.items = new HashMap<>();

        eventBus.addHandler(ProjectActionEvent.TYPE, new ProjectActionHandler() {
            @Override
            public void onProjectOpened(ProjectActionEvent event) {
            }

            @Override
            public void onProjectClosing(ProjectActionEvent event) {
            }

            @Override
            public void onProjectClosed(ProjectActionEvent event) {
                Iterator<TabItem> itemIterator = parts.keySet().iterator();

                while (itemIterator.hasNext()) {
                    TabItem tabItem = itemIterator.next();

                    view.removeTab(parts.get(tabItem));

                    itemIterator.remove();

                    removeItemFromList(tabItem);
                }
            }
        });
    }

    private void removeItemFromList(@Nonnull TabItem tab) {
        ListItem listItem = getListItemByTab(tab);

        if (listItem != null) {
            listButton.removeListItem(listItem);
        }
    }


    @Nullable
    private ListItem getListItemByTab(@Nonnull TabItem tabItem) {
        for (Map.Entry<ListItem, TabItem> entry : items.entrySet()) {
            if (tabItem.equals(entry.getValue())) {
                return entry.getKey();
            }
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void addPart(@Nonnull PartPresenter part, Constraints constraint) {
        addPart(part);
    }

    /** {@inheritDoc} */
    @Override
    public void addPart(@Nonnull PartPresenter part) {
        if (!containsPart(part)) {
            part.addPropertyListener(propertyListener);

            final EditorTab editorTab = tabItemFactory.createEditorPartButton(part.getTitleSVGImage(), part.getTitle());

            editorTab.setDelegate(this);

            parts.put(editorTab, part);

            view.addTab(editorTab, part);

            TabItem tabItem = getTabByPart(part);

            if (tabItem != null) {
                ListItem item = ListItemWidget.create(tabItem.getTitle());

                item.setDelegate(this);

                listButton.addListItem(item);

                items.put(item, tabItem);
            }

            if (part instanceof EditorWithErrors) {
                final EditorWithErrors presenter = ((EditorWithErrors)part);

                part.addPropertyListener(new PropertyListener() {
                    @Override
                    public void propertyChanged(PartPresenter source, int propId) {
                        EditorState editorState = presenter.getErrorState();

                        if (editorState == null) {
                            return;
                        }

                        editorTab.setErrorMark(ERROR.equals(editorState));
                        editorTab.setWarningMark(WARNING.equals(editorState));
                    }
                });
            }
        }

        view.selectTab(part);
    }

    /** {@inheritDoc} */
    @Override
    public void setActivePart(@Nonnull PartPresenter part) {
        addPart(part);
    }

    /** {@inheritDoc} */
    @Override
    public void onTabClicked(@Nonnull TabItem tab) {
        view.selectTab(parts.get(tab));
    }

    /** {@inheritDoc} */
    @Override
    public void onTabClose(@Nonnull TabItem tab) {
        view.removeTab(parts.get(tab));

        parts.remove(tab);

        removeItemFromList(tab);
    }

    /** {@inheritDoc} */
    @Override
    public void onListButtonClicked() {
        listButton.showList();
    }

    /** {@inheritDoc} */
    @Override
    public void onListButtonMouseOver() {

    }

    /** {@inheritDoc} */
    @Override
    public void onListButtonMouseOut() {

    }

    /** {@inheritDoc} */
    @Override
    public void onCloseItemClicked(@Nonnull ListItem listItem) {
        TabItem closedItem = items.get(listItem);

        removePart(parts.get(closedItem));

        listButton.removeListItem(listItem);
        listButton.hide();
    }
}
