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
package org.eclipse.che.test.framework.concordion;

import org.concordion.api.extension.ConcordionExtender;
import org.concordion.ext.ScreenshotExtension;
import org.openqa.selenium.WebDriver;

/**
 * An concordion extension that set up screenshots and js resources for markdown.
 */
public class CodenvyConcordionExtension extends CodenvyConcordionResourceExtension {

    ScreenshotExtension ssextension = new ScreenshotExtension().setScreenshotOnAssertionSuccess(true);

    public CodenvyConcordionExtension setSeleniumDriver(WebDriver driver) {
        ssextension.setScreenshotTaker(new SeleniumScreenshotTaker(driver));
        return this;
    }

    public void addTo(ConcordionExtender concordionExtender) {
        ssextension.addTo(concordionExtender);
        super.addTo(concordionExtender);
    }
}
