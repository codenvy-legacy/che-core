package org.eclipse.che.api.machine.shared;

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

/**
 * Command that can be used to create {@link Process} in a machine
 *
 * @author Eugene Voevodin
 */
public interface ManagedCommand extends Command {

    /**
     * Returns command unique identifier
     */
    String getId();

    /**
     * Returns command name (i.e. 'start tomcat')
     * <p>
     * The name should be unique per user in one workspace,
     * which means that user may create only one command with the same name in the same workspace
     */
    String getName();

    /**
     * Returns command line (i.e. 'mvn clean install') which is going to be executed
     * <p>
     * Serves as a base for {@link Process} creation.
     *
     * @see Process#getCommandLine()
     */
    String getCommandLine();

    /**
     * Returns identifier of user who is the command creator
     */
    String getCreator();

    /**
     * Returns workspace identifier which command is related with
     */
    String getWorkspaceId();

    /**
     * Returns command visibility (i.e. 'private')
     */
    String getVisibility();

    /**
     * Returns command type (i.e. 'maven')
     */
    String getType();

    /**
     * Returns absolute path to directory where the command should be executed
     */
    String getWorkingDir();
}
