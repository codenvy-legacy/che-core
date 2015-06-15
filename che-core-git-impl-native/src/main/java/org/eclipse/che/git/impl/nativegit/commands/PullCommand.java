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
package org.eclipse.che.git.impl.nativegit.commands;

import org.eclipse.che.api.git.GitException;
import org.eclipse.che.api.git.shared.GitUser;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Fetch from and merge with another repository
 *
 * @author Eugene Voevodin
 */
public class PullCommand extends GitCommand<Void> {

    private String  remote;
    private String  refSpec;
    private GitUser author;

    public PullCommand(File repository) {
        super(repository);
    }

    /** @see GitCommand#execute() */
    @Override
    public Void execute() throws GitException {
        remote = remote == null ? "origin" : remote;
        reset();
        commandLine.add("pull");
        if (remote != null) {
            commandLine.add(remote);
        }
        if (refSpec != null) {
            commandLine.add(refSpec);
        }
        if (author != null) {
            Map<String, String> environment = new HashMap<>();
            environment.put("GIT_AUTHOR_NAME", author.getName());
            environment.put("GIT_AUTHOR_EMAIL", author.getEmail());
            environment.put("GIT_COMMITTER_NAME", author.getName());
            environment.put("GIT_COMMITTER_EMAIL", author.getEmail());
            setCommandEnvironment(environment);
        }
        start();
        return null;
    }

    /**
     * @param remoteName
     *         remote name
     * @return PullCommand with established remote name
     */
    public PullCommand setRemote(String remoteName) {
        this.remote = remoteName;
        return this;
    }

    /**
     * @param refSpec
     *         ref spec to pull
     * @return PullCommand with established ref spec
     */
    public PullCommand setRefSpec(String refSpec) {
        this.refSpec = refSpec;
        return this;
    }

    /**
     * @param author
     *         author of command
     * @return PullCommand with established author
     */
    public PullCommand setAuthor(GitUser author) {
        this.author = author;
        return this;
    }
}
