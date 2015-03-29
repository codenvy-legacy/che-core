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
package org.eclipse.che.ide.output.event;

import com.google.gwt.event.shared.GwtEvent;

/** @author <a href="mailto:gavrikvetal@gmail.com">Vitaliy Gulyy</a> */

public class OutputEvent extends GwtEvent<OutputHandler> {

    public static final GwtEvent.Type<OutputHandler> TYPE = new GwtEvent.Type<OutputHandler>();

    private String message;

    private OutputMessage.Type outputType;

    public OutputEvent(String message) {
        this.message = message;
        outputType = OutputMessage.Type.INFO;
    }

    public OutputEvent(String message, OutputMessage.Type outputType) {
        this.message = message;
        this.outputType = outputType;
    }

    @Override
    protected void dispatch(OutputHandler handler) {
        handler.onOutput(this);
    }

    /** @return the message */
    public String getMessage() {
        return message;
    }

    /** @return the outputType */
    public OutputMessage.Type getOutputType() {
        return outputType;
    }

    @Override
    public com.google.gwt.event.shared.GwtEvent.Type<OutputHandler> getAssociatedType() {
        return TYPE;
    }
}