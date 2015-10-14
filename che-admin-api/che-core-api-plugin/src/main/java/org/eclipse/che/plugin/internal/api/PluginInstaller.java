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
package org.eclipse.che.plugin.internal.api;

import com.google.common.util.concurrent.FutureCallback;

import java.util.List;

/**
 * @author Florent Benoit
 */
public interface PluginInstaller {
    IPluginInstall requireNewInstall(FutureCallback pluginInstallerCallback) throws PluginInstallerException;


    IPluginInstall getInstall(long id) throws PluginInstallerNotFoundException;

    /**
     * Gets the list of all install
     * @return the IDs of every install
     */
    List<IPluginInstall> listInstall();
}
