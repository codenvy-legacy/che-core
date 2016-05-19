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
package org.eclipse.che.ide.jseditor.client.changeintercept;

import org.eclipse.che.ide.jseditor.client.document.ReadOnlyDocument;

/**
 * Interface for components that modify changes in the text.<br>
 * The interceptor should only modify the content using its return value (meaning it should not directly
 * access the odcument or editor to do changes).
 */
public interface TextChangeInterceptor {

    /**
     * Process a change in the editor text.
     * @param change the incoming change
     * @param the read-only version of the document
     * @return the new version of the change (null doesn't modify the change)
     */
    TextChange processChange(TextChange change, ReadOnlyDocument document);
}
