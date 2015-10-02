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
package org.eclipse.che.ide.project;

import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.project.shared.dto.ProjectReference;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.project.node.HasStorablePath.StorablePath;

/**
 * @author Vlad Zhukovskiy
 */
public class StorablePathResolver {
    public static HasStorablePath resolve(Object possibleDto) {
        if (possibleDto instanceof ItemReference) {
            return new StorablePath(((ItemReference)possibleDto).getPath());
        } else if (possibleDto instanceof ProjectDescriptor) {
            return new StorablePath(((ProjectDescriptor)possibleDto).getPath());
        } else if (possibleDto instanceof ProjectReference) {
            return new StorablePath(((ProjectReference)possibleDto).getPath());
        } else {
            return null;
        }
    }
}
