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
package org.eclipse.che.security.oauth.shared.dto;

import org.eclipse.che.api.core.rest.shared.dto.Hyperlinks;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.dto.shared.DTO;

import java.util.List;

/**
 * @author Max Shaposhnik (mshaposhnik@codenvy.com)
 *
 */
@DTO
public interface OAuthAuthenticatorDescriptor extends Hyperlinks {

    String getName();

    void setName(String name);

    OAuthAuthenticatorDescriptor withName(String name);

    @Override
    OAuthAuthenticatorDescriptor withLinks(List<Link> links);

}
