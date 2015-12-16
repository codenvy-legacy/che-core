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
package org.eclipse.che.ide.rest;

import org.eclipse.che.ide.dto.DtoFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;

/**
 * Provides implementations of Unmarshallable instances to
 * deserialize HTTP request result or WebSocket message to DTO.
 *
 * @author Artem Zatsarynnyi
 */
@Singleton
public class DtoUnmarshallerFactory {
    private DtoFactory dtoFactory;

    @Inject
    public DtoUnmarshallerFactory(DtoFactory dtoFactory) {
        this.dtoFactory = dtoFactory;
    }

    /**
     * Create new instance of {@link org.eclipse.che.ide.rest.Unmarshallable}
     * to deserialize HTTP request to DTO.
     *
     * @param dtoType
     *         type of DTO.
     * @return new instance of {@link org.eclipse.che.ide.rest.Unmarshallable}
     * @see org.eclipse.che.dto.shared.DTO
     */
    public <T> org.eclipse.che.ide.rest.Unmarshallable<T> newUnmarshaller(Class<T> dtoType) {
        return new DtoUnmarshaller<>(dtoType, dtoFactory);
    }

    /**
     * Create new instance of {@link org.eclipse.che.ide.rest.Unmarshallable}
     * to deserialize HTTP request to {@link org.eclipse.che.ide.collections.Array} of DTO.
     *
     * @param dtoType
     *         type of DTO
     * @return new instance of {@link org.eclipse.che.ide.rest.Unmarshallable}
     * @see org.eclipse.che.dto.shared.DTO
     */
    public <T> org.eclipse.che.ide.rest.Unmarshallable<List<T>> newListUnmarshaller(Class<T> dtoType) {
        return new DtoUnmarshaller<>(dtoType, dtoFactory);
    }

    /**
     * Create new instance of {@link org.eclipse.che.ide.websocket.rest.Unmarshallable}
     * to deserialize WebSocket message to DTO.
     *
     * @param dtoType
     *         type of DTO
     * @return new instance of {@link org.eclipse.che.ide.websocket.rest.Unmarshallable}
     * @see org.eclipse.che.dto.shared.DTO
     */
    public <T> org.eclipse.che.ide.websocket.rest.Unmarshallable<T> newWSUnmarshaller(Class<T> dtoType) {
        return new org.eclipse.che.ide.websocket.rest.DtoUnmarshaller<>(dtoType, dtoFactory);
    }

    /**
     * Create new instance of {@link org.eclipse.che.ide.websocket.rest.Unmarshallable}
     * to deserialize WebSocket message to {@link List} of DTO.
     *
     * @param dtoType
     *         type of DTO
     * @return new instance of {@link org.eclipse.che.ide.websocket.rest.Unmarshallable}
     * @see org.eclipse.che.dto.shared.DTO
     */
    public <T> org.eclipse.che.ide.websocket.rest.Unmarshallable<List<T>> newWSListUnmarshaller(Class<T> dtoType) {
        return new org.eclipse.che.ide.websocket.rest.DtoUnmarshaller<>(dtoType, dtoFactory);
    }
}
