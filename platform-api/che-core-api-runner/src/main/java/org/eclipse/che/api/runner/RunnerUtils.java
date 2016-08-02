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
package org.eclipse.che.api.runner;

import java.io.IOException;

import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.core.rest.HttpJsonRequest;
import org.eclipse.che.api.core.rest.HttpJsonResponse;

/**
 * Utilities for runner operation
 * 
 * @author Tareq Sharafy
 *
 */
public class RunnerUtils {

	/**
	 * Invoke the {@link HttpJsonRequest#request()} of the given request object,
	 * wrapping any exceptions with a {@link RunnerException} properly.
	 */
	public static HttpJsonResponse runnerRequest(HttpJsonRequest req) throws RunnerException {
		try {
			return req.request();
		} catch (IOException e) {
			throw new RunnerException(e);
		} catch (ServerException | UnauthorizedException | ForbiddenException | NotFoundException | ConflictException
				| BadRequestException e) {
			throw new RunnerException(e.getServiceError());
		}
	}

}
