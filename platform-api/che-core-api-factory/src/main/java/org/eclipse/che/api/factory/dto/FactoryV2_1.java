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

import static org.eclipse.che.api.core.factory.FactoryParameter.Obligation.OPTIONAL;

import org.eclipse.che.api.core.factory.FactoryParameter;
import org.eclipse.che.api.project.shared.dto.NewProject;
import org.eclipse.che.api.project.shared.dto.Source;
import org.eclipse.che.dto.shared.DTO;

/**
 * Factory of version 2.0
 *
 * @author Sergii Kabashniuk
 */
@DTO
public interface FactoryV2_1 extends FactoryV2_0 {
    /**
     * Describes ide look and feel.
     */
    @FactoryParameter(obligation = OPTIONAL)
    Ide getIde();

    void setIde(Ide ide);

    FactoryV2_1 withIde(Ide ide);

    // For method call chain

    FactoryV2_1 withV(String v);

    FactoryV2_1 withSource(Source source);

    FactoryV2_1 withWorkspace(Workspace workspace);

    FactoryV2_1 withPolicies(Policies policies);

    FactoryV2_1 withProject(NewProject project);

    FactoryV2_1 withCreator(Author creator);

    FactoryV2_1 withButton(Button button);

    FactoryV2_1 withId(String id);
}
