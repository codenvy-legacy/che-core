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
package org.eclipse.che.ide.event;

import com.google.gwt.event.shared.EventHandler;

/**
 * Handler for {@link NodeRenamedEvent}
 *
 * @author Igor Vinokur
 */
public interface NodeRenamedEventHandler extends EventHandler {

    /** Called when Node has been renamed.*/
    void onNodeRenamed(NodeRenamedEvent event);
}
