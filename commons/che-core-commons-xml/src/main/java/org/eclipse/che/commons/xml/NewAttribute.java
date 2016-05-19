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
package org.eclipse.che.commons.xml;

/**
 * Describes new attribute.
 * Should be used to insert new attribute into existing tree
 * element or may be a part of {@link NewElement}.
 *
 * @author Eugene Voevodin
 */
public final class NewAttribute extends QName {

    private String value;

    public NewAttribute(String qName, String value) {
        super(qName);
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public String asString() {
        return getName() + '=' + '"' + value + '"';
    }

    @Override
    public String toString() {
        return asString();
    }
}
