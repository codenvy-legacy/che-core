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
package org.eclipse.che.ide.createworkspace;

import org.eclipse.che.api.machine.shared.dto.recipe.RecipeDescriptor;
import org.eclipse.che.ide.createworkspace.tagentry.TagEntry;

/**
 * Special factory which allows creating different tags.
 *
 * @author Dmitry Shnurenko
 */
public interface TagEntryFactory {

    /**
     * Creates view representation of tag using special descriptor.
     *
     * @param descriptor
     *         descriptor which contains all information about tag
     * @return an instance of {@link TagEntry}
     */
    TagEntry create(RecipeDescriptor descriptor);
}
