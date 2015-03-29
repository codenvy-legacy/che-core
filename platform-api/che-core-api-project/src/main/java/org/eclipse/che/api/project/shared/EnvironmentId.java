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
package org.eclipse.che.api.project.shared;

import javax.annotation.Nonnull;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Unique identifier of dev environment.
 *
 * @author andrew00x
 */
public class EnvironmentId {
    public enum Scope {
        system("system"),
        project("project");

        private Scope(String value) {
            this.value = value;
        }

        private final String value;

        static Scope fromValue(String value) {
            String v = value.toLowerCase();
            for (Scope e : Scope.values()) {
                if (e.value.equals(v)) {
                    return e;
                }
            }
            throw new IllegalArgumentException(value);
        }
    }

    private static final Pattern ENV_FQN_PATTERN = Pattern.compile("(system|project):/(.*)?/(.+)?");

    private final Scope  scope;
    private final String category;
    private final String name;

    /**
     * Parse environment id, that is represented by fqn, in format <i>&lt;scope&gt;:/&lt;category&gt;/&lt;name&gt;</i>. Category is
     * optional and may be empty string, e.g. <i>&lt;project&gt;://&lt;name&gt;</i>.
     *
     * @throws IllegalArgumentException
     *         if {@code fqn} is {@code null} or has unsupported format.
     */
    public static EnvironmentId parse(String fqn) {
        if (fqn == null) {
            throw new IllegalArgumentException("Null fqn isn't allowed.");
        }
        final Matcher matcher = ENV_FQN_PATTERN.matcher(fqn);
        if (matcher.matches()) {
            return new EnvironmentId(Scope.fromValue(matcher.group(1)), matcher.group(2), matcher.group(3));
        }
        throw new IllegalArgumentException("Invalid fqn: " + fqn);
    }

    /**
     * Create new identifier.
     *
     * @param scope
     *         scope of this runner environment. Null value isn't allowed.
     * @param category
     *         category of this runner environment. Category is represented by string that is separated with '/' character. Category helps
     *         represent runner environments as hierarchically-organized system. Null value is allowed.
     * @param name
     *         name of this runner environment. Scope together with category and name gives fully-qualified name of runner environment. FQN
     *         of runner environment has a following syntax: <i>&lt;scope&gt;:/&lt;category&gt;/&lt;name&gt;</i>.  Null value isn't
     *         allowed.
     * @throws IllegalArgumentException
     *         if parameter {@code scope} or {@code name} is {@code null}
     */
    public EnvironmentId(Scope scope, String category, String name) {
        if (scope == null) {
            throw new IllegalArgumentException("Null scope isn't allowed.");
        }
        if (name == null) {
            throw new IllegalArgumentException("Null name isn't allowed.");
        }
        this.scope = scope;
        this.category = category;
        this.name = name;
    }

    /**
     * Create new identifier.
     *
     * @param scope
     *         scope of this runner environment. Null value isn't allowed.
     * @param name
     *         name of this runner environment. Scope together with category and name gives fully-qualified name of runner environment. FQN
     *         of runner environment has a following syntax: <i>&lt;scope&gt;:/&lt;category&gt;/&lt;name&gt;</i>.  Null value isn't
     *         allowed.
     * @throws IllegalArgumentException
     *         if parameter {@code scope} or {@code name} is {@code null}
     */
    public EnvironmentId(Scope scope, String name) {
        this(scope, null, name);
    }

    public String getFqn() {
        String category = this.category;
        if (category == null) {
            category = "";
        }
        return scope + ":/" + category + "/" + name;
    }

    @Override
    public String toString() {
        return getFqn();
    }

    /** Gets scope of this runner environment. Scope helps identify how environment was delivered, e.g. "project", "system". */
    @Nonnull
    public Scope getScope() {
        return scope;
    }

    /**
     * Gets category of this runner environment. Category is represented by string that is separated with '/' character. Category helps
     * represent runner environments as hierarchically-organized system.
     */
    public String getCategory() {
        return category;
    }

    /**
     * Gets name of this runner environment. Scope together with category and name gives fully-qualified name of runner environment. FQN of
     * runner environment has a following syntax: <i>&lt;scope&gt;:/&lt;category&gt;/&lt;name&gt;</i>.
     */
    @Nonnull
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EnvironmentId)) {
            return false;
        }
        final EnvironmentId other = (EnvironmentId)o;
        if (scope != other.scope) {
            return false;
        }
        if (category != null ? !category.equals(other.category) : other.category != null) {
            return false;
        }
        return name.equals(other.name);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = hash * 31 + scope.hashCode();
        hash = hash * 31 + (category != null ? category.hashCode() : 0);
        hash = hash * 31 + name.hashCode();
        return hash;
    }
}
