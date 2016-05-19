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
package org.eclipse.che.api.project.shared.dto;

import org.eclipse.che.api.core.rest.shared.dto.Hyperlinks;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.dto.shared.DTO;

import java.util.List;
import java.util.Map;

/**
 * @author andrew00x
 */
@DTO
public interface ItemReference extends Hyperlinks {
    /** Get name of item. */
    String getName();

    /** Set name of item. */
    void setName(String name);

    ItemReference withName(String name);

    /** Get type of item, e.g. "file", "folder" or "project". */
    String getType();

    /** Set type of item, e.g. "file" or "folder" or "project". */
    void setType(String type);

    ItemReference withType(String type);

    /** Get mediatype. */
    String getMediaType();

    /** Get mediatype. */
    void setMediaType(String mediaType);

    ItemReference withMediaType(String mediaType);

    /** Get path of item. */
    String getPath();

    /** Set path of item. */
    void setPath(String path);

    ItemReference withPath(String path);

    ItemReference withLinks(List<Link> links);

    /**
     * Attributes
     */
    Map<String, String> getAttributes();

    void setAttributes(Map<String, String> attributes);

    ItemReference withAttributes(Map<String, String> attributes);


    /** creating date. */
    long getCreated();

    void setCreated(long created);

    ItemReference withCreated(long created);


    /** last modified date. */
    long getModified();

    void setModified(long modified);

    ItemReference withModified(long modified);


    /** content length for file */
    long getContentLength();

    void setContentLength(long length);

    ItemReference withContentLength(long length);

}
