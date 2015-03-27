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
package org.eclipse.che.test.framework.itests;


import org.eclipse.che.test.framework.AbstractIntegrationTest;
import org.eclipse.che.test.framework.selenium.pages.IDEMainPage;

import org.openqa.selenium.support.PageFactory;

/**
 * Codenvy SDK basic integration test, see specs reports in src/main/resources/com/codenvy/test/framework/itests/ServerStarted.html
 */
public class ServerStarted extends AbstractIntegrationTest {

    protected IDEMainPage mainPage;

    public String access(String url) {
        driver.get(url);
        mainPage = PageFactory.initElements(driver, IDEMainPage.class);
        return "access";
    }

    public String displayExplorerTab() {
        return mainPage.getTab("Explorer").getText();
    }

    public String displayFileMenu() {
        return mainPage.getMainMenuItem("fileGroup").getText();
    }

}
