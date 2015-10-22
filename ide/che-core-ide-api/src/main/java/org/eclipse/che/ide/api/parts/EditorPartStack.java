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
package org.eclipse.che.ide.api.parts;

import java.util.List;

/**
 * Part Stack is tabbed layout element, containing Parts. EditorPartStack is shared
 * across the Perspectives and allows to display EditorParts
 *
 * @author <a href="mailto:nzamosenchuk@exoplatform.com">Nikolay Zamosenchuk</a>
 */
public interface EditorPartStack extends PartStack {

    /**
     * Get active editor
     * @return active editor
     */
    PartPresenter getActiveEditor();

    /**
     * Get list editors
     * @return list opened editors
     */
    List<PartPresenter> getEditors();
}