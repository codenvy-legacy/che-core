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
package org.eclipse.che.ide.commons.exception;

import org.eclipse.che.ide.rest.AsyncRequest;

import com.google.gwt.http.client.Response;


/** @author <a href="mailto:gavrikvetal@gmail.com">Vitaliy Gulyy</a> */

@SuppressWarnings("serial")
public class UnauthorizedException extends Exception {

    private Response response;

    private AsyncRequest request;

    public UnauthorizedException(Response response, AsyncRequest request) {
        this.response = response;
        this.request = request;
    }

    public Response getResponse() {
        return response;
    }

    public AsyncRequest getRequest() {
        return request;
    }

}
