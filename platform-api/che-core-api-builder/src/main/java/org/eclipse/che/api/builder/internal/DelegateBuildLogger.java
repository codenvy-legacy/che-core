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

/**
 * Implementation of the {@code BuildLogger} which delegates log messages to underlying {@code BuildLogger}.
 *
 * @author andrew00x
 */
public abstract class DelegateBuildLogger implements BuildLogger {
    protected final BuildLogger delegate;

    public DelegateBuildLogger(BuildLogger delegate) {
        this.delegate = delegate;
    }

    @Override
    public Reader getReader() throws IOException {
        return delegate.getReader();
    }

    @Override
    public void writeLine(String line) throws IOException {
        delegate.writeLine(line);
    }

    @Override
    public String getContentType() {
        return delegate.getContentType();
    }

    @Override
    public java.io.File getFile() {
        return delegate.getFile();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public String toString() {
        return "DelegateBuildLogger{" +
               "delegate=" + delegate +
               '}';
    }
}
