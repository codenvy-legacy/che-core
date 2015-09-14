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
package org.eclipse.che.ide.api.wizard;

import org.eclipse.che.ide.api.mvp.Presenter;

import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * The main responsibility of a {@link WizardPage} subclass is collecting data.
 *
 * @param <T>
 *         the type of the data-object that stores collected data
 * @author Andrey Plotnikov
 * @author Artem Zatsarynnyy
 */
public interface WizardPage<T> extends Presenter {
    /** Initializes page by the passed {@code dataObject}. */
    void init(T dataObject);

    void setContext(@NotNull Map<String, String> context);

    /** Sets update control delegate. */
    void setUpdateDelegate(@NotNull Wizard.UpdateDelegate delegate);

    /**
     * Returns whether this page is completed or not.
     * This information is typically used by the wizard to decide when it is okay to finish.
     *
     * @return {@code true} if this page is completed, otherwise - {@code false}
     */
    boolean isCompleted();

    /**
     * Determines whether the page should be skipped (shouldn't be shown) by wizard.
     *
     * @return {@code true} if this page should be skipped, otherwise - {@code false}
     */
    boolean canSkip();
}
