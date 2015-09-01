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
package org.eclipse.che.ide.ui.smartTree.state;

import org.eclipse.che.ide.ui.smartTree.Tree;

/**
 * @author Vlad Zhukovskiy
 */
public abstract class AbstractStateHandler<S> {

    protected Tree tree;
    protected AbstractStateProvider stateProvider;

    protected S state;

    public AbstractStateHandler(Tree tree, AbstractStateProvider stateProvider) {
        this.tree = tree;
        this.stateProvider = stateProvider;
    }

    public S getState() {
        return state;
    }

    public abstract void applyState();

    public abstract void loadState();

    public abstract void saveState();
}
