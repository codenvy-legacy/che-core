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

import java.io.File;

/**
 * Checkout branch
 *
 * @author Eugene Voevodin
 */
public class BranchCheckoutCommand extends GitCommand<Void> {

    private boolean createNew;
    private boolean isRemote;
    private String  branchName;
    private String  startPoint;

    public BranchCheckoutCommand(File place) {
        super(place);
    }

    /** @see GitCommand#execute() */
    @Override
    public Void execute() throws GitException {
        if (branchName == null) {
            throw new GitException("Branch name was not set.");
        }
        reset();
        commandLine.add("checkout");
        if (isRemote) {
            commandLine.add("-t");
            commandLine.add(branchName);
            start();
            return null;
        }

        if (createNew) {
            commandLine.add("-b");
        }
        commandLine.add(branchName);
        if (startPoint != null) {
            commandLine.add(startPoint);
        }
        start();
        return null;
    }

    /**
     * @param createNew
     *         if <code>true</code> new branch will be created
     * @return BranchCheckoutCommand with established create new branch parameter
     */
    public BranchCheckoutCommand setCreateNew(boolean createNew) {
        this.createNew = createNew;
        return this;
    }

    /**
     * @param branchName
     *         branch to checkout
     * @return BranchCheckoutCommand with established branch to checkout
     */
    public BranchCheckoutCommand setBranchName(String branchName) {
        this.branchName = branchName;
        return this;
    }

    /**
     * @param remote
     *         if <code>true</code> branch will be tracked to remote
     * @return BranchCheckoutCommand with established remote parameter
     */
    public BranchCheckoutCommand setRemote(boolean remote) {
        isRemote = remote;
        return this;
    }

    /**
     * @param startPoint
     *         checkout start point
     * @return BranchCheckoutCommand with start point
     */
    public BranchCheckoutCommand setStartPoint(String startPoint) {
        this.startPoint = startPoint;
        return this;
    }
}
