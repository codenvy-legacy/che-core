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

import java.io.File;


/**
 * @author Sergii Kabashniuk
 */
public abstract class RemoteOperationCommand<T> extends GitCommand<T> {

    private String remoteUrl;

    /**
     * @param repository
     *         directory where command will be executed
     */
    public RemoteOperationCommand(File repository) {
        super(repository);
    }


    public String getRemoteUrl() {
        return remoteUrl;
    }

    public void setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }

}
