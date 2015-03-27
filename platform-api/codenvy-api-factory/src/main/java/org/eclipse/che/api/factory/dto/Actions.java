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
package org.eclipse.che.api.factory.dto;

import org.eclipse.che.api.core.factory.FactoryParameter;
import org.eclipse.che.api.vfs.shared.dto.ReplacementSet;
import org.eclipse.che.dto.shared.DTO;

import java.util.List;

import static org.eclipse.che.api.core.factory.FactoryParameter.FactoryFormat.ENCODED;
import static org.eclipse.che.api.core.factory.FactoryParameter.Obligation.OPTIONAL;

/**
 * Describes actions that should be done after loading of the IDE
 *
 * @author andrew00x
 * @author Alexander Garagatyi
 */
@DTO
@Deprecated
public interface Actions {
    /**
     * Welcome page configuration.
     */
    @Deprecated
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "welcome", format = ENCODED, trackedOnly = true)
    WelcomePage getWelcome();

    @Deprecated
    void setWelcome(WelcomePage welcome);

    @Deprecated
    Actions withWelcome(WelcomePage welcome);

    /**
     * Allow to use text replacement in project files after clone
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "findReplace")
    @Deprecated
    List<ReplacementSet> getFindReplace();

    @Deprecated
    void setFindReplace(List<ReplacementSet> variable);

    @Deprecated
    Actions withFindReplace(List<ReplacementSet> variable);


    /**
     * Path of the file to open in the project.
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "openFile")
    @Deprecated
    String getOpenFile();

    @Deprecated
    void setOpenFile(String openFile);

    @Deprecated
    Actions withOpenFile(String openFile);

    /**
     * Warn on leave page
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "warnOnClose")
    @Deprecated
    Boolean getWarnOnClose();

    @Deprecated
    void setWarnOnClose(Boolean warnOnClose);

    @Deprecated
    Actions withWarnOnClose(Boolean warnOnClose);

}
