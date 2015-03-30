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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.concordion.ext.ScreenshotTaker;
import org.concordion.ext.ScreenshotUnavailableException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.events.EventFiringWebDriver;

import java.io.IOException;
import java.io.OutputStream;

public class SeleniumScreenshotTaker implements ScreenshotTaker {

    public static final Log log = LogFactory.getLog(SeleniumScreenshotTaker.class);

    private final WebDriver driver;

    public SeleniumScreenshotTaker(WebDriver driver) {
        WebDriver baseDriver = driver;
        while (baseDriver instanceof EventFiringWebDriver) {
            baseDriver = ((EventFiringWebDriver)baseDriver).getWrappedDriver();
        }
        this.driver = baseDriver;
    }

    @Override
    public int writeScreenshotTo(OutputStream outputStream) throws IOException {
        byte[] screenshot;
        try {
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.BYTES);
        } catch (ClassCastException e) {
            throw new ScreenshotUnavailableException("driver does not implement TakesScreenshot");
        }
        outputStream.write(screenshot);
        try {
            return ((Long)((JavascriptExecutor)driver).executeScript("return document.body.clientWidth")).intValue() + 2; // window.outerWidth"));
        } catch (Exception e) {
            log.warn("Failed taking screenshot", e);
        }
        return 0;
    }

    @Override
    public String getFileExtension() {
        return "png";
    }
}
