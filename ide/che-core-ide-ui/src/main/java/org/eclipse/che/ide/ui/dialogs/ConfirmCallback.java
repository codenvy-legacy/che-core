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
package org.eclipse.che.ide.ui.dialogs;

/**
 * Callback called when the user clicks on "OK" in the confirmation dialog.
 *
 * @author Mickaël Leduque
 * @author Artem Zatsarynnyi
 */
public interface ConfirmCallback {

    /** Action called when the user click on OK. */
    void accepted();
}
