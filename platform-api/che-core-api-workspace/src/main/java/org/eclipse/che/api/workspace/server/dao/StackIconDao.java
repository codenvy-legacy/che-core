/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/

package org.eclipse.che.api.workspace.server.dao;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.workspace.server.stack.image.StackIcon;
import org.eclipse.che.commons.annotation.Nullable;

/**
 * Defines data access object for {@link StackIcon}
 *
 * @author Alexander Andrienko
 */
public interface StackIconDao {

    @Nullable
    StackIcon getIcon(String stackId);

    void save(StackIcon stackIcon) throws ServerException;

    void remove(String stackId) throws NotFoundException, ServerException;
}
