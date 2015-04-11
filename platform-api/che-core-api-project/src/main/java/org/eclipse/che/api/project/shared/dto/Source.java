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
package org.eclipse.che.api.project.shared.dto;

import org.eclipse.che.api.core.factory.FactoryParameter;
import org.eclipse.che.dto.shared.DTO;

import java.util.Map;

import static org.eclipse.che.api.core.factory.FactoryParameter.Obligation.OPTIONAL;
import static org.eclipse.che.api.core.factory.FactoryParameter.Obligation.MANDATORY;

/**
 * Describes project source with additional sources such as runner's
 *
 * @author Alexander Garagatyi
 */
@DTO
public interface Source {
    @FactoryParameter(obligation = MANDATORY)
    ImportSourceDescriptor getProject();

    void setProject(ImportSourceDescriptor project);

    Source withProject(ImportSourceDescriptor project);

//    @FactoryParameter(obligation = OPTIONAL)
//    Map<String, RunnerSource> getRunners();
//
//    void setRunners(Map<String, RunnerSource> runners);
//
//    Source withRunners(Map<String, RunnerSource> runners);
}

