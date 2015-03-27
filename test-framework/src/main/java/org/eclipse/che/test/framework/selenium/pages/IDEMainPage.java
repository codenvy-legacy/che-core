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
package org.eclipse.che.test.framework.selenium.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * The main webdrive page object for driving Codenvy IDE
 */
public class IDEMainPage {

    protected WebDriver driver;

    public IDEMainPage(WebDriver driver) {
        this.driver = driver;
    }

    public WebElement getTab(String tabName) {
        return new WebDriverWait(driver, 30).until(ExpectedConditions.presenceOfElementLocated(By.id("gwt-debug-tabButton-" + tabName)));
    }

    public WebElement getMainMenuItem(String mainMenuItemName) {
        return new WebDriverWait(driver, 30).until(ExpectedConditions.presenceOfElementLocated(By.id("gwt-debug-MainMenu/"
                                                                                                     + mainMenuItemName + "-true")));
    }

    public WebElement getMainMenuAction(String menuPath) {
        return new WebDriverWait(driver, 30).until(ExpectedConditions.elementToBeClickable(By.id("topmenu/" + menuPath)));
    }
}
