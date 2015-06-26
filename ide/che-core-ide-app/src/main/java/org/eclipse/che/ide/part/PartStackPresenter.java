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
package org.eclipse.che.ide.part;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.ide.api.constraints.Constraints;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.event.EditorDirtyStateChangedEvent;
import org.eclipse.che.ide.api.mvp.Presenter;
import org.eclipse.che.ide.api.parts.PartPresenter;
import org.eclipse.che.ide.api.parts.PartStack;
import org.eclipse.che.ide.api.parts.PartStackView;
import org.eclipse.che.ide.api.parts.PartStackView.TabItem;
import org.eclipse.che.ide.api.parts.PropertyListener;
import org.eclipse.che.ide.api.parts.base.BasePresenter;
import org.eclipse.che.ide.client.inject.factories.TabItemFactory;
import org.eclipse.che.ide.part.widgets.partbutton.PartButton;
import org.eclipse.che.ide.workspace.WorkBenchPartController;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements "Tab-like" UI Component, that accepts PartPresenters as child elements.
 * <p/>
 * PartStack support "focus" (please don't mix with GWT Widget's Focus feature). Focused PartStack will highlight active Part, notifying
 * user what component is currently active.
 *
 * @author Nikolay Zamosenchuk
 * @author Stéphane Daviet
 * @author Dmitry Shnurenko
 */
public class PartStackPresenter implements Presenter, PartStackView.ActionDelegate, PartButton.ActionDelegate, PartStack {

    private static final double DEFAULT_PART_SIZE = 285;

    private final WorkBenchPartController         workBenchPartController;
    private final PartsComparator                 partsComparator;
    private final Map<PartPresenter, Constraints> constraints;
    private final PartStackEventHandler           partStackHandler;

    protected final Map<TabItem, PartPresenter> parts;
    protected final TabItemFactory              tabItemFactory;
    protected final PartStackView               view;
    protected final PropertyListener            propertyListener;

    private TabItem previousSelectedTab;
    private double  partSize;

    protected PartPresenter activePart;

    @Inject
    public PartStackPresenter(final EventBus eventBus,
                              PartStackEventHandler partStackEventHandler,
                              TabItemFactory tabItemFactory,
                              PartsComparator partsComparator,
                              @Assisted final PartStackView view,
                              @Assisted @Nonnull WorkBenchPartController workBenchPartController) {
        this.view = view;
        this.view.setDelegate(this);

        this.partStackHandler = partStackEventHandler;
        this.workBenchPartController = workBenchPartController;
        this.tabItemFactory = tabItemFactory;
        this.partsComparator = partsComparator;

        this.parts = new HashMap<>();
        this.constraints = new HashMap<>();

        this.propertyListener = new PropertyListener() {
            @Override
            public void propertyChanged(PartPresenter source, int propId) {
                if (PartPresenter.TITLE_PROPERTY == propId) {
                    updatePartTab(source);
                } else if (EditorPartPresenter.PROP_DIRTY == propId) {
                    eventBus.fireEvent(new EditorDirtyStateChangedEvent((EditorPartPresenter)source));
                }
            }
        };

        partSize = DEFAULT_PART_SIZE;
    }

    private void updatePartTab(@Nonnull PartPresenter part) {
        if (!containsPart(part)) {
            throw new IllegalArgumentException("This part stack not contains: " + part.getTitle());
        }

        view.updateTabItem(part);
    }

    /** {@inheritDoc} */
    @Override
    public void go(AcceptsOneWidget container) {
        container.setWidget(view);
        if (activePart != null) {
            view.selectTab(activePart);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void addPart(@Nonnull PartPresenter part) {
        addPart(part, null);
    }

    /** {@inheritDoc} */
    @Override
    public void addPart(@Nonnull PartPresenter part, @Nullable Constraints constraint) {
        if (containsPart(part)) {
            workBenchPartController.setHidden(true);

            TabItem selectedItem = getTabByPart(part);

            if (selectedItem != null) {
                selectedItem.unSelect();
            }

            return;
        }

        if (part instanceof BasePresenter) {
            ((BasePresenter)part).setPartStack(this);
        }

        part.addPropertyListener(propertyListener);

        PartButton partButton = tabItemFactory.createPartButton(part.getTitle())
                                              .addTooltip(part.getTitleToolTip())
                                              .addIcon(part.getTitleSVGImage())
                                              .addWidget(part.getTitleWidget());
        partButton.setDelegate(this);

        parts.put(partButton, part);
        constraints.put(part, constraint);

        view.addTab(partButton, part);

        sortPartsOnView();

        onRequestFocus();
    }

    private void sortPartsOnView() {
        List<PartPresenter> sortedParts = new ArrayList<>();
        sortedParts.addAll(parts.values());
        partsComparator.setConstraints(constraints);

        Collections.sort(sortedParts, partsComparator);

        view.setTabPositions(sortedParts);
    }

    /** {@inheritDoc} */
    @Override
    public boolean containsPart(PartPresenter part) {
        return parts.values().contains(part);
    }

    /** {@inheritDoc} */
    @Override
    public PartPresenter getActivePart() {
        return activePart;
    }

    /** {@inheritDoc} */
    @Override
    public void setActivePart(@Nonnull PartPresenter part) {
        TabItem activeTab = getTabByPart(part);

        if (activeTab == null) {
            return;
        }

        activePart = part;
        selectActiveTab(activeTab);
    }

    @Nullable
    protected TabItem getTabByPart(@Nonnull PartPresenter part) {
        for (Map.Entry<TabItem, PartPresenter> entry : parts.entrySet()) {

            if (part.equals(entry.getValue())) {
                return entry.getKey();
            }
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void hidePart(PartPresenter part) {
        TabItem activeTab = getTabByPart(part);

        if (activeTab == null) {
            return;
        }

        previousSelectedTab = activeTab;

        onTabClicked(activeTab);
    }

    /** {@inheritDoc} */
    @Override
    public void removePart(PartPresenter part) {
        parts.remove(getTabByPart(part));

        view.removeTab(part);
    }

    /** {@inheritDoc} */
    @Override
    public void onRequestFocus() {
        partStackHandler.onRequestFocus(PartStackPresenter.this);
    }

    /** {@inheritDoc} */
    @Override
    public void setFocus(boolean focused) {
        view.setFocus(focused);
    }

    /** {@inheritDoc} */
    @Override
    public void onTabClicked(@Nonnull TabItem selectedTab) {
        if (selectedTab.equals(previousSelectedTab)) {
            selectedTab.unSelect();

            partSize = workBenchPartController.getSize();

            workBenchPartController.setSize(0);

            previousSelectedTab = null;

            return;
        }

        previousSelectedTab = selectedTab;
        activePart = parts.get(selectedTab);

        selectActiveTab(selectedTab);
    }

    private void selectActiveTab(@Nonnull TabItem selectedTab) {
        workBenchPartController.setSize(partSize);
        workBenchPartController.setHidden(false);

        PartPresenter selectedPart = parts.get(selectedTab);

        view.selectTab(selectedPart);
    }

    /** Handles PartStack actions */
    public interface PartStackEventHandler {
        /** PartStack is being clicked and requests Focus */
        void onRequestFocus(PartStack partStack);
    }
}
