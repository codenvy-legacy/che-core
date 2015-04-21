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
package org.eclipse.che.api.promises.client;

/**
 * A placeholder for a value that will be completed at a later time.
 * @param <V> the type of the 'promised' value
 */
public interface Promise<V> extends Thenable<V> {

    /**
     * Adds an action when the promise is fulfilled.<br>
     * The action is added both to the original promise and the returned value, but the promises are not
     * necessarily the same object. 
     * @param onFulfilled the action
     * @return a promise equivalent to the original promise with the action added
     */
    <B> Promise<B> then(Function<V, B> onFulfilled);

    /**
     * Adds actions when the promise is fulfilled and rejected.<br>
     * The actions are added both to the original promise and the returned value, but the promises are not
     * necessarily the same object. 
     * @param onFulfilled the fulfillment action added
     * @param onRejected the rejection action added
     * @return a promise equivalent to the original promise with the actions added
     */
    <B> Promise<B> then(Function<V, B> onFulfilled, Function<PromiseError, B> onRejected);

    <B> Promise<B> thenPromise(Function<V, Promise<B>> onFulfilled);

    /**
     * Adds actions when the promise is rejected.
     * <p>The action is added both to the original promise and the returned value, but the promises are not
     * necessarily the same object.</p>
     * <p>This is equivalent to <code>promise.then(null, onRejected)</code>.</p>
     * <p>Note: the method name is <code>catch_</code> with an underscore added, and does not match the ES6
     * specification, obviously because <code>catch</code> can't be used in java.
     * 
     * @param onRejected the rejection action added
     * @return a promise equivalent to the original promise with the action added
     */
    <B> Promise<B> catchError(Function<PromiseError, B> onRejected);

    Promise<V> then(Operation<V> onFulfilled);
    Promise<V> then(Operation<V> onFulfilled, Function<PromiseError, V> onRejected);
    Promise<V> then(Operation<V> onFulfilled, Operation<PromiseError> onRejected);

    <B> Promise<B> then(Thenable<B> thenable);

    Promise<V> catchError(Operation<PromiseError> onRejected);
}
