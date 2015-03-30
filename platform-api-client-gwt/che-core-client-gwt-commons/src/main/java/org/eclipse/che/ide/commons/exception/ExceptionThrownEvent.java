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
package org.eclipse.che.ide.commons.exception;

import com.google.gwt.event.shared.GwtEvent;

/** @author <a href="mailto:gavrikvetal@gmail.com">Vitaliy Gulyy</a> */

public class ExceptionThrownEvent extends ServerExceptionEvent<ExceptionThrownHandler> {

    private String errorMessage;

    public static final GwtEvent.Type<ExceptionThrownHandler> TYPE = new GwtEvent.Type<ExceptionThrownHandler>();

    @Override
    public GwtEvent.Type<ExceptionThrownHandler> getAssociatedType() {
        return TYPE;
    }

    public ExceptionThrownEvent(Throwable throwable) {
        this(throwable, null);
    }

    public ExceptionThrownEvent(String errorMessage) {
        this(null, errorMessage);
    }

    public ExceptionThrownEvent(Throwable throwable, String errorMesage) {
        super(throwable);
        this.errorMessage = errorMesage;
    }

    @Override
    protected void dispatch(ExceptionThrownHandler handler) {
        handler.onError(this);
    }

    public String getErrorMessage() {
        return errorMessage;
    }

}
