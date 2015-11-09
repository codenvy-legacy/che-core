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
package org.eclipse.che.ide.project.shared;

import com.google.common.base.Strings;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.project.node.HasProjectDescriptor;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.project.node.ProjectDescriptorNode;

import javax.validation.constraints.NotNull;

/**
 * Helper methods for Content Root manipulations.
 *
 * @author Vlad Zhukovskiy
 */
public class ContentRootHelper {
    /**
     * Get "Content Root" path from Node or null.
     * Content root means path where project explorer should show own contents as root content.
     * For example if we have this structure:
     * .
     * |-src
     * |---main
     * |-----java
     * |-----resources
     * |---test
     * |-----java
     * |-target
     * <p/>
     * and we want to display path src/main/java as root directory than we should set this path
     * in project descriptor as "Content Root". For more details see how work "Go Into" feature
     * in Eclipse IDE.
     *
     * @param node
     *         node to process, should be {@link ProjectDescriptorNode}
     * @return content root path or null
     */
    public static String getRootOrNull(Node node) {
        if (node instanceof ProjectDescriptorNode && isValidContentRoot((HasProjectDescriptor)node)) {
            ProjectDescriptor descriptor = ((HasProjectDescriptor)node).getProjectDescriptor();
            String rawContentRoot = descriptor.getContentRoot();

            return descriptor.getPath() + (rawContentRoot.startsWith("/") ? rawContentRoot : "/" + rawContentRoot);
        }

        return null;
    }

    private static boolean isValidContentRoot(@NotNull HasProjectDescriptor node) {
        return !Strings.isNullOrEmpty(node.getProjectDescriptor().getContentRoot()); //TODO maybe add more checks
    }
}
