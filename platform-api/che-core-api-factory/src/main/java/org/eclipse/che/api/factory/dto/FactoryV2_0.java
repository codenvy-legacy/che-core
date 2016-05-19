/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
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
import org.eclipse.che.api.project.shared.dto.NewProject;
import org.eclipse.che.api.project.shared.dto.Source;
import org.eclipse.che.dto.shared.DTO;

import static org.eclipse.che.api.core.factory.FactoryParameter.Obligation.MANDATORY;
import static org.eclipse.che.api.core.factory.FactoryParameter.Obligation.OPTIONAL;
import static org.eclipse.che.api.core.factory.FactoryParameter.Version.V2_1;

/**
 * Factory of version 2.0
 *
 * @author andrew00x
 * @author Alexander Garagatyi
 */
@DTO
public interface FactoryV2_0 {

    /**
     * @return Version for Codenvy Factory API.
     */
    @FactoryParameter(obligation = MANDATORY)
    String getV();

    void setV(String v);

    FactoryV2_0 withV(String v);

    /**
     * Describes source where project's files can be retrieved
     */
    @FactoryParameter(obligation = MANDATORY)
    Source getSource();

    void setSource(Source source);

    FactoryV2_0 withSource(Source source);

    /**
     * Describes parameters of the workspace that should be used for factory
     */
    @FactoryParameter(obligation = OPTIONAL)
    Workspace getWorkspace();

    void setWorkspace(Workspace workspace);

    FactoryV2_0 withWorkspace(Workspace workspace);

    /**
     * Describe restrictions of the factory
     */
    @FactoryParameter(obligation = OPTIONAL, trackedOnly = true)
    Policies getPolicies();

    void setPolicies(Policies policies);

    FactoryV2_0 withPolicies(Policies policies);

    /**
     * Describes project that should be factory-created
     */
    @FactoryParameter(obligation = OPTIONAL)
    NewProject getProject();

    void setProject(NewProject project);

    FactoryV2_0 withProject(NewProject project);

    /**
     * Identifying information of author
     */
    @FactoryParameter(obligation = OPTIONAL)
    Author getCreator();

    void setCreator(Author creator);

    FactoryV2_0 withCreator(Author creator);

    /**
     * Describes actions that should be done after loading of the IDE
     */
    @Deprecated
    @FactoryParameter(obligation = OPTIONAL, deprecatedSince = V2_1)
    Actions getActions();

    @Deprecated
    void setActions(Actions actions);

    @Deprecated
    FactoryV2_0 withActions(Actions actions);

    /**
     * Describes factory button
     */
    @FactoryParameter(obligation = OPTIONAL)
    Button getButton();

    void setButton(Button button);

    FactoryV2_0 withButton(Button button);


    /**
     * @return - id of stored factory object
     */
    @FactoryParameter(obligation = OPTIONAL, setByServer = true)
    String getId();

    void setId(String id);

    FactoryV2_0 withId(String id);

}
