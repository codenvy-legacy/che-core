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
package org.eclipse.che.api.builder.internal;

import org.eclipse.che.api.builder.BuilderException;
import org.eclipse.che.api.builder.dto.BaseBuilderRequest;

/** @author andrew00x */
public interface BuilderConfigurationFactory {
    BuilderConfiguration createBuilderConfiguration(BaseBuilderRequest request) throws BuilderException;
}
