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
package org.eclipse.che.api.core.util;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.eclipse.che.api.core.rest.HttpJsonRequestFactory;
import org.eclipse.che.api.core.rest.HttpResponse;

/**
 * DownloadPlugin that downloads single file.
 *
 * @author Tareq Sharafy
 */
@Singleton
public class DownloadPluginImpl extends HttpDownloadPlugin {

    private static final int READ_TIMEOUT = (int) TimeUnit.MINUTES.toMillis(3);

    private final HttpJsonRequestFactory provider;

    public DownloadPluginImpl(HttpJsonRequestFactory provider) {
        this.provider = provider;
    }

    @Override
    protected HttpResponse openUrlConnection(String downloadUrl) throws IOException {
        // Check it
        HttpResponse conn = provider.fromUrl(downloadUrl).setTimeout(READ_TIMEOUT).requestGeneral();
        // Connect
        final int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            conn.close();
            throw new IOException(String.format("Invalid response status %d from remote server. ", responseCode));
        }
        return conn;
    }

}
