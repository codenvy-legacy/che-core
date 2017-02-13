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
package org.eclipse.che.dto.server;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * Wraps Gson's default List/Map adapter factories serialize null List/Map fields as empty instead.
 * 
 * @author tareq.sha@gmail.com
 */
public class NullAsEmptyTAF<U> implements TypeAdapterFactory {

    private final Class<?> matchedClass;
    private final U defaultValue;

    public NullAsEmptyTAF(Class<U> matchedClass, U defaultValue) {
        this.matchedClass = matchedClass;
        this.defaultValue = defaultValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!matchedClass.isAssignableFrom(type.getRawType())) {
            return null;
        }
        TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
        return new TypeAdapter<T>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                delegate.write(out, value != null ? value : (T) defaultValue);
            }

            @Override
            public T read(JsonReader in) throws IOException {
                return delegate.read(in);
            }
        };
    }

}
