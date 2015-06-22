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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.ide.api.constraints.Anchor;
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
import org.eclipse.che.ide.part.projectexplorer.ProjectExplorerPartPresenter;
import org.eclipse.che.ide.workspace.WorkBenchPartController;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
public class PartStackPresenter implements Presenter, PartStackView.ActionDelegate, TabItem.ActionDelegate, PartStack {

    private final Comparator<PartPresenter>      partPresenterComparator;
    private final WorkBenchPartController        workBenchPartController;
    private final HashMap<PartPresenter, Double> partSizes;
    private final TabItemFactory                 tabItemFactory;

    protected final PartStackView                   view;
    protected final Map<TabItem, PartPresenter>     parts;
    protected final List<Integer>                   viewPartPositions;
    protected final PropertyListener                propertyListener;
    protected final PartStackEventHandler           partStackHandler;
    protected final Map<PartPresenter, Constraints> constraints;

    protected PartPresenter activePart;
    protected boolean       partsClosable;

    @Inject
    public PartStackPresenter(final EventBus eventBus,
                              PartStackEventHandler partStackEventHandler,
                              TabItemFactory tabItemFactory,
                              @Assisted final PartStackView view,
                              @Assisted @Nonnull WorkBenchPartController workBenchPartController) {
        this.view = view;
        this.partStackHandler = partStackEventHandler;
        this.workBenchPartController = workBenchPartController;
        this.tabItemFactory = tabItemFactory;
        this.partPresenterComparator = getPartPresenterComparator();

        this.parts = new HashMap<>();
        this.viewPartPositions = new ArrayList<>();
        this.constraints = new HashMap<>();
        this.partSizes = new HashMap<>();

        this.partsClosable = false;

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

        view.setDelegate(this);
    }

    @Nonnull
    private Comparator<PartPresenter> getPartPresenterComparator() {
        return new Comparator<PartPresenter>() {
            @Override
            public int compare(PartPresenter part1, PartPresenter part2) {
                String title1 = part1.getTitle();
                String title2 = part2.getTitle();
                Constraints constr1 = constraints.get(part1);
                Constraints constr2 = constraints.get(part2);

                if (constr1 == null && constr2 == null) {
                    return 0;
                }

                if ((constr1 != null && constr1.myAnchor == Anchor.FIRST) || (constr2 != null && constr2.myAnchor == Anchor.LAST)) {
                    return -1;
                }

                if ((constr2 != null && constr2.myAnchor == Anchor.FIRST) || (constr1 != null && constr1.myAnchor == Anchor.LAST)) {
                    return 1;
                }

                if (constr1 != null && constr1.myRelativeToActionId != null) {
                    Anchor anchor1 = constr1.myAnchor;
                    String relative1 = constr1.myRelativeToActionId;
                    if (anchor1 == Anchor.BEFORE && relative1.equals(title2)) {
                        return -1;
                    }
                    if (anchor1 == Anchor.AFTER && relative1.equals(title2)) {
                        return 1;
                    }
                }

                if (constr2 != null && constr2.myRelativeToActionId != null) {
                    Anchor anchor2 = constr2.myAnchor;
                    String relative2 = constr2.myRelativeToActionId;
                    if (anchor2 == Anchor.BEFORE && relative2.equals(title1)) {
                        return 1;
                    }
                    if (anchor2 == Anchor.AFTER && relative2.equals(title1)) {
                        return -1;
                    }
                }

                if (constr1 != null && constr2 == null) {
                    return 1;
                }
                if (constr1 == null) {
                    return -1;
                }
                return 0;
            }
        };
    }

    /**
     * Update part tab, it's may be title, icon or tooltip
     *
     * @param part
     */
    private void updatePartTab(PartPresenter part) {
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
            view.setActiveTab(activePart);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void addPart(PartPresenter part) {
        addPart(part, null);
    }

    /** {@inheritDoc} */
    @Override
    public void addPart(PartPresenter part, Constraints constraint) {
        if (containsPart(part)) {
            setActivePart(part);
            return;
        }

        if (part instanceof BasePresenter) {
            ((BasePresenter)part).setPartStack(this);
        }

        constraints.put(part, constraint);
        partSizes.put(part, (double)part.getSize());

        part.addPropertyListener(propertyListener);

        TabItem partButton = tabItemFactory.createPartButton(part.getTitle())
                                           .addTooltip(part.getTitleToolTip())
                                           .addIcon(part.getTitleSVGImage())
                                           .addWidget(part.getTitleWidget());
        partButton.setDelegate(this);

        parts.put(partButton, part);

        view.addTab(partButton, part);

        sortPartsOnView();
        part.onOpen();

        onRequestFocus();
    }

    /** {@inheritDoc} */
    @Override
    public boolean containsPart(PartPresenter part) {
        return parts.values().contains(part);
    }

    /** {@inheritDoc} */
    @Override
    public int getNumberOfParts() {
        return parts.size();
    }

    /** {@inheritDoc} */
    @Override
    public PartPresenter getActivePart() {
        return activePart;
    }

    /** {@inheritDoc} */
    @Override
    public void setActivePart(PartPresenter part) {
        if (activePart != null && activePart == part) {
            // request part stack to get the focus
            onRequestFocus();
            return;
        }

        // remember size of the previous active part
        if (activePart != null) {
            double size = workBenchPartController.getSize();
            partSizes.put(activePart, size);
        }

        activePart = part;

        if (part == null) {
            view.unSelectTabs();
            workBenchPartController.setHidden(true);

            return;
        } else {
            view.setActiveTab(part);
        }

        // request part stack to get the focus
        onRequestFocus();

        if (activePart != null) {
            workBenchPartController.setHidden(false);
            if (partSizes.containsKey(activePart)) {
                workBenchPartController.setSize(partSizes.get(activePart));
            } else {
                workBenchPartController.setSize(activePart.getSize());
            }
        }
    }

    /**
     * Gets all the parts registered.
     */
    public List<PartPresenter> getPartPresenters() {
        List<PartPresenter> presenters = new ArrayList<>();
        for (PartPresenter part : parts.values()) {
            presenters.add(part);
        }
        return presenters;
    }

    /** {@inheritDoc} */
    @Override
    public void hidePart(PartPresenter part) {
        if (activePart == part) {
            double size = workBenchPartController.getSize();
            partSizes.put(activePart, size);

            workBenchPartController.setHidden(true);

            activePart = null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void removePart(PartPresenter part) {
        close(part);
    }

    /**
     * Close Part
     *
     * @param part
     */
    protected void close(final PartPresenter part) {
        part.onClose(new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable throwable) {

            }

            @Override
            public void onSuccess(Void aVoid) {
                if (activePart == part) {
                    PartPresenter newActivePart = null;
                    for (PartPresenter tmpPart : parts.values()) {
                        if (tmpPart instanceof ProjectExplorerPartPresenter) {
                            newActivePart = tmpPart;
                            break;
                        }
                    }
                    setActivePart(newActivePart);
                }
                view.removeTab(part);
                constraints.remove(part);
                partSizes.remove(part);
                sortPartsOnView();
                part.removePropertyListener(propertyListener);
            }
        });
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

    /** Sort parts depending on constraint. */
    protected void sortPartsOnView() {
//        viewPartPositions.clear();
//        List<PartPresenter> sortedParts = getSortedParts();
//        for (PartPresenter partPresenter : sortedParts) {
//            viewPartPositions.add(sortedParts.indexOf(partPresenter), parts.indexOf(partPresenter));
//        }
//        view.setTabpositions(viewPartPositions);
    }

    protected List<PartPresenter> getSortedParts() {
        List<PartPresenter> sortedParts = new ArrayList<>();
        sortedParts.addAll(parts.values());
        Collections.sort(sortedParts, partPresenterComparator);
        return sortedParts;
    }

    /** {@inheritDoc} */
    @Override
    public void onTabClicked(@Nonnull TabItem selectedTab, boolean isSelected) {
        for (PartPresenter presenter : parts.values()) {
            presenter.setVisible(false);
        }

        PartPresenter selectedPart = parts.get(selectedTab);

        selectedPart.setVisible(true);

        workBenchPartController.setSize(350);
        workBenchPartController.setHidden(!isSelected);
    }

    /** Handles PartStack actions */
    public interface PartStackEventHandler {
        /** PartStack is being clicked and requests Focus */
        void onRequestFocus(PartStack partStack);
    }
}
