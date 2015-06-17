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

import com.google.common.base.MoreObjects;

import org.eclipse.che.api.git.GitException;

import java.io.File;
import java.util.List;

/**
 * Update remote refs with associated objects
 *
 * @author Eugene Voevodin
 */
public class PushCommand extends RemoteUperationCommand<Void> {

    private List<String> refSpec;
    private boolean      force;

    public PushCommand(File repository) {
        super(repository);
    }

    /** @see GitCommand#execute() */
    @Override
    public Void execute() throws GitException {
        reset();
        commandLine.add("push");
        commandLine.add(MoreObjects.firstNonNull(getRemoteUrl(), "origin"));
        if (refSpec != null) {
            commandLine.add(refSpec);
        }
        if (force) {
            commandLine.add("--force");
        }
        start();
        return null;
    }

    /**
     * @param refSpecs
     *         ref specs to push
     * @return PushCommand with established ref specs
     */
    public PushCommand setRefSpec(List<String> refSpecs) {
        this.refSpec = refSpecs;
        return this;
    }


    /**
     * @param force
     *         if <code>true</code> push will be forced
     * @return PushCommand with established force parmeter
     */
    public PushCommand setForce(boolean force) {
        this.force = force;
        return this;
    }
}
