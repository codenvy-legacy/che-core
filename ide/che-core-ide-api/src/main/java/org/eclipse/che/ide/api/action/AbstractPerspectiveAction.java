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
package org.eclipse.che.ide.api.action;

import com.google.gwt.resources.client.ImageResource;

import org.eclipse.che.ide.api.parts.PerspectiveManager;
import org.vectomatic.dom.svg.ui.SVGResource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * The class contains general business logic for all actions displaying of which depend on current perspective.All actions must
 * extend this class if their displaying depend on changing of perspective.
 *
 * @author Dmitry Shnurenko
 */
public abstract class AbstractPerspectiveAction extends Action {

    private final List<String> activePerspectives;

    public AbstractPerspectiveAction(@Nonnull List<String> activePerspectives,
                                     @Nonnull String tooltip,
                                     @Nonnull String description,
                                     @Nullable ImageResource resource,
                                     @Nullable SVGResource icon) {
        super(tooltip, description, resource, icon);
        this.activePerspectives = activePerspectives;
    }

    /** {@inheritDoc} */
    @Override
    public final void update(@Nonnull ActionEvent event) {
        PerspectiveManager manager = event.getPerspectiveManager();

        Presentation presentation = event.getPresentation();

        boolean isActivePerspective = activePerspectives.contains(manager.getPerspectiveId());

        presentation.setEnabledAndVisible(isActivePerspective);

        if (isActivePerspective) {
            updatePerspective(event);
        }
    }

    /**
     * Updates displaying of action within current perspective.
     *
     * @param event
     *         update action
     */
    public abstract void updatePerspective(@Nonnull ActionEvent event);
}
