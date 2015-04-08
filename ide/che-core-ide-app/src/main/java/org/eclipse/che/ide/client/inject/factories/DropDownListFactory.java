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
package org.eclipse.che.ide.client.inject.factories;

import org.eclipse.che.ide.dropdown.ListHeaderWidget;
import org.eclipse.che.ide.dropdown.SimpleListElementAction;
import org.vectomatic.dom.svg.ui.SVGResource;

import javax.annotation.Nonnull;

/**
 * The factory for creating drop down list.
 *
 * @author Valeriy Svydenko
 */
public interface DropDownListFactory {
    /**
     * Create an instance of {@link ListHeaderWidget} with a given identifier for registering.
     *
     * @param listId
     *         list identifier
     * @return an instance of {@link ListHeaderWidget}
     */
    @Nonnull
    ListHeaderWidget createList(@Nonnull String listId);

    /**
     * Create an instance of {@link SimpleListElementAction} with given name amd icon for displaying it and header which is configured this
     * element.
     *
     * @param name
     *         name of action
     * @param image
     *         icon of action
     * @param header
     *         header widget of custom list
     * @return
     */
    @Nonnull
    SimpleListElementAction createElement(@Nonnull String name, @Nonnull SVGResource image, @Nonnull ListHeaderWidget header);
}
