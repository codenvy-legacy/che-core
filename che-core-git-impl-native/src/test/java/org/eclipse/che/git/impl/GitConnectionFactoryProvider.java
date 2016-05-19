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
package org.eclipse.che.git.impl;

import com.google.common.io.Files;

import org.eclipse.che.api.git.GitException;
import org.eclipse.che.api.git.CredentialsLoader;
import org.eclipse.che.git.impl.nativegit.NativeGitConnectionFactory;
import org.eclipse.che.git.impl.nativegit.ssh.GitSshScriptProvider;
import org.testng.annotations.DataProvider;

import java.io.File;

import static org.mockito.Mockito.mock;

/**
 * @author Sergii Kabashniuk
 */
public class GitConnectionFactoryProvider {

    @DataProvider(name = "GitConnectionFactory")
    public static Object[][] createConnection() throws GitException {
        return new Object[][]{
                new Object[]{
                        new NativeGitConnectionFactory(Files.createTempDir(),
                                mock(CredentialsLoader.class),
                                new GitSshScriptProvider(host -> new byte[0]))
                }
        };
    }
}
