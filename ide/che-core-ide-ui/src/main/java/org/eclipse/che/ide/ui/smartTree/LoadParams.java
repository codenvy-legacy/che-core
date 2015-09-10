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
package org.eclipse.che.ide.ui.smartTree;

import org.eclipse.che.ide.api.project.node.HasDataObject;

/**
 * @author Vlad Zhukovskiy
 */
public class LoadParams {
    private HasDataObject<?> selectAfter;
    private boolean          callAction;
    private boolean          goInto;

    public LoadParams(HasDataObject<?> selectAfter, boolean callAction, boolean goInto) {
        this.selectAfter = selectAfter;
        this.callAction = callAction;
        this.goInto = goInto;
    }

    public HasDataObject<?> getSelectAfter() {
        return selectAfter;
    }

    public boolean isCallAction() {
        return callAction;
    }

    public boolean isGoInto() {
        return goInto;
    }
}
