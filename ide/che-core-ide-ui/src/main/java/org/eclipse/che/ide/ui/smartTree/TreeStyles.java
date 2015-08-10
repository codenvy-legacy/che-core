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
package org.eclipse.che.ide.ui.smartTree;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

import org.vectomatic.dom.svg.ui.SVGResource;

/**
 * @author Vlad Zhukovskiy
 */
public interface TreeStyles extends ClientBundle {
    public interface CSS extends CssResource {
        String noFocusOutline();

        String rootContainer();

        String nodeContainer();

        String jointContainer();

        String iconContainer();

        String presentableTextContainer();

        String infoTextContainer();

        String descendantsContainer();

        String selected();

        String hover();

        String joint();

        String dragOver();

        String loadIconContainer();

        String tree();
    }

    @Source("TreeStyles.css")
    CSS styles();

    @Source("iconCollapsed.svg")
    SVGResource iconCollapsed();

    @Source("iconExpanded.svg")
    SVGResource iconExpanded();

    @Source("iconLoading.svg")
    SVGResource iconLoading();
}
