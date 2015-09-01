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
package org.eclipse.che.ide.project.node;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.project.node.settings.NodeSettings;

import javax.annotation.Nonnull;

/**
 * Base class for the synthetic, non-resourced item
 *
 * @author Vlad Zhukovskiy
 */
public abstract class SyntheticBasedNode<DataObject> extends AbstractProjectBasedNode<DataObject> {

    public SyntheticBasedNode(@Nonnull DataObject dataObject,
                              @Nonnull ProjectDescriptor projectDescriptor,
                              @Nonnull NodeSettings nodeSettings) {
        super(dataObject, projectDescriptor, nodeSettings);
    }

}
