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
package org.eclipse.che.api.auth.shared.dto;

import org.eclipse.che.dto.shared.DTO;
import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * @author gazarenkov
 */
@DTO
public interface Credentials {

    @ApiModelProperty(value = "Parameter used to login as system/admin. It is not necessary for users", allowableValues = "sysldap")
    String getRealm();

    void setRealm(String realm);

    Credentials withRealm(String realm);

    @ApiModelProperty(value = "Codenvy login - registration email", required = true)
    String getUsername();

    void setUsername(String name);

    Credentials withUsername(String name);

    @ApiModelProperty(value = "Codenvy password. If you don't know your password, restore it at /site/recover-password page", required = true)
    String getPassword();

    void setPassword(String password);

    Credentials withPassword(String password);

}
