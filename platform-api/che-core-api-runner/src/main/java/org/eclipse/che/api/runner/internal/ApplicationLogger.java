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
package org.eclipse.che.api.runner.internal;

import org.eclipse.che.api.core.util.LineConsumer;

import java.io.IOException;

import javax.ws.rs.core.MediaType;

/**
 * Collects application logs. A ApplicationLogger is open after creation, and may consumes applications logs with method {@link
 * #writeLine(String)}. Once a ApplicationLogger is closed, any attempt to write new lines upon it will cause a {@link java.io.IOException}
 * to be thrown, but closing should not prevent to get logs with {@link #getLogs(Appendable)}.
 *
 * @author andrew00x
 */
public interface ApplicationLogger extends LineConsumer {
    /**
     * Get application logs.
     *
     * @param output
     *         output for logs
     * @throws java.io.IOException
     *         if an i/o errors occur
     */
    void getLogs(Appendable output) throws IOException;

    /**
     * Get content type of application logs.
     *
     * @return content type
     */
    String getContentType();

    /** Dummy {@code ApplicationLogger} implementation. */
    ApplicationLogger DUMMY = new ApplicationLogger() {
        @Override
        public void getLogs(Appendable output) {
        }

        @Override
        public String getContentType() {
            return MediaType.TEXT_PLAIN;
        }

        @Override
        public void writeLine(String line) {
        }

        @Override
        public void close() {
        }
    };
}
