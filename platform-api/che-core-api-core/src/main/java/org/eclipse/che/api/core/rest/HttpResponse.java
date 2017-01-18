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

import com.google.common.annotations.Beta;

/**
 * Defines response of {@link HttpRequestFactory}.
 * 
 * @author Tareq Sharafy
 */
@Beta
public interface HttpResponse extends AutoCloseable {

    /**
     * Returns a response code.
     */
    int getResponseCode() throws IOException;

    /**
     * The content type of the response data.
     */
    String getHeaderField(String name);

    /**
     * The value of the Content-Type header.
     */
    String getContentType();

    /**
     * Gets a stream of the response body.
     */
    InputStream getInputStream() throws IOException;

    @Override
    void close() throws IOException;

}
