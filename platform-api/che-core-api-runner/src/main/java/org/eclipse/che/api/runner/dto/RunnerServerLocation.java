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
package org.eclipse.che.api.runner.dto;

import org.eclipse.che.dto.shared.DTO;
import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * Location of {@code SlaveRunnerService} resource.
 *
 * @author andrew00x
 * @see org.eclipse.che.api.runner.internal.SlaveRunnerService
 */
@DTO
public interface RunnerServerLocation {
    /**
     * Get URL of this SlaveRunnerService. This URL may be used for direct access to the {@code SlaveRunnerService} functionality.
     *
     * @return resource URL
     */
    @ApiModelProperty(value = "Runner URL")
    String getUrl();

    /**
     * Set URL of this SlaveRunnerService. This URL may be used for direct access to the {@code SlaveRunnerService} functionality.
     *
     * @param url
     *         resource URL
     */
    void setUrl(String url);

    RunnerServerLocation withUrl(String url);
}
