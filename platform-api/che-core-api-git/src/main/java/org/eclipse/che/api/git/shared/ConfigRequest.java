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
package org.eclipse.che.api.git.shared;

import org.eclipse.che.dto.shared.DTO;

import java.util.Map;

/**
 * @author andrew00x
 */
@DTO
public interface ConfigRequest {

    Map<String, String> getConfigEntries();

    void setConfigEntries(Map<String, String> configEntries);

    ConfigRequest withConfigEntries(Map<String, String> configEntries);

}
