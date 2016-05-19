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
package org.eclipse.che.ide.commons.exception;

import org.eclipse.che.ide.rest.HTTPHeader;
import com.google.gwt.http.client.Response;

/** @author <a href="mailto:gavrikvetal@gmail.com">Vitaliy Gulyy</a> */

@SuppressWarnings("serial")
public class ServerException extends Exception {

    private Response response;

    private String msg = "";

    private boolean errorMessageProvided;

    public ServerException(Response response) {
        this.response = response;
        this.msg = "";
        this.errorMessageProvided = checkErrorMessageProvided();
    }

    public ServerException(Response response, String msg) {
        this.response = response;
        this.msg = msg;
    }

    public int getHTTPStatus() {
        return response.getStatusCode();
    }

    public String getStatusText() {
        return response.getStatusText();
    }

    @Override
    public String getMessage() {
        if (response.getText().length() > 0)
            return msg + response.getText();
        else
            return msg + response.getStatusText();
    }

    public String getHeader(String key) {
        return response.getHeader(key);
    }

    private boolean checkErrorMessageProvided() {
        String value = response.getHeader(HTTPHeader.JAXRS_BODY_PROVIDED);
        if (value != null) {
            return true;
        }

        return false;
    }

    public boolean isErrorMessageProvided() {
        return errorMessageProvided;
    }
}
