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

import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.project.shared.dto.NewProject;
import org.eclipse.che.api.project.shared.dto.Source;
import org.eclipse.che.dto.shared.DTO;

import java.util.List;

/**
 * Latest version of factory implementation.
 *
 * @author Alexander Garagatyi
 */
@DTO
public interface Factory extends FactoryV2_1 {
    List<Link> getLinks();

    void setLinks(List<Link> links);

    Factory withLinks(List<Link> links);

    Factory withV(String v);

    Factory withSource(Source source);

    Factory withWorkspace(Workspace workspace);

    Factory withPolicies(Policies policies);

    Factory withProject(NewProject project);

    Factory withCreator(Author creator);

    Factory withActions(Actions actions);

    Factory withButton(Button button);

    Factory withIde(Ide ide);
}
