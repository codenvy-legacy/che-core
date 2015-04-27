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
package org.eclipse.che.api.machine.server.dao;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.machine.shared.Recipe;

import java.util.List;

/**
 * TODO add docs
 *
 * @author Eugene Voevodin
 */
public interface RecipeDao {

    void create(Recipe recipe) throws ServerException, ConflictException; //persist

    void update(Recipe recipe) throws ServerException, NotFoundException; //refresh

    void remove(String id) throws ServerException, NotFoundException;

    Recipe getById(String id) throws ServerException, NotFoundException;

    List<Recipe> search(List<String> tags, String type) throws ServerException;

    List<Recipe> getByCreator(String creator) throws ServerException;
}
