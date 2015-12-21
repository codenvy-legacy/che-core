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
package org.eclipse.che.dto.definitions.model;

import org.eclipse.che.dto.shared.DTO;
import org.eclipse.che.dto.shared.Compared;

/**
 * Test dto extension for model component {@link ModelComponent}
 *
 * @author Vlad Zhukovskiy
 */
@DTO
public interface ComparedModelDto extends ModelComponent {

    @Compared
    @Override
    String getName();

    void setName(String name);

    ComparedModelDto withName(String name);

    String getValue();

    void setValue(String value);

    ComparedModelDto withValue(String value);
}
