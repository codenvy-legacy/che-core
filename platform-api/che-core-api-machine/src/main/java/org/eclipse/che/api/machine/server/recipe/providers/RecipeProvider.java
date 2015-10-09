package org.eclipse.che.api.machine.server.recipe.providers;

import com.google.inject.Provider;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author Anton Korneta
 */
public class RecipeProvider implements Provider<String> {
    @Inject
    @Named("local.recipe.path")
    public String path;

    @Override
    public String get() {
        return path;
    }
}
