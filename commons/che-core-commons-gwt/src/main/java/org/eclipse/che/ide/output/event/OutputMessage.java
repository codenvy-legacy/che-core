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

/** @author <a href="mailto:gavrikvetal@gmail.com">Vitaliy Gulyy</a> */

public class OutputMessage {

    public enum Type {

        INFO, ERROR, WARNING, LOG, OUTPUT

    }

    private String message;

    private OutputMessage.Type type;

    public OutputMessage(String message, OutputMessage.Type type) {
        this.message = message;
        this.type = type;
    }

    /** @return the message */
    public String getMessage() {
        return message;
    }

    /** @return the type */
    public OutputMessage.Type getType() {
        return type;
    }
}