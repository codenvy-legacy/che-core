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
package org.eclipse.che.api.machine.shared.dto;

import org.eclipse.che.api.core.rest.shared.dto.Hyperlinks;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.machine.shared.ProjectBinding;
import org.eclipse.che.dto.shared.DTO;

import java.util.List;

/**
 * Describes project that is bound to machine
 *
 * @author andrew00x
 */
@DTO
public interface ProjectBindingDescriptor extends ProjectBinding, Hyperlinks {
    void setPath(String path);

    ProjectBindingDescriptor withPath(String path);

    @Override
    ProjectBindingDescriptor withLinks(List<Link> links);
}
