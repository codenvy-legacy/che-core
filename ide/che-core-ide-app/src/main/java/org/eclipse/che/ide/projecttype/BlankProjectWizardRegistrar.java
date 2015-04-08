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
package org.eclipse.che.ide.projecttype;

import org.eclipse.che.api.project.shared.dto.ImportProject;
import org.eclipse.che.ide.api.project.type.wizard.ProjectWizardRegistrar;
import org.eclipse.che.ide.api.wizard.WizardPage;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.collections.Collections;
import com.google.inject.Provider;

import javax.annotation.Nonnull;

/**
 * Provides information for registering Blank project type into project wizard.
 *
 * @author Artem Zatsarynnyy
 */
public class BlankProjectWizardRegistrar implements ProjectWizardRegistrar {

    public static final String BLANK_CATEGORY            = "Blank";
    public static final String BLANK_ID                  = "blank";

    private final Array<Provider<? extends WizardPage<ImportProject>>> wizardPages;

    public BlankProjectWizardRegistrar() {
        wizardPages = Collections.createArray();
    }

    @Nonnull
    public String getProjectTypeId() {
        return BLANK_ID;
    }

    @Nonnull
    public String getCategory() {
        return BLANK_CATEGORY;
    }

    @Nonnull
    public Array<Provider<? extends WizardPage<ImportProject>>> getWizardPages() {
        return wizardPages;
    }
}
