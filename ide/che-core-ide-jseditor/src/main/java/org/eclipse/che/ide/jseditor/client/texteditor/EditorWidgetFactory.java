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
package org.eclipse.che.ide.jseditor.client.texteditor;

import org.eclipse.che.ide.jseditor.client.texteditor.EditorWidget.WidgetInitializedCallback;

import java.util.List;

/**
 * Interface for {@link EditorWidget} factories.
 *
 * @author "Mickaël Leduque"
 * @author Artem Zatsarynnyi
 */
public interface EditorWidgetFactory<T extends EditorWidget> {

    /**
     * Create an editor instance.
     *
     * @param editorModes
     *         the editor modes
     * @param widgetInitializedCallback
     *         the callback that will be called when the editor widget is fully initialize
     * @return an editor instance
     */
    T createEditorWidget(List<String> editorModes, WidgetInitializedCallback widgetInitializedCallback);
}
