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
package org.eclipse.che.ide.api.selection;

import org.eclipse.che.ide.api.event.SelectionChangedEvent;
import org.eclipse.che.ide.api.extension.SDK;
import com.google.web.bindery.event.shared.EventBus;

/**
 * Selection API allows to provide a way of data-based communication, when Parts provide a static
 * access to the data selected in active Part.
 * In order to listen to dynamic Selection changes, please subscribe to {@link SelectionChangedEvent}
 * on {@link EventBus}.
 *
 * @author Nikolay Zamosenchuk
 */
@SDK(title = "ide.api.ui.selection")
public interface SelectionAgent {

    /**
     * Provides a way of getting current app-wide Selection.
     *
     * @return
     */
    Selection<?> getSelection();
}