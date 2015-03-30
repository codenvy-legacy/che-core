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
package org.eclipse.che.api.vfs.gwt.client;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.vfs.shared.dto.Item;
import org.eclipse.che.api.vfs.shared.dto.ReplacementSet;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.rest.AsyncRequestCallback;

import javax.annotation.Nonnull;

/**
 * GWT Client for VFS Service.
 *
 * @author Sergii Leschenko
 * @author Artem Zatsarynnyy
 */
public interface VfsServiceClient {
    public void replaceInCurrentWorkspace(@Nonnull ProjectDescriptor project,
                                          Array<ReplacementSet> replacementSets,
                                          AsyncRequestCallback<Void> callback);

    public void getItemByPath(@Nonnull String path,
                              AsyncRequestCallback<Item> callback);
}
