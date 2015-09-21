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
package org.eclipse.che.ide.createworkspace.tagentry;

import org.eclipse.che.api.machine.shared.dto.recipe.RecipeDescriptor;
import org.eclipse.che.ide.api.mvp.View;

/**
 * Provides methods which allows get information about tag.
 *
 * @author Dmitry Shnurenko
 */
public interface TagEntry extends View<TagEntry.ActionDelegate> {

    /** Returns descriptor which contains information about recipe. */
    RecipeDescriptor getDescriptor();

    /**
     * This method need to set necessary styles to tag component. This styles don't set via ui binder because it doesn't load yet
     * when styles are necessary.
     */
    void setStyles();

    interface ActionDelegate {
        /**
         * Performs some actions when user clicks on tag.
         *
         * @param tag
         *         tag which was selected
         */
        void onTagClicked(TagEntry tag);
    }
}
