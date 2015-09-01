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
package org.eclipse.che.ide.api.project.node.settings.impl;

import org.eclipse.che.ide.api.project.node.settings.NodeSettings;
import org.eclipse.che.ide.api.project.node.settings.SettingsProvider;

/**
 * @author Vlad Zhukovskiy
 */
public class UserProfileNodeSettingsProvider implements SettingsProvider {
    @Override
    public NodeSettings getSettings() {
        //TODO logic for loading node settings from user profile
        return null;
    }

    @Override
    public void setSettings(NodeSettings settings) {
        //TODO logic for storing node settings into user profile
    }
}
