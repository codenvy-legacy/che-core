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
package org.eclipse.che.api.project.shared.dto;

import org.eclipse.che.dto.shared.DTO;
import org.eclipse.che.dto.shared.DelegateRule;
import org.eclipse.che.dto.shared.DelegateTo;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Represents RunnerEnvironment as tree.
 *
 * @author andrew00x
 */
@DTO
public interface RunnerEnvironmentTree {
    enum Util {
        INSTANCE;

        public static RunnerEnvironmentTree getNode(RunnerEnvironmentTree tree, String name) {
            for (RunnerEnvironmentTree node : tree.getNodes()) {
                if (node.getDisplayName().equals(name)) {
                    return node;
                }
            }
            return null;
        }

        public static void addNode(RunnerEnvironmentTree tree, RunnerEnvironmentTree node) {
            tree.getNodes().add(node);
        }

        public static RunnerEnvironmentLeaf getLeaf(RunnerEnvironmentTree tree, String name) {
            for (RunnerEnvironmentLeaf leaf : tree.getLeaves()) {
                if (leaf.getDisplayName().equals(name)) {
                    return leaf;
                }
            }
            return null;
        }

        public static void addLeaf(RunnerEnvironmentTree tree, RunnerEnvironmentLeaf leaf) {
            tree.getLeaves().add(leaf);
        }
    }


    /**
     * Gets runner environments on current tree level. If this method returns empty {@code List} that means there is no any runner
     * environments on current level. Always need check {@link #getNodes()} for child environments.
     * <pre>
     *     + Java
     *       |- Web
     *         |- Spring
     *           |- Tomcat7
     *           |- JBoss7
     *           |- Jetty9
     *         |- Struts
     *           |- ...
     *     + Python
     *       |- Web
     *         |- ...
     *     + Ruby
     *       |- Web
     *         |- Rails
     *           |- ...
     * </pre>
     * In example above there is no any environment on level Java, Java/Web.
     */
    List<RunnerEnvironmentLeaf> getLeaves();

    /**
     * Gets runner environments on current tree level.
     *
     * @see #getLeaves()
     */
    void setLeaves(List<RunnerEnvironmentLeaf> leaves);

    RunnerEnvironmentTree withLeaves(List<RunnerEnvironmentLeaf> leaves);

    /** Gets node name. Need this for display tree on client side. */
    @NotNull
    String getDisplayName();

    /**
     * Sets display name.
     *
     * @see #getDisplayName()
     */
    void setDisplayName(@NotNull String name);

    RunnerEnvironmentTree withDisplayName(@NotNull String name);

    /**
     * Gets child environments. Empty list means that current tree level is last in hierarchy.
     */
    @NotNull
    List<RunnerEnvironmentTree> getNodes();

    /**
     * Sets child environments.
     *
     * @see #getNodes()
     */
    void setNodes(List<RunnerEnvironmentTree> nodes);

    RunnerEnvironmentTree withNodes(List<RunnerEnvironmentTree> nodes);

    @DelegateTo(client = @DelegateRule(type = Util.class, method = "getNode"),
                server = @DelegateRule(type = Util.class, method = "getNode"))
    RunnerEnvironmentTree getNode(String name);

    @DelegateTo(client = @DelegateRule(type = Util.class, method = "addNode"),
                server = @DelegateRule(type = Util.class, method = "addNode"))
    void addNode(RunnerEnvironmentTree node);

    @DelegateTo(client = @DelegateRule(type = Util.class, method = "getLeaf"),
                server = @DelegateRule(type = Util.class, method = "getLeaf"))
    RunnerEnvironmentLeaf getEnvironment(String name);

    @DelegateTo(client = @DelegateRule(type = Util.class, method = "addLeaf"),
                server = @DelegateRule(type = Util.class, method = "addLeaf"))
    void addLeaf(RunnerEnvironmentLeaf leaf);
}
