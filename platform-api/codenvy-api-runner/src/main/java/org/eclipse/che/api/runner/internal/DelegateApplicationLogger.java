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

import java.io.IOException;

/**
 * Implementation of the {@code ApplicationLogger} which delegates log messages to underlying {@code ApplicationLogger}.
 *
 * @author andrew00x
 */
public abstract class DelegateApplicationLogger implements ApplicationLogger {
    protected final ApplicationLogger delegate;

    public DelegateApplicationLogger(ApplicationLogger delegate) {
        this.delegate = delegate;
    }

    @Override
    public void getLogs(Appendable output) throws IOException {
        delegate.getLogs(output);
    }

    @Override
    public String getContentType() {
        return delegate.getContentType();
    }

    @Override
    public void writeLine(String line) throws IOException {
        delegate.writeLine(line);
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
