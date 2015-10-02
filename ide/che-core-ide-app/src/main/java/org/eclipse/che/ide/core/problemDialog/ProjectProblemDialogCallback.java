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
package org.eclipse.che.ide.core.problemDialog;

import org.eclipse.che.api.project.shared.dto.SourceEstimation;
import org.eclipse.che.commons.annotation.Nullable;

/**
 * Callback will call when the user clicks on one of the buttons in the project problem dialog.
 *
 * @author Roman Nikitenko
 */
public interface ProjectProblemDialogCallback {

    /** Call when the user clicks on "Configure" button. */
    void onConfigure(@Nullable SourceEstimation estimatedType);

    /** Call when the user clicks on "Open as ..." button. */
    void onOpenAs(@Nullable SourceEstimation estimatedType);

    /** Call when the user clicks on "Open as is" button. */
    void onOpenAsIs();
}
