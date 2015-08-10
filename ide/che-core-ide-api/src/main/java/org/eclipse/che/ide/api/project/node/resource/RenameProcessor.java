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
package org.eclipse.che.ide.api.project.node.resource;

import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.api.project.node.HasDataObject;

import javax.annotation.Nonnull;

/**
 * Provide mechanism to allow physically renaming data object during node rename.
 *
 * @author Vlad Zhukovskiy
 */
public interface RenameProcessor<DataObject> {
    /**
     * Rename data object stored in node.
     *
     * @param node
     *         data store node
     * @param newName
     *         new name for data object
     * @return promise with updated data object
     */
    Promise<DataObject> rename(@Nonnull HasDataObject<DataObject> node, @Nonnull String newName);
}
