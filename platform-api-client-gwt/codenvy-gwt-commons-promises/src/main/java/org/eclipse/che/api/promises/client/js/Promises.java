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
package org.eclipse.che.api.promises.client.js;

import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayMixed;

import elemental.js.util.JsArrayOf;
import elemental.util.ArrayOf;


public final class Promises {

    /** Private constructor, the class is not instantiable. */
    private Promises() {}

    /**
     * Creates a new promise using the provided executor.
     * @param conclusion the executor
     * @return a promise
     * @param <V> the type of the promised value
     */
    public static final native <V> JsPromise<V> create(Executor<V> conclusion) /*-{
        return new Promise(conclusion);
    }-*/;

    /**
     * Creates a promise that resolves as soon as all the promises used as parameters are resolved or rejected
     * as soon as the first rejection happens on one of the included promises.
     * @param promises the included promises
     * @return a promise with an array of unit values as fulfillment value
     */
    public static final native JsPromise<JsArrayMixed> all(ArrayOf<Promise<?>> promises) /*-{
        return Promise.all(promises);
    }-*/;

    public static final JsPromise<JsArrayMixed> all(final Promise<?>... promises) {
        final JsArrayOf<Promise<?>> promisesArray = JavaScriptObject.createArray().cast();
        for (final Promise<?> promise: promises) {
            promisesArray.push(promise);
        }
        return all(promisesArray);
    }

    public static final native <U> JsPromise<U> reject(PromiseError reason) /*-{
        return Promise.reject(reason);
    }-*/;

    public static final native <U> JsPromise<U> resolve(U value) /*-{
        return Promise.resolve(value);
    }-*/;
}
