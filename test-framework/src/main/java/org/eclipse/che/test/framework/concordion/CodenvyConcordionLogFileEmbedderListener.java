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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

import org.concordion.api.Element;
import org.concordion.api.Resource;
import org.concordion.api.Target;
import org.concordion.api.listener.ConcordionBuildEvent;
import org.concordion.api.listener.ConcordionBuildListener;
import org.concordion.api.listener.SpecificationProcessingEvent;
import org.concordion.api.listener.SpecificationProcessingListener;

/**
 * Will embed logs/catalina.out to the specs if it can find it. Instance of this class is to be added with a Concordion extension as
 * SpecificationProcessingListener and ConcordionBuildListener listeners
 */
public class CodenvyConcordionLogFileEmbedderListener implements SpecificationProcessingListener, ConcordionBuildListener {

    protected Target   target;
    protected Resource resource;

    @Override
    public void concordionBuilt(ConcordionBuildEvent event) {
        target = event.getTarget();
    }

    @Override
    public void beforeProcessingSpecification(SpecificationProcessingEvent event) {
        resource = event.getResource();
    }

    @Override
    public void afterProcessingSpecification(SpecificationProcessingEvent event) {
        String serverLocation = System.getProperty("codenvy.ide.path");
        if (serverLocation == null) {
            return;
        }
        String logFileLocation = serverLocation + "/logs/catalina.out";
        File originalLogFile = new File(logFileLocation);
        if (!originalLogFile.exists()) {
            return;
        }

        String relativeCopiedLogFilePath = "catalina-" + System.currentTimeMillis() + ".out";
        Resource imageResource = resource.getRelativeResource(relativeCopiedLogFilePath);
        try {
            OutputStream outputStream = target.getOutputStream(imageResource);
            Files.copy(originalLogFile.toPath(), outputStream);
        } catch (IOException e) {
            return;
        }


        Element a = new Element("a");
        a.addAttribute("href", relativeCopiedLogFilePath);
        a.appendText("Server log file");
        event.getRootElement().getFirstDescendantNamed("body").appendChild(a);

    }

}
