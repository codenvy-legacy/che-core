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

    public static final JsPromiseError create(final Throwable e) {
        final StackTraceElement[] stack = e.getStackTrace();
        JsPromiseError result;
        if (stack != null && stack.length != 0) {
            result = create(e.getMessage(), stack[0].getFileName(), Integer.toString(stack[0].getLineNumber()));
            result.setStack(stack);
         } else {
             result = create(e.getMessage());
         }
         return result;
    }

    private final void setStack(final StackTraceElement[] stack) {
        // TODO
    }

    public final native String getMessage() /*-{
        return this.message;
    }-*/;

    public final native String getName() /*-{
        return this.name;
    }-*/;
}
