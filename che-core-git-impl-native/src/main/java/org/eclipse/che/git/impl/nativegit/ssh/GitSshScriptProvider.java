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
package org.eclipse.che.git.impl.nativegit.ssh;

import org.eclipse.che.api.git.GitException;
import org.eclipse.che.git.impl.nativegit.GitUrl;

import javax.inject.Inject;

/**
 * Provides GitSshScript
 *
 * @author Sergii Kabashniuk
 * @author Anton Korneta
 */
public class GitSshScriptProvider {

    private final SshKeyProvider sshKeyProvider;

    @Inject
    public GitSshScriptProvider(SshKeyProvider sshKeyProvider) {
        this.sshKeyProvider = sshKeyProvider;
    }

    /**
     * Get GitSshScript object
     *
     * @param url
     *         url to git repository
     * @throws GitException
     *         if an error occurs when creating a script file
     */
    public GitSshScript gitSshScript(String url) throws GitException {
        String host = GitUrl.getHost(url);
        if (host == null) {
            throw new GitException("URL does not have a host");
        }
        return new GitSshScript(host, sshKeyProvider.getPrivateKey(url));
    }
}
