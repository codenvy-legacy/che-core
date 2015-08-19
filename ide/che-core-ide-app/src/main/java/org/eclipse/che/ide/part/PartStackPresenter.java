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

import org.eclipse.che.ide.part.projectexplorer.ProjectExplorerPartPresenter;
import org.eclipse.che.ide.workspace.WorkBenchPartController;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.web.bindery.event.shared.EventBus;

import org.vectomatic.dom.svg.ui.SVGImage;
import org.vectomatic.dom.svg.ui.SVGResource;

import java.util.ArrayList;
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
 */
public class PartStackPresenter implements Presenter, PartStackView.ActionDelegate, PartStack {

    /** Handles PartStack actions */
    public interface PartStackEventHandler {
        /** PartStack is being clicked and requests Focus */
        void onRequestFocus(PartStack partStack);
    }

    private         HashMap<PartPresenter, Double>  partSizes         = new HashMap<>();
    /** list of parts */
    protected final List<PartPresenter>             parts             = new ArrayList<>();
    protected final List<Integer>                   viewPartPositions = new ArrayList<>();
    protected final Map<PartPresenter, Constraints> constraints       = new HashMap<>();
    /** view implementation */
    protected final PartStackView view;
    private final   EventBus      eventBus;
    private final Comparator partPresenterComparator = getPartPresenterComparator();
    protected     boolean    partsClosable           = false;

    protected PropertyListener propertyListener = new PropertyListener() {
        @Override
        public void propertyChanged(PartPresenter source, int propId) {
            if (PartPresenter.TITLE_PROPERTY == propId) {
                updatePartTab(source);
            } else if (EditorPartPresenter.PROP_DIRTY == propId) {
                eventBus.fireEvent(new EditorDirtyStateChangedEvent(
                        (EditorPartPresenter)source));
            }
        }
    };

    /** current active part */
    protected PartPresenter           activePart;
    protected PartStackEventHandler   partStackHandler;
    /** Container for every new PartPresenter which will be added to this PartStack. */
    protected AcceptsOneWidget        partViewContainer;
    private   WorkBenchPartController workBenchPartController;

    @Inject
    public PartStackPresenter(EventBus eventBus,
                              PartStackEventHandler partStackEventHandler,
                              @Assisted final PartStackView view,
                              @Assisted WorkBenchPartController workBenchPartController) {
        this.view = view;
        this.eventBus = eventBus;
        partStackHandler = partStackEventHandler;
        this.workBenchPartController = workBenchPartController;
        partViewContainer = new AcceptsOneWidget() {
            @Override
            public void setWidget(IsWidget w) {
                view.getContentPanel().add(w);
            }
        };
        view.setDelegate(this);
    }

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
        if (!parts.contains(part)) {
            throw new IllegalArgumentException("This part stack not contains: " + part.getTitle());
        }
        int index = parts.indexOf(part);
        view.updateTabItem(index,
                           part.decorateIcon(part.getTitleSVGImage() != null ? new SVGImage(part.getTitleSVGImage()) : null),
                           part.getTitle(),
                           part.getTitleToolTip(),
                           part.getTitleWidget());
    }

    /** {@inheritDoc} */
    @Override
    public void go(AcceptsOneWidget container) {
        container.setWidget(view);
        if (activePart != null) {
            view.setActiveTab(parts.indexOf(activePart));
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
        if (parts.contains(part)) {
            // part already exists
            // activate it
            setActivePart(part);
            // and return
            return;
        }

        if (part instanceof BasePresenter) {
            ((BasePresenter)part).setPartStack(this);
        }

        parts.add(part);
        constraints.put(part, constraint);
        partSizes.put(part, Double.valueOf(part.getSize()));

        part.addPropertyListener(propertyListener);
        // include close button
        SVGResource titleSVGResource = part.getTitleSVGImage();
        SVGImage titleSVGImage = null;
        if (titleSVGResource != null) {
            titleSVGImage = part.decorateIcon(new SVGImage(titleSVGResource));
        }
        TabItem tabItem = view.addTab(titleSVGImage, part.getTitle(), part.getTitleToolTip(), part.getTitleWidget(), partsClosable);
        bindEvents(tabItem, part);
        part.go(partViewContainer);
        sortPartsOnView();
        part.onOpen();

        // request focus
        onRequestFocus();
    }

    /** {@inheritDoc} */
    @Override
    public boolean containsPart(PartPresenter part) {
        return parts.contains(part);
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
        if (activePart != null && workBenchPartController != null) {
            double size = workBenchPartController.getSize();
            partSizes.put(activePart, Double.valueOf(size));
        }

        activePart = part;

        if (part == null) {
            view.setActiveTab(-1);
            workBenchPartController.setHidden(true);
        } else {
            view.setActiveTab(parts.indexOf(activePart));
        }

        // request part stack to get the focus
        onRequestFocus();

        if (activePart != null && workBenchPartController != null) {
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
        for (PartPresenter part : parts) {
            presenters.add(part);
        }
        return presenters;
    }

    /** {@inheritDoc} */
    @Override
    public void hidePart(PartPresenter part) {
        if (activePart == part) {
            if (workBenchPartController != null) {
                double size = workBenchPartController.getSize();
                partSizes.put(activePart, Double.valueOf(size));
                workBenchPartController.setHidden(true);
            }
            activePart = null;
            view.setActiveTab(-1);
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
                int partIndex = parts.indexOf(part);
                if (activePart == part) {
                    PartPresenter newActivePart = null;
                    for (PartPresenter tmpPart : parts) {
                        if (tmpPart instanceof ProjectExplorerPartPresenter) {
                            newActivePart = tmpPart;
                            break;
                        }
                    }
                    setActivePart(newActivePart);
                }
                view.removeTab(partIndex);
                constraints.remove(part);
                parts.remove(part);
                partSizes.remove(part);
                sortPartsOnView();
                part.removePropertyListener(propertyListener);
            }
        });
    }

    HandlerRegistration eventsBlocker;

    /**
     * Bind Activate and Close events to the Tab
     *
     * @param item
     * @param part
     */
    protected void bindEvents(final TabItem item, final PartPresenter part) {
        item.addCloseHandler(new CloseHandler<PartStackView.TabItem>() {
            @Override
            public void onClose(CloseEvent<TabItem> event) {
                close(part);
            }
        });

        item.addMouseDownHandler(new MouseDownHandler() {
            @Override
            public void onMouseDown(MouseDownEvent event) {
                /* Blocking any events excepting Mouse UP */
                eventsBlocker = Event.addNativePreviewHandler(new Event.NativePreviewHandler() {
                    @Override
                    public void onPreviewNativeEvent(Event.NativePreviewEvent event) {
                        if (event.getTypeInt() == Event.ONMOUSEUP) {
                            eventsBlocker.removeHandler();
                            return;
                        }

                        event.cancel();
                        event.getNativeEvent().preventDefault();
                        event.getNativeEvent().stopPropagation();
                    }
                });

                if (activePart == part) {
                    if (partsClosable) {
                        // request part stack to get the focus
                        onRequestFocus();
                    } else {
                        if (workBenchPartController != null) {
                            //partsSize = workBenchPartController.getSize();
                            double size = workBenchPartController.getSize();
                            partSizes.put(activePart, Double.valueOf(size));
                            workBenchPartController.setHidden(true);
                        }
                        activePart = null;
                        view.setActiveTab(-1);
                    }
                } else {
                    // make active
                    setActivePart(part);
                }
            }
        });

    }

    /**
     * Returns the list of parts.
     *
     * @return {@link java.util.List} array of parts
     */
    protected List<PartPresenter> getParts() {
        return parts;
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
        viewPartPositions.clear();
        List<PartPresenter> sortedParts = getSortedParts();
        for (PartPresenter partPresenter : sortedParts) {
            viewPartPositions.add(sortedParts.indexOf(partPresenter), parts.indexOf(partPresenter));
        }
        view.setTabpositions(viewPartPositions);
    }

    protected List<PartPresenter> getSortedParts() {
        List<PartPresenter> sortedParts = new ArrayList<>();
        sortedParts.addAll(parts);
        java.util.Collections.sort(sortedParts, partPresenterComparator);
        return sortedParts;
    }
}
