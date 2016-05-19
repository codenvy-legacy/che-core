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
package org.eclipse.che.security.oauth1;

/**
 * Exception raised when the OAuth authentication failed.
 *
 * @author Kevin Pollet
 */
public final class OAuthAuthenticationException extends Exception {
    /**
     * Constructs an instance of {@link OAuthAuthenticationException}.
     *
     * @param message
     *         the exception message.
     */
    public OAuthAuthenticationException(final String message) {
        super(message);
    }

    /**
     * Constructs an instance of {@link OAuthAuthenticationException}.
     *
     * @param cause
     *         the cause of the exception.
     */
    public OAuthAuthenticationException(final Throwable cause) {
        super(cause);
    }
}
