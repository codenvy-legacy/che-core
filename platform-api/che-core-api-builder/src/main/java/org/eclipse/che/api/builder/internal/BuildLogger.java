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
package org.eclipse.che.api.builder.internal;

import org.eclipse.che.api.core.util.LineConsumer;

import java.io.IOException;
import java.io.Reader;

/**
 * Collects build logs. A BuildLogger is open after creation, and may consumes build logs with method {@link #writeLine(String)}. Once a
 * BuildLogger is closed, any attempt to write new logs upon it will cause a {@link java.io.IOException} to be thrown, but closing should
 * not prevent to get logs with {@link #getReader()}.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public interface BuildLogger extends LineConsumer {
    /**
     * Get Reader of build log.
     *
     * @return reader
     * @throws java.io.IOException
     *         if any i/o errors occur
     */
    Reader getReader() throws IOException;

    /**
     * Get content type of build logs.
     *
     * @return content type
     */
    String getContentType();

    /**
     * Get {@code File} is case if logs stored in file.
     *
     * @return {@code File} or {@code null} if BuildLogger does not use {@code File} as backend
     */
    java.io.File getFile();

    /** Dummy {@code BuildLogger} implementation. */
    BuildLogger DUMMY = new BuildLogger() {
        @Override
        public Reader getReader() {
            return new Reader() {
                @Override
                public int read(char[] buf, int off, int len) throws IOException {
                    return -1;
                }

                @Override
                public void close() throws IOException {
                }
            };
        }

        @Override
        public String getContentType() {
            return null;
        }

        @Override
        public java.io.File getFile() {
            return null;
        }

        @Override
        public void writeLine(String line) {
        }

        @Override
        public void close() {
        }
    };
}
