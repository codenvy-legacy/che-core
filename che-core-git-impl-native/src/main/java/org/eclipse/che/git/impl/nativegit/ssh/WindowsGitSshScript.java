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
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.attribute.AclEntryType.ALLOW;

import static java.nio.file.attribute.AclEntryPermission.READ_DATA;
import static java.nio.file.attribute.AclEntryPermission.APPEND_DATA;
import static java.nio.file.attribute.AclEntryPermission.READ_NAMED_ATTRS;
import static java.nio.file.attribute.AclEntryPermission.READ_ATTRIBUTES;
import static java.nio.file.attribute.AclEntryPermission.DELETE;
import static java.nio.file.attribute.AclEntryPermission.READ_ACL;
import static java.nio.file.attribute.AclEntryPermission.SYNCHRONIZE;

/**
 * Implementation of script that provide ssh connection on Windows
 *
 * @author Alexander Andrienko
 */
public class WindowsGitSshScript extends AbstractGitSshScript {

    private static final String OWNER_NAME_PROPERTY = "user.name";

    public WindowsGitSshScript(String host, byte[] sshKey) throws GitException {
        super(host, sshKey);
    }

    @Override
    protected String getSshScript() {
        return "ssh_script.bat";
    }

    @Override
    protected String getSshScriptTemplate() {
        return  "@echo off\n ssh -o IdentitiesOnly=yes -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -i \"$ssh_key\" %*";
    }

    @Override
    protected void protectPrivateKeyFile(File ssKey) throws GitException {
        try {
            AclFileAttributeView attributes = Files.getFileAttributeView(ssKey.toPath(), AclFileAttributeView.class);

            AclEntry.Builder builder = AclEntry.newBuilder();
            builder.setType(ALLOW);

            String ownerName = System.getProperty(OWNER_NAME_PROPERTY);
            UserPrincipal userPrincipal = FileSystems.getDefault().getUserPrincipalLookupService().lookupPrincipalByName(ownerName);

            builder.setPrincipal(userPrincipal);
            builder.setPermissions(READ_DATA,
                                   APPEND_DATA,
                                   READ_NAMED_ATTRS,
                                   READ_ATTRIBUTES,
                                   DELETE,
                                   READ_ACL,
                                   SYNCHRONIZE);

            AclEntry entry = builder.build();
            List<AclEntry> aclEntryList = new ArrayList<>();
            aclEntryList.add(entry);
            attributes.setAcl(aclEntryList);
        } catch (IOException e) {
            throw new GitException("Failed to set file permissions");
        }
    }
}
