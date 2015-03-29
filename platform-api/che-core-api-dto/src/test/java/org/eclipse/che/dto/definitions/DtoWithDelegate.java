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
package org.eclipse.che.dto.definitions;

import org.eclipse.che.dto.shared.DTO;
import org.eclipse.che.dto.shared.DelegateRule;
import org.eclipse.che.dto.shared.DelegateTo;

/**
 * @author andrew00x
 */
@DTO
public interface DtoWithDelegate {
    String getName();

    void setName(String name);

    DtoWithDelegate withName(String name);

    @DelegateTo(client = @DelegateRule(type = Util.class, method = "addPrefix"),
                server = @DelegateRule(type = Util.class, method = "addPrefix"))
    String nameWithPrefix(String prefix);
}
