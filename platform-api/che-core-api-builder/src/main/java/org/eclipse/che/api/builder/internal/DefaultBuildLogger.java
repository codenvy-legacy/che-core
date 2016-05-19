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

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;

/**
 * File based implementation of BuildLogger.
 *
 * @author andrew00x
 */
public final class DefaultBuildLogger implements BuildLogger {
    private final java.io.File file;
    private final String       contentType;
    private final Writer       writer;
    private final boolean      autoFlush;

    public DefaultBuildLogger(java.io.File file, String contentType) throws IOException {
        this.file = file;
        this.contentType = contentType;
        autoFlush = true;
        writer = Files.newBufferedWriter(file.toPath(), Charset.defaultCharset());
    }

    @Override
    public Reader getReader() throws IOException {
        return Files.newBufferedReader(file.toPath(), Charset.defaultCharset());
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public java.io.File getFile() {
        return file;
    }

    @Override
    public void writeLine(String line) throws IOException {
        if (line != null) {
            writer.write(line);
        }
        writer.write('\n');
        if (autoFlush) {
            writer.flush();
        }
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    @Override
    public String toString() {
        return "DefaultBuildLogger{" +
               "file=" + file +
               '}';
    }
}
