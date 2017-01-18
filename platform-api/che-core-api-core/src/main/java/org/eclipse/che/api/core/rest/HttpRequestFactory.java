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

import javax.validation.constraints.NotNull;

import com.google.common.annotations.Beta;
import com.google.inject.ImplementedBy;

/**
 * Factory for {@link HttpRequest} instances.
 *
 * @author Yevhenii Voevodin
 */
@Beta
@ImplementedBy(DefaultHttpJsonRequestFactory.class)
public interface HttpRequestFactory {

    /**
     * Creates {@link HttpJsonRequest} based on {@code url}, with an initial HTTP method {@code GET}.
     *
     * @param url
     *            request target url
     * @return new instance of {@link HttpRequest}
     * @throws NullPointerException
     *             when url is null
     */
    HttpRequest target(@NotNull String url);

}
