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
package org.eclipse.che.ide.projectimport;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

import org.eclipse.che.ide.projectimport.local.LocalZipImporterPageViewImpl;
import org.eclipse.che.ide.projectimport.zip.ZipImporterPageViewImpl;

/**
 * @author Roman Nikitenko
 */
public interface ProjectImporterResource extends ClientBundle {

    public interface Css extends CssResource {
        String inputError();
    }

    @Source({"org/eclipse/che/ide/projectimport/ImporterPage.css", "org/eclipse/che/ide/ui/Styles.css"})
    ZipImporterPageViewImpl.Style zipImporterPageStyle();

    @Source({"org/eclipse/che/ide/projectimport/ImporterPage.css", "org/eclipse/che/ide/ui/Styles.css"})
    LocalZipImporterPageViewImpl.Style localZipImporterPageStyle();
}
