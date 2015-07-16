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


import java.io.File;

/**
 * @author Sergii Kabashniuk
 */
public class GitSshScriptProvider {

    private final SshKeyProvider sshKeyProvider;

    public GitSshScriptProvider(SshKeyProvider sshKeyProvider) {
        this.sshKeyProvider = sshKeyProvider;
    }

    public File gitSshScript() throws GitException{
        return null;
    }
//
//    /**
//     * @param command
//     *         GitCommand that will be executed
//     * @param lineConsumerFactory
//     *         factory that provides LineConsumer for propagate output of this command
//     * @throws org.eclipse.che.api.git.GitException
//     *         when command execution error occurs
//     */
//    public static void executeGitCommand(RemoteOperationCommand command, LineConsumerFactory lineConsumerFactory) throws GitException {
//        try {
//            // save private key in local file
//            final File keyFile = new File(getKeyDirectoryPath() + '/' + host + '/' + DEFAULT_KEY_NAME);
//            try (FileOutputStream fos = new FileOutputStream(keyFile)) {
//                fos.write(privateKey.getBytes());
//            } catch (IOException e) {
//                LOG.error("Cant store key", e);
//                throw new GitException("Cant store ssh key. ");
//            }
//
//            //set perm to -r--r--r--
//            keyFile.setReadOnly();
//            //set perm to ----------
//            keyFile.setReadable(false, false);
//            //set perm to -r--------
//            keyFile.setReadable(true, true);
//            //set perm to -rw-------
//            keyFile.setWritable(true, true);
//            executeGitCommand(command, lineConsumerFactory);
//        } finally {
//
//        }
//
//    }

//    /**
//     * Stores ssh script that will be executed with all commands that need ssh.
//     *
//     * @param pathToSSHKey
//     *         path to ssh key
//     * @throws GitException
//     *         when any error with ssh script storing occurs
//     */
//    private void storeSshScript(String pathToSSHKey) throws GitException {
//        File sshScript = new File(SshKeysManager.getKeyDirectoryPath(), SSH_SCRIPT);
//        //creating script
//        try (FileOutputStream fos = new FileOutputStream(sshScript)) {
//            fos.write(sshScriptTemplate.replace("$ssh_key", pathToSSHKey).getBytes());
//        } catch (IOException e) {
//            LOG.error("It is not possible to store " + pathToSSHKey + " ssh key");
//            throw new GitException("Can't store SSH key");
//        }
//        if (!sshScript.setExecutable(true)) {
//            LOG.error("Can't make " + sshScript + " executable");
//            throw new GitException("Can't set permissions to SSH key");
//        }
//    }
}
