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
package org.eclipse.che.api.local;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.inject.Inject;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.machine.server.dao.RecipeDao;
import org.eclipse.che.api.machine.shared.Recipe;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.lang.String.format;

/**
 * @author Eugene Voevodin
 */
@Singleton
public class LocalRecipeDaoImpl implements RecipeDao {

    private final Map<String, Recipe> recipes;
    private final ReadWriteLock       lock;

    @Inject
    public LocalRecipeDaoImpl(@Named("codenvy.local.infrastructure.recipes") Set<Recipe> recipes) {
        this.recipes = new HashMap<>();
        lock = new ReentrantReadWriteLock();
        try {
            for (Recipe recipe : recipes) {
                create(recipe);
            }
        } catch (Exception ex) {
            // fail if can't validate this instance properly
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void create(Recipe recipe) throws ConflictException {
        lock.writeLock().lock();
        try {
            if (recipes.containsKey(recipe.getId())) {
                throw new ConflictException(format("Recipe with id %s already exists", recipe.getId()));
            }
            recipes.put(recipe.getId(), recipe);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void update(Recipe recipe) throws NotFoundException {
        lock.writeLock().lock();
        try {
            final Recipe target = recipes.get(recipe.getId());
            if (target == null) {
                throw new NotFoundException(format("Recipe with id %s was not found", recipe.getId()));
            }
            target.setType(recipe.getType());
            target.setScript(recipe.getScript());
            target.setTags(recipe.getTags());
            target.setPermissions(recipe.getPermissions());
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void remove(String id) {
        lock.writeLock().lock();
        try {
            recipes.remove(id);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Recipe getById(String id) throws NotFoundException {
        lock.readLock().lock();
        try {
            final Recipe recipe = recipes.get(id);
            if (recipe == null) {
                throw new NotFoundException(format("Recipe with id %s was not found", id));
            }
            return recipe;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Recipe> search(final List<String> tags, final String type, int skipCount, int maxItems) {
        lock.readLock().lock();
        try {
            return FluentIterable.from(recipes.values())
                                 .skip(skipCount)
                                 .filter(new Predicate<Recipe>() {
                                     @Override
                                     public boolean apply(Recipe recipe) {
                                         return recipe.getTags().containsAll(tags) && (type == null || type.equals(recipe.getType()));
                                     }
                                 })
                                 .limit(maxItems)
                                 .toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Recipe> getByCreator(final String creator, int skipCount, int maxItems) {
        lock.readLock().lock();
        try {
            return FluentIterable.from(recipes.values())
                                 .skip(skipCount)
                                 .filter(new Predicate<Recipe>() {
                                     @Override
                                     public boolean apply(Recipe recipe) {
                                         return recipe.getCreator().equals(creator);
                                     }
                                 })
                                 .limit(maxItems)
                                 .toList();
        } finally {
            lock.readLock().unlock();
        }
    }
}
