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
package org.eclipse.che.api.factory;

import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.factory.dto.Factory;
import org.eclipse.che.commons.lang.Pair;

import java.util.List;
import java.util.Set;

/** Interface for CRUD operations with factory data. */
public interface FactoryStore {
    /**
     * Save factory at storage.
     *
     * @param factoryUrl
     *         - factory information
     * @param images
     *         - factory images
     * @return - if of stored factory
     * @throws org.eclipse.che.api.core.ApiException
     */
    public String saveFactory(Factory factoryUrl, Set<FactoryImage> images) throws ApiException;

    /**
     * Remove factory by id
     *
     * @param id
     *         - id of factory to remove
     * @throws org.eclipse.che.api.core.ApiException
     */
    public void removeFactory(String id) throws ApiException;

    /**
     * Retrieve factory data by its id
     *
     * @param id
     *         - factory id
     * @return - {@code AdvancedFactoryUrl} if factory exist and found, null otherwise
     * @throws org.eclipse.che.api.core.ApiException
     */
    public Factory getFactory(String id) throws ApiException;

    /**
     * Retrieve factory by given attribute name and value.
     *
     * @param  attributes
     *              - attribute pairs to search for
     *
     * @return - List {@code AdvancedFactoryUrl} if factory(s) exist and found, empty list otherwise
     * @throws org.eclipse.che.api.core.ApiException
     */
    public List<Factory> findByAttribute(Pair<String, String>... attributes) throws ApiException;

    /**
     * Retrieve factory images by factory id
     *
     * @param factoryId
     *         - factory id
     * @param imageId
     *         - id of the requested image. When null, all images for given factory will be returned.
     * @return - {@code Set} of images if factory found, empty set otherwise
     * @throws org.eclipse.che.api.core.ApiException
     */
    public Set<FactoryImage> getFactoryImages(String factoryId, String imageId) throws ApiException;

    /**
     * Update factory at storage.
     *
     * @param factoryId
     *         - factory information
     * @param factory
     *         - factory information
     * @return - if of stored factory
     * @throws org.eclipse.che.api.core.ApiException
     */
    public String updateFactory(String factoryId, Factory factory) throws ApiException;

}
