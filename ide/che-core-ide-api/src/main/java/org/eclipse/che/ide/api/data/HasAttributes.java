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
package org.eclipse.che.ide.api.data;

import java.util.List;
import java.util.Map;

/**
 * Holder for attributes that may has any object.
 *
 * @author Vlad Zhukovskiy
 */
public interface HasAttributes {
    /**
     * Returns attributes map.
     * Attributes map should not be a {@code null}.
     *
     * @return non-null attributes map
     */
    Map<String, List<String>> getAttributes();

    /**
     * Store attributes map.
     * Attributes map should not be a {@code null}.
     *
     * @param attributes
     *         non-null attributes map
     */
    void setAttributes(Map<String, List<String>> attributes);
}
