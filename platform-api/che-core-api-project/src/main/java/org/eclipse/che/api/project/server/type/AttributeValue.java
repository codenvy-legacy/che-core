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
package org.eclipse.che.api.project.server.type;

import java.util.ArrayList;
import java.util.List;

/**
 * @author gazarenkov
 */
public class AttributeValue {

    private final List<String> values = new ArrayList<>();

    public AttributeValue(List<String> list) {
        values.addAll(list);
    }

    public AttributeValue(String str) {
        values.add(str);
    }

    public String getString() {
        return values.isEmpty()?null:values.get(0);
    }

    public void setString(String str) {
        values.clear();
        values.add(str);
    }

    public List<String> getList() {
        return values;
    }

    public void setList(List<String> list) {
        values.clear();
        values.addAll(list);
    }

    @Override
    public boolean equals(Object obj) {

        if(obj instanceof AttributeValue) {
            return this.values.equals(((AttributeValue)obj).getList());
        }
        return false;
    }

    //ValueType getType();

}
