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
package org.eclipse.che.api.git;

import org.eclipse.che.api.git.exception.*;
import org.eclipse.che.api.git.shared.ConflictExceptionError;
import org.eclipse.che.dto.server.DtoFactory;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static org.eclipse.che.dto.server.DtoFactory.newDto;

/**
 * Exception mapper for all GIT exceptions
 *
 * @author Yossi Balan (yossi.balan@sap.com)
 */
@Provider
@Singleton
public class GitExceptionMapper implements ExceptionMapper<GitException> {

    /**
     * check the exception type and build Response with the status body and the type of the error
     */
    @Override
    public Response toResponse(GitException exception) {
        if (exception instanceof GitRefNotFoundException)
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(DtoFactory.getInstance().toJson(exception.getServiceError()))
                    .type(MediaType.APPLICATION_JSON).build();
        else if (exception instanceof GitInvalidRefNameException)
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(DtoFactory.getInstance().toJson(exception.getServiceError()))
                    .type(MediaType.APPLICATION_JSON).build();
        else if (exception instanceof GitRefAlreadyExistsException)
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(DtoFactory.getInstance().toJson(exception.getServiceError()))
                    .type(MediaType.APPLICATION_JSON).build();
        else if (exception instanceof GitConflictException) {
            ConflictExceptionError conflictExceptionError = newDto(ConflictExceptionError.class).withMessage(exception.getServiceError().getMessage())
                    .withConflictingPaths(((GitConflictException) exception).getConflictPaths());
            return Response.status(Response.Status.CONFLICT)
                    .entity(DtoFactory.getInstance().toJson(conflictExceptionError))
                    .type(MediaType.APPLICATION_JSON).build();
        }
        return Response.serverError()
                .entity(DtoFactory.getInstance()
                        .toJson(exception.getServiceError()))
                .type(MediaType.APPLICATION_JSON).build();
    }

}
