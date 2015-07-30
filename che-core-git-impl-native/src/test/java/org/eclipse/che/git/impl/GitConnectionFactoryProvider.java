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
package org.eclipse.che.git.impl;

import org.eclipse.che.git.impl.nativegit.CredentialsLoader;
import org.eclipse.che.git.impl.nativegit.CredentialsProvider;
import org.eclipse.che.git.impl.nativegit.NativeGitConnectionFactory;
import org.eclipse.che.git.impl.nativegit.ssh.GitSshScriptProvider;
import org.eclipse.che.git.impl.nativegit.ssh.SshKeyProvider;
import org.testng.annotations.DataProvider;

import java.util.Collections;

/**
 * @author Sergii Kabashniuk
 */
public class GitConnectionFactoryProvider {


    @DataProvider(name = "GitConnectionFactory")
    public static Object[][] createConnection() {
        return new Object[][]{
                new Object[]{new NativeGitConnectionFactory(
                        new CredentialsLoader(Collections.<CredentialsProvider>emptySet()),
                        new GitSshScriptProvider(
                                new SshKeyProvider() {
                                    @Override
                                    public byte[] getPrivateKey(String host) {
                                        return new byte[0];
                                    }
                                }),
                        null)
                }
        };
    }
}
