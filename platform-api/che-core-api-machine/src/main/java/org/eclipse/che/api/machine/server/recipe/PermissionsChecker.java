/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.machine.server.recipe;

import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.machine.shared.Permissible;
import org.eclipse.che.api.machine.shared.dto.recipe.GroupDescriptor;
import org.eclipse.che.api.machine.shared.dto.recipe.PermissionsDescriptor;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.user.User;

//TODO add methods for 'all' 'any' strategies

/**
 * Helps to check access to Entity.
 *
 * @author Eugene Voevodin
 * @author Alexander Andrienko
 */
public interface PermissionsChecker {

    /**
     * Checks that user with id {@code userId} has access to target with given {@code permission}
     *
     * @param target
     *          entity to check permissions
     * @param userId
     *         user identifier
     * @param creator
     *          creator identifier
     * @param permission
     *          permission which user should have to access entity
     * @return {@code true} when user with identifier {@code userId} has access
     *          to {@code target} with given {@code permission} otherwise returns {@code false}
     * @throws ServerException
     *         when any error occurs while checking access
     */
    boolean hasAccess(Permissible target, String userId, String creator, String permission) throws ServerException;

    /**
     * Checks that user with given {@code permissions} has public access to entity
     *
     * @param permissions
     *          which user should have to public access entity
     * @return {@code true}
     *          when user has public access to entity, otherwise returns {@code false}
     */
    default boolean hasPublicAccess(PermissionsDescriptor permissions) {
        final User user = EnvironmentContext.getCurrent().getUser();
        if (!user.isMemberOf("system/admin") && !user.isMemberOf("system/manager")) {
            for (GroupDescriptor group : permissions.getGroups()) {
                if ("public".equalsIgnoreCase(group.getName()) && group.getAcl().contains("search")) {
                    return true;
                }
            }
        }
        return false;
    }
}