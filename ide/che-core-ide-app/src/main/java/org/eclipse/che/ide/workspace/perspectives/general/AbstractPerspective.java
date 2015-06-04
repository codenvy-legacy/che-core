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
package org.eclipse.che.ide.workspace.perspectives.general;

import org.eclipse.che.ide.api.constraints.Constraints;
import org.eclipse.che.ide.api.mvp.Presenter;
import org.eclipse.che.ide.api.parts.EditorPartStack;
import org.eclipse.che.ide.api.parts.PartPresenter;
import org.eclipse.che.ide.api.parts.PartStack;
import org.eclipse.che.ide.api.parts.PartStackType;
import org.eclipse.che.ide.api.parts.PartStackView;
import org.eclipse.che.ide.workspace.PartStackPresenterFactory;
import org.eclipse.che.ide.workspace.PartStackViewFactory;
import org.eclipse.che.ide.workspace.WorkBenchControllerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.che.ide.api.parts.PartStackType.INFORMATION;
import static org.eclipse.che.ide.api.parts.PartStackType.NAVIGATION;
import static org.eclipse.che.ide.api.parts.PartStackType.TOOLING;
import static org.eclipse.che.ide.api.parts.PartStackView.TabPosition.BELOW;
import static org.eclipse.che.ide.api.parts.PartStackView.TabPosition.LEFT;
import static org.eclipse.che.ide.api.parts.PartStackView.TabPosition.RIGHT;

/**
 * The class which contains general business logic for all perspectives.
 *
 * @author Dmitry Shnurenko
 */
public abstract class AbstractPerspective implements Presenter, Perspective {

    protected final Map<PartStackType, PartStack> partStacks;
    protected final PerspectiveViewImpl           view;

    private List<PartPresenter> activeParts;

    protected AbstractPerspective(@Nonnull PerspectiveViewImpl view,
                                  @Nonnull PartStackPresenterFactory stackPresenterFactory,
                                  @Nonnull PartStackViewFactory partViewFactory,
                                  @Nonnull WorkBenchControllerFactory controllerFactory) {
        this.view = view;
        this.partStacks = new HashMap<>();

        PartStackView navigationView = partViewFactory.create(LEFT, view.getLeftPanel());
        PartStack navigationPartStack = stackPresenterFactory.create(navigationView,
                                                                     controllerFactory.createController(view.getSplitPanel(),
                                                                                                        view.getNavigationPanel()));
        partStacks.put(NAVIGATION, navigationPartStack);

        PartStackView informationView = partViewFactory.create(BELOW, view.getBottomPanel());
        PartStack informationStack = stackPresenterFactory.create(informationView,
                                                                  controllerFactory.createController(view.getSplitPanel(),
                                                                                                     view.getInformationPanel()));
        partStacks.put(INFORMATION, informationStack);

        PartStackView toolingView = partViewFactory.create(RIGHT, view.getRightPanel());
        PartStack toolingPartStack = stackPresenterFactory.create(toolingView, controllerFactory.createController(view.getSplitPanel(),
                                                                                                                  view.getToolPanel()));
        partStacks.put(TOOLING, toolingPartStack);
    }

    /** {@inheritDoc} */
    @Override
    public void removePart(@Nonnull PartPresenter part) {
        PartStack destPartStack = findPartStackByPart(part);
        if (destPartStack != null) {
            destPartStack.removePart(part);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void hidePart(@Nonnull PartPresenter part) {
        PartStack destPartStack = findPartStackByPart(part);
        if (destPartStack != null) {
            destPartStack.hidePart(part);
        }
    }

    /** Expands all editors parts. */
    public void expandEditorPart() {
        activeParts = new ArrayList<>();
        for (PartStack value : partStacks.values()) {
            if (value instanceof EditorPartStack) {
                continue;
            }

            PartPresenter part = value.getActivePart();
            if (part != null) {
                value.hidePart(part);
                activeParts.add(part);
            }
        }
    }

    /** Restores editor parts. */
    public void restoreEditorPart() {
        for (PartPresenter activePart : activeParts) {
            PartStack destPartStack = findPartStackByPart(activePart);
            PartPresenter newPart = destPartStack.getActivePart();

            if (newPart == null) {
                destPartStack.setActivePart(activePart);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setActivePart(@Nonnull PartPresenter part) {
        PartStack destPartStack = findPartStackByPart(part);
        if (destPartStack != null) {
            destPartStack.setActivePart(part);
        }
    }

    /**
     * Find parent PartStack for given Part
     *
     * @param part
     *         part for which need find parent
     * @return Parent PartStackPresenter or null if part not registered
     */
    public PartStack findPartStackByPart(@Nonnull PartPresenter part) {
        for (PartStackType partStackType : PartStackType.values()) {

            if (partStacks.get(partStackType).containsPart(part)) {
                return partStacks.get(partStackType);
            }
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void openPart(@Nonnull PartPresenter part, @Nonnull PartStackType type) {
        openPart(part, type, null);
    }

    /** {@inheritDoc} */
    @Override
    public void openPart(@Nonnull PartPresenter part, @Nonnull PartStackType type, @Nullable Constraints constraint) {
        PartStack destPartStack = partStacks.get(type);

        if (!destPartStack.containsPart(part)) {
            destPartStack.addPart(part, constraint);
        }
    }

    /** {@inheritDoc} */
    @Override
    @Nullable
    public PartStack getPartStack(@Nonnull PartStackType type) {
        return partStacks.get(type);
    }
}
