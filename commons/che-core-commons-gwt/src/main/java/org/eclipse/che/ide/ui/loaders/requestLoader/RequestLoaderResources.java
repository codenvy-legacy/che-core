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
package org.eclipse.che.ide.ui.loaders.requestLoader;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.DataResource;
import com.google.gwt.resources.client.DataResource.MimeType;

/**
 * Resources for request loader.
 *
 * @author Andrey Plotnikov
 * @author Oleksii Orel
 */
public interface RequestLoaderResources extends ClientBundle {

    interface LoaderCss extends CssResource {
        String loader();

        String glassStyle();
                                                                                                                                                
        String pinionPanel();

        String textField();

        String hide();
    }

    @MimeType("image/png")
    @Source("pinion-icon.png")
    DataResource pinionIcon();

    @Source({"RequestLoader.css", "org/eclipse/che/ide/api/ui/style.css"})
    LoaderCss Css();
}
