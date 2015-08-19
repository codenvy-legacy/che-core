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
package org.eclipse.che.ide.projecttype.wizard.runnerspage;

import org.eclipse.che.api.project.shared.dto.RunnerEnvironment;
import org.eclipse.che.api.project.shared.dto.RunnerEnvironmentLeaf;
import org.eclipse.che.api.project.shared.dto.RunnerEnvironmentTree;
import org.eclipse.che.ide.ui.tree.NodeDataAdapter;
import org.eclipse.che.ide.ui.tree.TreeNodeElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * @author Evgen Vidolob
 */
public class RunnersDataAdapter implements NodeDataAdapter<Object> {
    private static final Comparator<Object>                       COMPARATOR       = new Comparator<Object>() {
        @Override
        public int compare(Object o1, Object o2) {
            if (o1 instanceof RunnerEnvironmentTree && o2 instanceof RunnerEnvironmentLeaf) {
                return 1;
            }
            if (o2 instanceof RunnerEnvironmentTree && o1 instanceof RunnerEnvironmentLeaf) {
                return -1;
            }
            if (o1 instanceof RunnerEnvironmentTree && o2 instanceof RunnerEnvironmentTree) {
                return ((RunnerEnvironmentTree)o1).getDisplayName().compareTo(((RunnerEnvironmentTree)o2).getDisplayName());
            }
            if (o1 instanceof RunnerEnvironmentLeaf && o2 instanceof RunnerEnvironmentLeaf) {
                return ((RunnerEnvironmentLeaf)o1).getDisplayName().compareTo(((RunnerEnvironmentLeaf)o2).getDisplayName());
            }
            return 0;
        }
    };
    private              HashMap<Object, TreeNodeElement<Object>> treeNodeElements = new HashMap<>();

    @Override
    public int compare(Object a, Object b) {
        return COMPARATOR.compare(a, b);
    }

    @Override
    public boolean hasChildren(Object data) {
        if (data instanceof RunnerEnvironmentTree) {
            RunnerEnvironmentTree environmentTree = (RunnerEnvironmentTree)data;
            return !(environmentTree.getNodes().isEmpty() && environmentTree.getLeaves().isEmpty());
        }
        return false;
    }

    @Override
    public List<Object> getChildren(Object data) {
        List<Object> res = new ArrayList<>();
        if (data instanceof RunnerEnvironmentTree) {
            RunnerEnvironmentTree environmentTree = (RunnerEnvironmentTree)data;
            for (RunnerEnvironmentTree runnerEnvironmentTree : environmentTree.getNodes()) {
                res.add(runnerEnvironmentTree);
            }

            for (RunnerEnvironmentLeaf leaf : environmentTree.getLeaves()) {
                RunnerEnvironment environment = leaf.getEnvironment();
                if (environment != null) {
                    res.add(leaf);
                }
            }

        }
        Collections.sort(res, COMPARATOR);
        return res;
    }

    @Override
    public String getNodeId(Object data) {
        if (data instanceof RunnerEnvironmentTree) {
            return ((RunnerEnvironmentTree)data).getDisplayName();
        } else if (data instanceof RunnerEnvironmentLeaf) {
            return ((RunnerEnvironmentLeaf)data).getDisplayName();
        }
        return null;
    }

    @Override
    public String getNodeName(Object data) {
        return null;
    }

    @Override
    public Object getParent(Object data) {
        // TODO: implement it in order to pragmatically select a node in a tree
        return null;
    }

    @Override
    public TreeNodeElement<Object> getRenderedTreeNode(Object data) {
        return treeNodeElements.get(data);
    }

    @Override
    public void setNodeName(Object data, String name) {
    }

    @Override
    public void setRenderedTreeNode(Object data, TreeNodeElement<Object> renderedNode) {
        treeNodeElements.put(data, renderedNode);
    }

    @Override
    public Object getDragDropTarget(Object data) {
        return null;
    }

    @Override
    public List<String> getNodePath(Object data) {
        return PathUtils.getNodePath(this, data);
    }

    @Override
    public Object getNodeByPath(Object root, List<String> relativeNodePath) {
        return null;
    }

}
