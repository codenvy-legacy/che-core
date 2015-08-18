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

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;

import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.ui.smartTree.Tree;
import org.eclipse.che.ide.ui.smartTree.event.CollapseNodeEvent;
import org.eclipse.che.ide.ui.smartTree.event.CollapseNodeEvent.CollapseNodeHandler;
import org.eclipse.che.ide.ui.smartTree.event.ExpandNodeEvent;
import org.eclipse.che.ide.ui.smartTree.event.ExpandNodeEvent.ExpandNodeHandler;
import org.eclipse.che.ide.ui.smartTree.event.StoreDataChangeEvent;
import org.eclipse.che.ide.ui.smartTree.event.StoreDataChangeEvent.StoreDataChangeHandler;
import org.eclipse.che.ide.ui.smartTree.event.StoreUpdateEvent;
import org.eclipse.che.ide.ui.smartTree.event.StoreUpdateEvent.StoreUpdateHandler;
import org.eclipse.che.ide.util.loging.Log;

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
            String key = tree.getNodeStorage().getKeyProvider().getKey(event.getNode());
            getState().remove(key);

            saveState();
        }

        @Override
        public void onExpand(ExpandNodeEvent event) {
            String key = tree.getNodeStorage().getKeyProvider().getKey(event.getNode());
            getState().add(key);

            saveState();
        }

        @Override
        public void onDataChange(StoreDataChangeEvent event) {
            applyState();
        }


        @Override
        public void onUpdate(StoreUpdateEvent event) {
            Log.info(this.getClass(), "onUpdate():76: " + "state handler on update fired");
//            getState().clear();
//            tree.getNodeStorage().
//            String key = tree.getNodeStorage().getKeyProvider().getKey(event.getNode());
//            getState().add(key);
//
//            saveState();
        }
    }


    public void applyState() {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                for (String key : getState()) {
                    Node item = tree.getNodeStorage().findNodeWithKey(key);
                    if (item != null && !tree.isExpanded(item)) {
                        tree.setExpanded(item, true);
                    }
                }
            }
        });
    }

    public void loadState() {
        String value = stateProvider.getValue(DEF_KEY);
        JSONValue jsonValue = JSONParser.parseStrict(value);
        if (jsonValue.isArray() != null) {
            JSONArray array = jsonValue.isArray();
            state.clear();

            for (int i = 0; i < array.size(); i++) {
                state.add(array.get(i).isString().stringValue());
            }
        }

        applyState();
    }

    public void saveState() {
        String[] array = state.toArray(new String[state.size()]);

        JSONArray obj = new JSONArray();
        for (int i = 0; i < array.length; i++) {
            obj.set(i, new JSONString(array[i]));
        }
        stateProvider.setValue(DEF_KEY, obj.toString());
    }
}
