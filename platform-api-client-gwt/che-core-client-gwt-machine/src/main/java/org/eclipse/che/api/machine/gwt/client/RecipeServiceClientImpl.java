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
package org.eclipse.che.api.machine.gwt.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;

import org.eclipse.che.api.machine.shared.dto.recipe.NewRecipe;
import org.eclipse.che.api.machine.shared.dto.recipe.RecipeDescriptor;
import org.eclipse.che.api.machine.shared.dto.recipe.RecipeUpdate;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper.RequestCall;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.AsyncRequestLoader;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.RestContext;
import org.eclipse.che.ide.rest.StringUnmarshaller;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static com.google.gwt.http.client.RequestBuilder.DELETE;
import static com.google.gwt.http.client.RequestBuilder.PUT;
import static org.eclipse.che.api.promises.client.callback.PromiseHelper.newCallback;
import static org.eclipse.che.api.promises.client.callback.PromiseHelper.newPromise;
import static org.eclipse.che.ide.MimeType.APPLICATION_JSON;
import static org.eclipse.che.ide.rest.HTTPHeader.ACCEPT;
import static org.eclipse.che.ide.rest.HTTPHeader.CONTENT_TYPE;

/**
 * Implementation for {@link RecipeServiceClient}.
 *
 * @author Artem Zatsarynnyy
 * @author Valeriy Svydenko
 */
public class RecipeServiceClientImpl implements RecipeServiceClient {
    private final DtoFactory             dtoFactory;
    private final DtoUnmarshallerFactory dtoUnmarshallerFactory;
    private final AsyncRequestFactory    asyncRequestFactory;
    private final AsyncRequestLoader     loader;
    private final String                 baseHttpUrl;

    @Inject
    protected RecipeServiceClientImpl(@RestContext String restContext,
                                      DtoFactory dtoFactory,
                                      DtoUnmarshallerFactory dtoUnmarshallerFactory,
                                      AsyncRequestFactory asyncRequestFactory,
                                      AsyncRequestLoader loader) {
        this.dtoFactory = dtoFactory;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.asyncRequestFactory = asyncRequestFactory;
        this.loader = loader;
        this.baseHttpUrl = restContext + "/recipe";
    }

    /** {@inheritDoc} */
    @Override
    public Promise<RecipeDescriptor> createRecipe(@Nonnull final NewRecipe newRecipe) {
        return newPromise(new RequestCall<RecipeDescriptor>() {
            @Override
            public void makeCall(AsyncCallback<RecipeDescriptor> callback) {
                createRecipe(newRecipe, callback);
            }
        });
    }

    private void createRecipe(@Nonnull final NewRecipe newRecipe, @Nonnull AsyncCallback<RecipeDescriptor> callback) {
        asyncRequestFactory.createPostRequest(baseHttpUrl, newRecipe)
                           .header(ACCEPT, APPLICATION_JSON)
                           .header(CONTENT_TYPE, APPLICATION_JSON)
                           .loader(loader, "Creating recipe...")
                           .send(newCallback(callback, dtoUnmarshallerFactory.newUnmarshaller(RecipeDescriptor.class)));
    }

    /** {@inheritDoc} */
    @Override
    public Promise<String> getRecipeScript(@Nonnull final String id) {
        return newPromise(new RequestCall<String>() {
            @Override
            public void makeCall(AsyncCallback<String> callback) {
                getRecipeScript(id, callback);
            }
        });
    }

    private void getRecipeScript(@Nonnull String id, @Nonnull AsyncCallback<String> callback) {
        final String url = baseHttpUrl + '/' + id + "/script";
        asyncRequestFactory.createGetRequest(url)
                           .header(ACCEPT, APPLICATION_JSON)
                           .loader(loader, "Getting recipe script...")
                           .send(newCallback(callback, new StringUnmarshaller()));
    }

    /** {@inheritDoc} */
    @Override
    public Promise<RecipeDescriptor> getRecipe(@Nonnull final String id) {
        return newPromise(new RequestCall<RecipeDescriptor>() {
            @Override
            public void makeCall(AsyncCallback<RecipeDescriptor> callback) {
                getRecipe(id, callback);
            }
        });
    }

    private void getRecipe(@Nonnull String id, @Nonnull AsyncCallback<RecipeDescriptor> callback) {
        final String url = baseHttpUrl + '/' + id;
        asyncRequestFactory.createGetRequest(url)
                           .header(ACCEPT, APPLICATION_JSON)
                           .loader(loader, "Getting recipe...")
                           .send(newCallback(callback, dtoUnmarshallerFactory.newUnmarshaller(RecipeDescriptor.class)));
    }

    /** {@inheritDoc} */
    @Override
    public Promise<List<RecipeDescriptor>> getAllRecipes() {
        return newPromise(new RequestCall<Array<RecipeDescriptor>>() {
            @Override
            public void makeCall(AsyncCallback<Array<RecipeDescriptor>> callback) {
                getRecipes(0, -1, callback);
            }
        }).then(new Function<Array<RecipeDescriptor>, List<RecipeDescriptor>>() {
            @Override
            public List<RecipeDescriptor> apply(Array<RecipeDescriptor> arg) throws FunctionException {
                final ArrayList<RecipeDescriptor> descriptors = new ArrayList<>();
                for (RecipeDescriptor descriptor : arg.asIterable()) {
                    descriptors.add(descriptor);
                }
                return descriptors;
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public Promise<List<RecipeDescriptor>> getRecipes(final int skipCount, final int maxItems) {
        return newPromise(new RequestCall<Array<RecipeDescriptor>>() {
            @Override
            public void makeCall(AsyncCallback<Array<RecipeDescriptor>> callback) {
                getRecipes(skipCount, maxItems, callback);
            }
        }).then(new Function<Array<RecipeDescriptor>, List<RecipeDescriptor>>() {
            @Override
            public List<RecipeDescriptor> apply(Array<RecipeDescriptor> arg) throws FunctionException {
                final ArrayList<RecipeDescriptor> descriptors = new ArrayList<>();
                for (RecipeDescriptor descriptor : arg.asIterable()) {
                    descriptors.add(descriptor);
                }
                return descriptors;
            }
        });
    }

    private void getRecipes(int skipCount, int maxItems, @Nonnull AsyncCallback<Array<RecipeDescriptor>> callback) {
        String url = baseHttpUrl + "?skipCount=" + skipCount;
        if (maxItems > 0) {
            url += "&maxItems=" + maxItems;
        }

        asyncRequestFactory.createGetRequest(url)
                           .header(ACCEPT, APPLICATION_JSON)
                           .loader(loader, "Getting recipes...")
                           .send(newCallback(callback, dtoUnmarshallerFactory.newArrayUnmarshaller(RecipeDescriptor.class)));
    }

    /** {@inheritDoc} */
    @Override
    public Promise<List<RecipeDescriptor>> searchRecipes(@Nullable final List<String> tags,
                                                         @Nullable final String type,
                                                         final int skipCount,
                                                         final int maxItems) {
        return newPromise(new RequestCall<Array<RecipeDescriptor>>() {
            @Override
            public void makeCall(AsyncCallback<Array<RecipeDescriptor>> callback) {
                searchRecipes(tags, type, skipCount, maxItems, callback);
            }
        }).then(new Function<Array<RecipeDescriptor>, List<RecipeDescriptor>>() {
            @Override
            public List<RecipeDescriptor> apply(Array<RecipeDescriptor> arg) throws FunctionException {
                final ArrayList<RecipeDescriptor> descriptors = new ArrayList<>();
                for (RecipeDescriptor descriptor : arg.asIterable()) {
                    descriptors.add(descriptor);
                }
                return descriptors;
            }
        });
    }

    private void searchRecipes(@Nullable List<String> tags,
                               @Nullable String type,
                               int skipCount,
                               int maxItems,
                               @Nonnull AsyncCallback<Array<RecipeDescriptor>> callback) {
        final StringBuilder tagsParam = new StringBuilder();
        if (tags != null && !tags.isEmpty()) {
            for (String tag : tags) {
                tagsParam.append("tags=").append(tag).append("&");
            }
            tagsParam.deleteCharAt(tagsParam.length() - 1);
        }

        final String url = baseHttpUrl + "/list?" + tagsParam.toString() +
                           "&type=" + type +
                           "&skipCount=" + skipCount +
                           "&maxItems=" + maxItems;
        asyncRequestFactory.createGetRequest(url)
                           .header(ACCEPT, APPLICATION_JSON)
                           .send(newCallback(callback, dtoUnmarshallerFactory.newArrayUnmarshaller(RecipeDescriptor.class)));
    }

    /** {@inheritDoc} */
    @Override
    public Promise<RecipeDescriptor> updateRecipe(@Nonnull final String id, @Nonnull final RecipeUpdate recipeUpdate) {
        return newPromise(new RequestCall<RecipeDescriptor>() {
            @Override
            public void makeCall(AsyncCallback<RecipeDescriptor> callback) {
                updateCommand(id, recipeUpdate, callback);
            }
        });
    }

    private void updateCommand(@Nonnull final String id,
                               @Nonnull RecipeUpdate recipeUpdate,
                               @Nonnull AsyncCallback<RecipeDescriptor> callback) {
        final String url = baseHttpUrl + '/' + id;
        asyncRequestFactory.createRequest(PUT, url, recipeUpdate, false)
                           .header(ACCEPT, APPLICATION_JSON)
                           .header(CONTENT_TYPE, APPLICATION_JSON)
                           .loader(loader, "Updating recipe...")
                           .send(newCallback(callback, dtoUnmarshallerFactory.newUnmarshaller(RecipeDescriptor.class)));
    }

    /** {@inheritDoc} */
    @Override
    public Promise<Void> removeRecipe(@Nonnull final String id) {
        return newPromise(new RequestCall<Void>() {
            @Override
            public void makeCall(AsyncCallback<Void> callback) {
                removeRecipe(id, callback);
            }
        });
    }

    private void removeRecipe(@Nonnull String id, @Nonnull AsyncCallback<Void> callback) {
        asyncRequestFactory.createRequest(DELETE, baseHttpUrl + '/' + id, null, false)
                           .loader(loader, "Deleting recipe...")
                           .send(newCallback(callback));
    }
}
