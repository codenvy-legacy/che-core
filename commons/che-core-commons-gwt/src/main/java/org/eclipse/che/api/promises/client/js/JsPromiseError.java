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
package org.eclipse.che.api.promises.client.js;

import org.eclipse.che.api.promises.client.PromiseError;

import com.google.gwt.core.client.JavaScriptObject;

public class JsPromiseError extends JavaScriptObject implements PromiseError {

    protected JsPromiseError() {
    }

    public static final native JsPromiseError create() /*-{
        return new Error();
    }-*/;

    public static final native JsPromiseError create(String message, String filename, String linenumber) /*-{
        return new Error(message, filename, linenumber);
    }-*/;

    public static final native JsPromiseError create(String message, String filename) /*-{
        return new Error(message, filename);
    }-*/;

    public static final native JsPromiseError create(String message) /*-{
        return new Error(message);
    }-*/;

    public static final native JsPromiseError create(JavaScriptObject object) /*-{
        return object;
    }-*/;

    public static final JsPromiseError create(final Throwable e) {
        if (e == null) {
            return create();
        } else {
            return createFromThrowable(e);
        }
    }

    private static final native JsPromiseError createFromThrowable(final Throwable e) /*-{
        var message = e.@java.lang.Throwable::getMessage()();
        var result = new Error(message);
        result.cause = e;
        return result;
    }-*/;

    private final void setStack(final StackTraceElement[] stack) {
        // TODO
    }

    public final native String getMessage() /*-{
        return this.message;
    }-*/;

    public final native String getName() /*-{
        return this.name;
    }-*/;

    @Override
    public final native Throwable getCause() /*-{
        return this.cause;
    }-*/;
}
