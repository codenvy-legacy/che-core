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
package org.eclipse.che.ide.projecttype.wizard;

import org.eclipse.che.ide.projecttype.wizard.categoriespage.CategoriesPageViewImpl;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

import org.eclipse.che.ide.projecttype.wizard.categoriespage.CategoriesPageViewImpl;
import org.vectomatic.dom.svg.ui.SVGResource;

/**
 * @author Ann Shumilova
 */
public interface ProjectWizardResources extends ClientBundle {

    public interface Css extends CssResource {
        String buttonPanel();

        String button();

        String rightButton();

        /**
         * @deprecated use {@link #buttonPrimary()} instead
         */
        @Deprecated
        String blueButton();

        // Primary
        String buttonPrimary();

        // Success
        String buttonSuccess();

        String inputError();
    }

    @Source({"Wizard.css", "org/eclipse/che/ide/api/ui/style.css"})
    Css wizardCss();

    @Source({"categoriespage/MainPage.css", "org/eclipse/che/ide/api/ui/style.css", "org/eclipse/che/ide/ui/Styles.css"})
    CategoriesPageViewImpl.Style mainPageStyle();

    @Source("runnerspage/environment.svg")
    SVGResource environment();
}
