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
package org.eclipse.che.ide.projectimport.wizard;

import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;

import javax.validation.constraints.NotNull;

/**
 * Helps to create new instances of {@link ImportWizard}.
 *
 * @author Artem Zatsarynnyi
 */
public interface ImportWizardFactory {
    ImportWizard newWizard(@NotNull ProjectConfigDto dataObject);
}
