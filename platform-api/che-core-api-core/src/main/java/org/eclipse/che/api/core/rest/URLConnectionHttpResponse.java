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
package org.eclipse.che.api.core.rest;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import javax.net.ssl.HttpsURLConnection;

/**
 * An {@link HttpResponse} implementation that is based on {@link HttpURLConnection} and {@link HttpsURLConnection}
 * connections.
 * 
 * @author Tareq Sharafy
 *
 */
public class URLConnectionHttpResponse implements HttpResponse {

    private final HttpURLConnection conn;

    public URLConnectionHttpResponse(HttpURLConnection conn) {
        this.conn = conn;
    }

    @Override
    public void close() {
        conn.disconnect();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        InputStream errStm = conn.getErrorStream();
        return errStm != null ? errStm : conn.getInputStream();
    }

    @Override
    public int getResponseCode() throws IOException {
        return conn.getResponseCode();
    }

    @Override
    public String getHeaderField(String name) {
        return conn.getHeaderField(name);
    }

    @Override
    public String getContentType() {
        return conn.getContentType();
    }

}
