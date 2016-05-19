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
package org.eclipse.che.ide.jseditor.client.debug;

import org.eclipse.che.ide.debug.BreakpointRenderer;
import org.eclipse.che.ide.jseditor.client.document.Document;
import org.eclipse.che.ide.jseditor.client.gutter.Gutter;
import org.eclipse.che.ide.jseditor.client.texteditor.LineStyler;

/** Factory for {@link BreakpointRenderer} instances.*/
public interface BreakpointRendererFactory {

    /**
     * Creates an instance of {@link BreakpointRenderer} that uses both a gutter and a line styler.
     * @param hasGutter the gutter manager
     * @param lineStyler the line style manager
     * @param document the document
     * @return a {@link BreakpointRenderer}
     */
    BreakpointRenderer create(final Gutter hasGutter,
                              final LineStyler lineStyler,
                              final Document document);
}
