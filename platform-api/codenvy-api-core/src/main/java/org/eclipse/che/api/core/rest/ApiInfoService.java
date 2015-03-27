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
package org.eclipse.che.api.core.rest;

import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.rest.annotations.OPTIONS;
import org.eclipse.che.api.core.rest.shared.dto.ApiInfo;
import org.eclipse.che.dto.server.DtoFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.ws.rs.Path;
import java.io.File;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * @author andrew00x
 */
@Path("/")
@Singleton
public class ApiInfoService {
    private static final Logger LOG = LoggerFactory.getLogger(ApiInfoService.class);

    private volatile ApiInfo apiInfo;

    @OPTIONS
    public ApiInfo info() throws ServerException {
        ApiInfo myApiInfo = apiInfo;
        if (myApiInfo == null) {
            apiInfo = myApiInfo = readApiInfo();
        }
        return myApiInfo;
    }

    private ApiInfo readApiInfo() throws ServerException {
        try {
            URL url = ApiInfoService.class.getProtectionDomain().getCodeSource().getLocation();
            try (JarFile jar = new JarFile(new File(url.toURI()))) {
                final Manifest manifest = jar.getManifest();
                final Attributes mainAttributes = manifest.getMainAttributes();
                final DtoFactory dtoFactory = DtoFactory.getInstance();
                return dtoFactory.createDto(ApiInfo.class)
                                 .withSpecificationVendor(mainAttributes.getValue("Specification-Vendor"))
                                 .withImplementationVendor(mainAttributes.getValue("Implementation-Vendor"))
                                 .withSpecificationTitle("Codenvy REST API")
                                 .withSpecificationVersion(mainAttributes.getValue("Specification-Version"))
                                 .withImplementationVersion(mainAttributes.getValue("Implementation-Version"))
                                 .withScmRevision(mainAttributes.getValue("SCM-Revision"))
                                 .withIdeVersion(mainAttributes.getValue("IDE-Version"));
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new ServerException("Unable read info about API. Contact support for assistance.");
        }
    }
}
