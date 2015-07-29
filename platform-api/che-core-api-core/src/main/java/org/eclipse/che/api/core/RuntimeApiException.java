/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2014] Codenvy, S.A. 
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package org.eclipse.che.api.core;

import org.eclipse.che.api.core.rest.shared.dto.ServiceError;
import org.eclipse.che.dto.server.DtoFactory;

/**
 * Base class for all unchecked API errors.
 *
 * @author Alexander Garagatyi
 */
public class RuntimeApiException extends RuntimeException {
    private final ServiceError serviceError;

    public RuntimeApiException(ServiceError serviceError) {
        super(serviceError.getMessage());
        this.serviceError = serviceError;
    }

    public RuntimeApiException(String message) {
        super(message);

        this.serviceError = createError(message);
    }

    public RuntimeApiException(String message, Throwable cause) {
        super(message, cause);
        this.serviceError = createError(message);
    }

    public RuntimeApiException(Throwable cause) {
        super(cause);
        this.serviceError = createError(cause.getMessage());
    }

    public ServiceError getServiceError() {
        return serviceError;
    }

    private ServiceError createError(String message) {
        return DtoFactory.getInstance().createDto(ServiceError.class).withMessage(message);
    }
}
