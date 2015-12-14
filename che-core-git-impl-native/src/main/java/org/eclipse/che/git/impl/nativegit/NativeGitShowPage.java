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
package org.eclipse.che.git.impl.nativegit;



import org.eclipse.che.api.git.GitException;
import org.eclipse.che.api.git.ShowPage;
import org.eclipse.che.api.git.shared.ShowRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * Content of the file from specified revision or branch.
 *
 * @author Igor Vinokur
 */
public class NativeGitShowPage extends ShowPage {

    private static Logger LOG = LoggerFactory.getLogger(NativeGitShowPage.class);
    private ShowRequest request;
    private NativeGit   nativeGit;

    public NativeGitShowPage(ShowRequest request, NativeGit nativeGit) {
        this.request = request;
        this.nativeGit = nativeGit;
    }

    @Override
    public void writeTo(OutputStream out) throws IOException {
        try (PrintWriter outWriter = new PrintWriter(out)) {
            outWriter.print(nativeGit.createShowCommand()
                                     .withFilePattern(request.getFile())
                                     .withVersion(request.getVersion())
                                     .execute());
        } catch (GitException e) {
            LOG.error("Show page creating exception", e);
        }
    }
}
