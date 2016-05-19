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
package org.eclipse.che.api.promises.client;

import org.eclipse.che.commons.annotation.Nullable;


public interface PromiseError {

    /**
     * Returns the error message.
     * 
     * @return the message
     */
    @Nullable
    String getMessage();

    /**
     * Returns the error cause.
     * 
     * @return the cause
     */
    @Nullable
    Throwable getCause();
}
