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
package org.eclipse.che.api.core.recipe;

import javax.annotation.Nonnull;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Unique identifier of recipe in format: scope:/[ca/te/go/ry]/name.
 *
 * @author andrew00x
 */
public class RecipeId {
    public enum Scope {
        system("system"),
        project("project"),
        user("user");

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

    private static final Pattern ENV_FQN_PATTERN = Pattern.compile("(system|project):(/.*)?/(.+)?");

    private final Scope  scope;
    private final String category;
    private final String name;

    /**
     * Parse recipe id, that is represented by fqn, in format <i>&lt;scope&gt;:/&lt;category&gt;/&lt;name&gt;</i>. Category is
     * optional and may be empty string, e.g. <i>&lt;project&gt;://&lt;name&gt;</i>.
     *
     * @throws IllegalArgumentException
     *         if {@code fqn} is {@code null} or has unsupported format.
     */
    public static RecipeId parse(String fqn) {
        if (fqn == null) {
            throw new IllegalArgumentException("Null fqn isn't allowed.");
        }
        final Matcher matcher = ENV_FQN_PATTERN.matcher(fqn);
        if (matcher.matches()) {
            return new RecipeId(Scope.fromValue(matcher.group(1)), matcher.group(2), matcher.group(3));
        }
        throw new IllegalArgumentException("Invalid fqn: " + fqn);
    }

    /**
     * Create new identifier.
     *
     * @param scope
     *         scope of this recipe. Null value isn't allowed.
     * @param category
     *         category of this recipe. Category is represented by string that is separated with '/' character. Category helps
     *         represent recipe as hierarchically-organized system. Null value is allowed.
     * @param name
     *         name of this recipe. Scope together with category and name gives fully-qualified name of recipe. FQN
     *         of recipe has a following syntax: <i>&lt;scope&gt;:/&lt;category&gt;/&lt;name&gt;</i>.  Null value isn't
     *         allowed.
     * @throws IllegalArgumentException
     *         if parameter {@code scope} or {@code name} is {@code null}
     */
    public RecipeId(Scope scope, String category, String name) {
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
     *         scope of this recipe. Null value isn't allowed.
     * @param name
     *         name of this recipe. Scope together with category and name gives fully-qualified name of recipe. FQN
     *         of recipe has a following syntax: <i>&lt;scope&gt;:/&lt;category&gt;/&lt;name&gt;</i>.  Null value isn't
     *         allowed.
     * @throws IllegalArgumentException
     *         if parameter {@code scope} or {@code name} is {@code null}
     */
    public RecipeId(Scope scope, String name) {
        this(scope, null, name);
    }

    public String getFqn() {
        String category = this.category;
        String fqn = scope + ":/";
        return category != null && !category.isEmpty() ? fqn + category + "/" + name : fqn + name;
    }

    @Override
    public String toString() {
        return getFqn();
    }

    /** Gets scope of this recipe. Scope helps identify how recipe was delivered, e.g. "project", "system". */
    @Nonnull
    public Scope getScope() {
        return scope;
    }

    /**
     * Gets category of this recipe. Category is represented by string that is separated with '/' character. Category helps
     * represent recipe as hierarchically-organized system.
     */
    public String getCategory() {
        if (category.startsWith("/")) {
            return category.substring(1);
        }

        return category;
    }

    /**
     * Gets name of this recipe. Scope together with category and name gives fully-qualified name of recipe. FQN of
     * recipe has a following syntax: <i>&lt;scope&gt;:/&lt;category&gt;/&lt;name&gt;</i>.
     */
    @Nonnull
    public String getName() {
        return name;
    }

    /**
     * Gets path of this recipe. Path it is concatenation of Scope together with category and name gives fully-qualified name of recipe. FQN of
     * recipe has a following syntax: <i>&lt;scope&gt;:/&lt;category&gt;/&lt;name&gt;</i>.
     */
    @Nonnull
    public String getPath() {
        return category+"/"+name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RecipeId)) {
            return false;
        }
        final RecipeId other = (RecipeId)o;
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
