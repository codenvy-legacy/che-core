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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.api.constraints.Constraints;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.editor.EditorWithErrors;
import org.eclipse.che.ide.api.editor.EditorWithErrors.EditorState;
import org.eclipse.che.ide.api.event.project.CloseCurrentProjectEvent;
import org.eclipse.che.ide.api.event.project.CloseCurrentProjectHandler;
import org.eclipse.che.ide.api.parts.EditorPartStack;
import org.eclipse.che.ide.api.parts.PartPresenter;
import org.eclipse.che.ide.api.parts.PartStackView.TabItem;
import org.eclipse.che.ide.api.parts.PropertyListener;
import org.eclipse.che.ide.client.inject.factories.TabItemFactory;
import org.eclipse.che.ide.part.PartStackPresenter;
import org.eclipse.che.ide.part.PartsComparator;
import org.eclipse.che.ide.part.widgets.editortab.EditorTab;
import org.eclipse.che.ide.part.widgets.listtab.ListButton;
import org.eclipse.che.ide.part.widgets.listtab.ListItem;
import org.eclipse.che.ide.part.widgets.listtab.ListItemWidget;
import org.eclipse.che.ide.util.loging.Log;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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
                                                                            CloseCurrentProjectHandler {

    private final ListButton listButton;

    private final Map<ListItem, TabItem> items;

    //this list need to save order of added parts
    private final LinkedList<PartPresenter> partsOrder;

    private PartPresenter activePart;

    @Inject
    public EditorPartStackPresenter(final EditorPartStackView view,
                                    PartsComparator partsComparator,
                                    EventBus eventBus,
                                    TabItemFactory tabItemFactory,
                                    PartStackEventHandler partStackEventHandler,
                                    ListButton listButton) {
        //noinspection ConstantConditions
        super(eventBus, partStackEventHandler, tabItemFactory, partsComparator, view, null);

        this.listButton = listButton;
        this.listButton.setDelegate(this);

        this.view.setDelegate(this);
        view.setListButton(listButton);

        this.items = new HashMap<>();
        this.partsOrder = new LinkedList<>();

        eventBus.addHandler(CloseCurrentProjectEvent.TYPE, this);
    }

    @Override
    public void onCloseCurrentProject(CloseCurrentProjectEvent event) {
        String closedProjectPath = event.getProjectConfig().getPath() + "/";

        List<TabItem> deleteTabItemList = new ArrayList<>();

        for (Map.Entry<TabItem, PartPresenter> elem: parts.entrySet()) {
            if (!(elem.getValue() instanceof EditorPartPresenter)) {
                continue;
            }
            EditorPartPresenter openedEditor = (EditorPartPresenter) elem.getValue();
            String openedEditorPath = openedEditor.getEditorInput().getFile().getPath();
            if (openedEditorPath.startsWith(closedProjectPath)) {
                deleteTabItemList.add(elem.getKey());
            }
        }

        for (final TabItem tabItem: deleteTabItemList) {
            final PartPresenter openedEditor = parts.get(tabItem);
            openedEditor.onClose(new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable caught) {
                }

                @Override
                public void onSuccess(Void result) {
                    view.removeTab(openedEditor);
                    openedEditor.removePropertyListener(propertyListener);
                    parts.remove(tabItem);
                    removeItemFromList(tabItem);
                }
            });
        }
    }

    private void removeItemFromList(@NotNull TabItem tab) {
        ListItem listItem = getListItemByTab(tab);

        if (listItem != null) {
            listButton.removeListItem(listItem);
            items.remove(listItem);
        }
    }

    @Nullable
    private ListItem getListItemByTab(@NotNull TabItem tabItem) {
        for (Map.Entry<ListItem, TabItem> entry : items.entrySet()) {
            if (tabItem.equals(entry.getValue())) {
                return entry.getKey();
            }
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void setFocus(boolean focused) {
        view.setFocus(focused);
    }

    /** {@inheritDoc} */
    @Override
    public void addPart(@NotNull PartPresenter part, Constraints constraint) {
        addPart(part);
    }

    /** {@inheritDoc} */
    @Override
    public void addPart(@NotNull PartPresenter part) {
        if (!containsPart(part)) {
            part.addPropertyListener(propertyListener);

            final EditorTab editorTab = tabItemFactory.createEditorPartButton(part.getTitleSVGImage(), part.getTitle());

            editorTab.setDelegate(this);

            parts.put(editorTab, part);
            partsOrder.add(part);

            view.addTab(editorTab, part);

            TabItem tabItem = getTabByPart(part);

            if (tabItem != null) {
                ListItem item = new ListItemWidget(tabItem);
                listButton.addListItem(item);
                items.put(item, tabItem);
            }

            if (part instanceof EditorWithErrors) {
                final EditorWithErrors presenter = ((EditorWithErrors)part);

                part.addPropertyListener(new PropertyListener() {
                    @Override
                    public void propertyChanged(PartPresenter source, int propId) {
                        EditorState editorState = presenter.getErrorState();

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
    public PartPresenter getActivePart() {
        return activePart;
    }

    /** {@inheritDoc} */
    @Override
    public void setActivePart(@NotNull PartPresenter part) {
        activePart = part;
        view.selectTab(part);
    }

    /** {@inheritDoc} */
    @Override
    public void onTabClicked(@NotNull TabItem tab) {
        activePart = parts.get(tab);
        view.selectTab(parts.get(tab));
    }

    /** {@inheritDoc} */
    @Override
    public void onTabClose(@NotNull TabItem tab) {
        final PartPresenter closedPart = parts.get(tab);
        view.removeTab(closedPart);

        parts.remove(tab);
        partsOrder.remove(closedPart);

        removeItemFromList(tab);

        activePart = partsOrder.isEmpty() ? null : partsOrder.getLast();

        closedPart.onClose(new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
                Log.error(this.getClass(), "Unexpected error occured when closing the editor. " + caught.getMessage());
            }

            @Override
            public void onSuccess(Void result) {
            }
        });
    }

}
