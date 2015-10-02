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
import org.eclipse.che.ide.ui.smartTree.event.CollapseNodeEvent;
import org.eclipse.che.ide.ui.smartTree.event.CollapseNodeEvent.CollapseNodeHandler;
import org.eclipse.che.ide.ui.smartTree.event.ExpandNodeEvent;
import org.eclipse.che.ide.ui.smartTree.event.ExpandNodeEvent.ExpandNodeHandler;
import org.eclipse.che.ide.ui.smartTree.event.StoreDataChangeEvent;
import org.eclipse.che.ide.ui.smartTree.event.StoreDataChangeEvent.StoreDataChangeHandler;
import org.eclipse.che.ide.ui.smartTree.event.StoreUpdateEvent;
import org.eclipse.che.ide.ui.smartTree.event.StoreUpdateEvent.StoreUpdateHandler;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Vlad Zhukovskiy
 */
public class ExpandStateHandler extends AbstractStateHandler<Set<String>> {

    public static final String DEF_KEY = "expandedNodes";

    public ExpandStateHandler(Tree tree, AbstractStateProvider stateProvider) {
        super(tree, stateProvider);

        Handler h = new Handler();
        tree.addCollapseHandler(h);
        tree.addExpandHandler(h);
        tree.getNodeStorage().addStoreDataChangeHandler(h);
        tree.setExpandStateHandler(this);

        state = new HashSet<>();
    }

    private class Handler implements CollapseNodeHandler, ExpandNodeHandler, StoreDataChangeHandler, StoreUpdateHandler {
        @Override
        public void onCollapse(CollapseNodeEvent event) {
        }

        @Override
        public void onExpand(ExpandNodeEvent event) {
        }

        @Override
        public void onDataChange(StoreDataChangeEvent event) {
        }


        @Override
        public void onUpdate(StoreUpdateEvent event) {
        }
    }


    public void applyState() {
    }

    public void loadState() {
    }

    public void saveState() {
    }
}
