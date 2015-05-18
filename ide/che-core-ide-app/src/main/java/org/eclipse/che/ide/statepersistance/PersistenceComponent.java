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
package org.eclipse.che.ide.statepersistance;

import org.eclipse.che.ide.statepersistance.dto.ActionDescriptor;

import java.util.List;

/**
 * Defines requirements for a component which would like to persist some state of project across sessions.
 * <p/>
 * Implementations of this interface need to be registered using
 * a multibinder in order to be picked-up on start-up.
 *
 * @author Artem Zatsarynnyy
 */
public interface PersistenceComponent {

    /**
     * Returns sequence of actions which should be performed
     * each time when project with the given path is opened
     * in order to restore some part of project's state.
     * <p/>
     * Invoked each time when project with the given {@code projectPath} is closing.
     *
     * @param projectPath
     *         project path
     * @return actions with it's parameters which should be performed on each opening the project
     */
    List<ActionDescriptor> getActions(String projectPath);
}
