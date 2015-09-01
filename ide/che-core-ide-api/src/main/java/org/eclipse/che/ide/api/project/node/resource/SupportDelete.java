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

import javax.annotation.Nullable;

/**
 * Indicates node which allow deletion.
 *
 * @author Vlad Zhukovskiy
 */
public interface SupportDelete<DataObject> {
    /**
     * Return delete processor. To detail information {@see DeleteProcessor}.
     *
     * @return delete processor
     */
    @Nullable
    DeleteProcessor<DataObject> getDeleteProcessor();

    /**
     * Perform delete operation.
     */
    void delete();
}
