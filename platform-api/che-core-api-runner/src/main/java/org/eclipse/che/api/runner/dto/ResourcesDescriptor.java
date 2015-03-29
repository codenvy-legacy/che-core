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
package org.eclipse.che.api.runner.dto;

import org.eclipse.che.dto.shared.DTO;
import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 *
 * @author Sergii Leschenko
 */
@DTO
public interface ResourcesDescriptor {
    @ApiModelProperty(value = "Total RAM")
    String getTotalMemory();

    void setTotalMemory(String memory);

    ResourcesDescriptor withTotalMemory(String memory);

    @ApiModelProperty(value = "RAM in use")
    String getUsedMemory();

    void setUsedMemory(String memory);

    ResourcesDescriptor withUsedMemory(String memory);
}
