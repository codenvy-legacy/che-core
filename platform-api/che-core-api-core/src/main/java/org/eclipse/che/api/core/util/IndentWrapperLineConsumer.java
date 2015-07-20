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
package org.eclipse.che.api.core.util;

import java.io.IOException;

import static org.eclipse.che.api.core.util.SystemInfo.isWindows;
import static org.eclipse.che.api.core.util.SystemInfo.isUnix;

/**
 * Line consumer for adding indent if line without indent
 * @author Andrienko Alexander
 */
public class IndentWrapperLineConsumer implements LineConsumer {

    final LineConsumer lineConsumer;

    public IndentWrapperLineConsumer(LineConsumer lineConsumer) {
        this.lineConsumer = lineConsumer;
    }

    /** {@inheritDoc} */
    @Override
    public void writeLine(String line) throws IOException {
        if(isWindows()) {
            lineConsumer.writeLine(line + "\r\n");
        }
        if(isUnix()) {
            lineConsumer.writeLine(line + "\n");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        lineConsumer.close();
    }
}
