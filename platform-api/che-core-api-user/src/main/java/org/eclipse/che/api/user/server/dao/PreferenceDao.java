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
package org.eclipse.che.api.user.server.dao;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;

import java.util.Map;

/**
 * @author Eugene Voevodin
 */
public interface PreferenceDao {

    /**
     * Saves preferences for user
     *
     * @param userId
     *         user identifier
     * @param preferences
     *         preferences to setPreferences
     */
    void setPreferences(String userId, Map<String, String> preferences) throws ServerException, NotFoundException;

    /**
     * Loads preferences
     *
     * @param userId
     *         user identifier
     * @return user preferences, if user doesn't have preferences - empty upgradable map will be returned
     */
    Map<String, String> getPreferences(String userId) throws ServerException;

    /**
     * Loads filtered preferences
     *
     * @param userId
     *         user identifier
     * @param filter
     *         regex for preferences keys filtering
     * @return filtered preferences, if matched preferences don't exist - empty upgradable map will be returned
     */
    Map<String, String> getPreferences(String userId, String filter) throws ServerException;

    /**
     * Removes preferences
     *
     * @param userId
     *         user identifier
     */
    void remove(String userId) throws ServerException;
}
