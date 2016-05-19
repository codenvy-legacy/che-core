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
package org.eclipse.che.ide.project.shared;

import com.google.gwt.resources.client.ClientBundle;

import org.vectomatic.dom.svg.ui.SVGResource;

/**
 * @author Vlad Zhukovskiy
 */
public interface NodesResources extends ClientBundle {
    @Source("file.svg")
    SVGResource file();

    @Source("invalidProjectRoot-old.svg")
    SVGResource invalidProjectRoot();

    @Source("moduleRoot-old.svg")
    SVGResource moduleRoot();

    @Source("projectRoot-old.svg")
    SVGResource projectRoot();

    @Source("simpleRoot-old.svg")
    SVGResource simpleRoot();
}
