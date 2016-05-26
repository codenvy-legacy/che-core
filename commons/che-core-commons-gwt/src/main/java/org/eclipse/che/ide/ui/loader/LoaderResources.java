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
package org.eclipse.che.ide.ui.loader;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.DataResource;
import com.google.gwt.resources.client.DataResource.MimeType;

/**
 * Resources for loader.
 *
 * @author <a href="mailto:aplotnikov@codenvy.com">Andrey Plotnikov</a>
 */
public interface LoaderResources extends ClientBundle {

    public interface PinionCss extends CssResource {
        String pinion();
    }

    @MimeType("image/png")
    @Source("org/eclipse/che/ide/ui/loader/pinion-icon.png")
    DataResource pinionIcon();

    @Source({"org/eclipse/che/ide/common/constants.css", "org/eclipse/che/ide/ui/loader/IdeLoader.css", "org/eclipse/che/ide/api/ui/style.css"})
    PinionCss Css();

}
