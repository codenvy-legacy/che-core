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

import com.google.gwt.core.client.JavaScriptObject;

/**
 * The executor is the conclusion callback, a js function with two parameters, usually named
 * resolve and reject. The first argument fulfills the promise, the second argument rejects it.
 * @param <V> the type of the promised value
 */
public class Executor<V> extends JavaScriptObject {

    /** JSO mandated protected constructor. */
    protected Executor() {}

    /*
     * The first parameter is the fulfillment function, the second is the rejection function. Both
     * functions accept one argument.
     * These functions are handed by the promise implementation
     * The resolve function single parameter is the eventual value that's born by the promise
     * The reject function single parameter is the cause of the rejection
     */

    /**
     * Creates an executor.
     * @param executorBody the body of the executor
     * @return the new executor
     * @param <V> the fulfillment value
     */
    public static final native <V> Executor<V> create(ExecutorBody<V> executorBody) /*-{
        return function(resolve, reject) {
            try {
                executorBody.@org.eclipse.che.api.promises.client.js.Executor.ExecutorBody::apply(*)(resolve, reject);
            } catch (e) {
                reject(e);
            }
        }
    }-*/;

    /**
     * The definition of an executor.
     * @param <V> the type of the fulfillment value
     */
    public interface ExecutorBody<V> {
        /**
         * The executor describes what the promise must do in order to be fulfilled.
         * It will execute some code to process or retrieve some value, then use the <code>resolve</code> or
         * <code>reject</code> callback to conclude.
         * @param resolve what to do on success
         * @param reject what to do on failure
         */
        void apply(ResolveFunction<V> resolve, RejectFunction reject);
    }
}
