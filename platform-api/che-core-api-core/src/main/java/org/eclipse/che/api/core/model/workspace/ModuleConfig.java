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
package org.eclipse.che.api.core.model.workspace;

import java.util.List;
import java.util.Map;

/**
 * @author  Vitalii Parfonov
 */
public interface ModuleConfig {

    String getName();

    String getPath();

    String getDescription();

    String getType();

    List<String> getMixinTypes();

    Map<String, List<String>> getAttributes();

    List<? extends ModuleConfig> getModules();
}
