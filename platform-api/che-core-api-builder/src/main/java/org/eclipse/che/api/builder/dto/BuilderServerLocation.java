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
package org.eclipse.che.api.builder.dto;

import org.eclipse.che.dto.shared.DTO;
import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * Location of {@code SlaveBuilderService} resource.
 *
 * @author andrew00x
 * @see org.eclipse.che.api.builder.internal.SlaveBuilderService
 */
@DTO
public interface BuilderServerLocation {
    /**
     * Get URL of this SlaveBuilderService. This URL may be used for direct access to the {@code SlaveBuilderService} functionality.
     *
     * @return resource URL
     */
    @ApiModelProperty(value = "Builder URL", required = true, notes = "This is URL of a new builder service, where builder name is a prefix to main DNS name, for example 'http://builder2.hostname.com/builder/internal/builder' ")
    String getUrl();

    /**
     * Set URL of this SlaveBuilderService. This URL may be used for direct access to the {@code SlaveBuilderService} functionality.
     *
     * @param url
     *         resource URL
     */
    void setUrl(String url);

    BuilderServerLocation withUrl(String url);
}
