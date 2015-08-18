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

import com.google.common.io.Files;

import org.apache.commons.io.FileUtils;
import org.eclipse.che.api.git.GitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Implementation of script that provide ssh connection
 *
 * @author Anton Korneta
 */
public class GitSshScript {

    private static final Logger LOG                 = LoggerFactory.getLogger(GitSshScriptProvider.class);
    private static final String SSH_SCRIPT_TEMPLATE = "exec ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -i $ssh_key $@";
    private static final String SSH_SCRIPT          = "ssh_script";
    private static final String DEFAULT_KEY_NAME    = "identity";

    private byte[] sshKey;
    private String host;
    private File   rootFolder;
    private File   sshScriptFile;

    public GitSshScript(String host, byte[] sshKey) throws GitException {
        this.rootFolder = Files.createTempDir();
        this.host = host;
        this.sshKey = sshKey;
        this.sshScriptFile = storeSshScript(writePrivateKeyFile().getPath());
    }

    /**
     * Writes private SSH key into file.
     *
     * @return file that contains SSH key
     * @throws GitException
     *         if other error occurs
     */
    private File writePrivateKeyFile() throws GitException {
        final File keyDirectory = new File(rootFolder, host);
        if (!keyDirectory.exists()) {
            keyDirectory.mkdirs();
        }

        final File keyFile = new File(rootFolder, host + File.separator + DEFAULT_KEY_NAME);
        try (FileOutputStream fos = new FileOutputStream(keyFile)) {
            fos.write(sshKey);
        } catch (IOException e) {
            LOG.error("Cant store key", e);
            throw new GitException("Cant store ssh key. ");
        }

        boolean perms;
        //set perm to -r--r--r--
        perms = keyFile.setReadOnly();
        //set perm to ----------
        perms &= keyFile.setReadable(false, false);
        //set perm to -r--------
        perms &= keyFile.setReadable(true, true);
        //set perm to -rw-------
        perms &= keyFile.setWritable(true, true);
        if (!perms) throw new GitException("Failed to set file permissions");
        this.sshScriptFile = keyFile;
        return keyFile;
    }

    /**
     * Stores ssh script that will be executed with all commands that need ssh.
     *
     * @param keyPath
     *         path to ssh key
     * @return file that contains script for ssh commands
     * @throws GitException
     *         when any error with ssh script storing occurs
     */
    private File storeSshScript(String keyPath) throws GitException {
        File sshScriptFile = new File(rootFolder, SSH_SCRIPT);
        try (FileOutputStream fos = new FileOutputStream(sshScriptFile)) {
            fos.write(SSH_SCRIPT_TEMPLATE.replace("$ssh_key", keyPath).getBytes());
        } catch (IOException e) {
            LOG.error("It is not possible to store " + keyPath + " ssh key");
            throw new GitException("Can't store SSH key");
        }
        if (!sshScriptFile.setExecutable(true)) {
            LOG.error("Can't make " + sshScriptFile + " executable");
            throw new GitException("Can't set permissions to SSH key");
        }
        return sshScriptFile;
    }

    public File getSshScriptFile() {
        return sshScriptFile;
    }

    /**
     * Remove script folder with sshScript and sshKey
     *
     * @throws GitException
     *         when any error with ssh script deleting occurs
     */
    public void delete() throws GitException {
        try {
            FileUtils.deleteDirectory(rootFolder);
        } catch (IOException ioEx) {
            throw new GitException("Can't remove SSH script directory", ioEx);
        }
    }
}
