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
package org.eclipse.che.ide.ui.loaders;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.DataResource;
import com.google.gwt.resources.client.DataResource.MimeType;

import org.vectomatic.dom.svg.ui.SVGResource;

/**
 * Resources for loader.
 *
 * @author <a href="mailto:aplotnikov@codenvy.com">Andrey Plotnikov</a>
 */
public interface LoaderResources extends ClientBundle {

    interface PinionCss extends CssResource {
        String pinion();
        String expandControl();
        String expandedImage();
        String errorStatus();
        String successStatus();
        String inProgressStatus();
    }

    @MimeType("image/png")
    @Source("pinion-icon.png")
    DataResource pinionIcon();

    @Source({"org/eclipse/che/ide/ui/constants.css", "IdeLoader.css", "org/eclipse/che/ide/api/ui/style.css"})
    PinionCss Css();

    @Source("org/eclipse/che/ide/ui/loaders/initializationLoader/expansionIcon.svg")
    SVGResource expansionImage();

}
