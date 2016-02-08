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
package org.eclipse.che.api.workspace.server.stack.image;

import java.util.Arrays;
import java.util.Set;

import com.google.common.base.Objects;
import com.google.gson.annotations.Expose;

import org.eclipse.che.commons.annotation.Nullable;

import static com.google.common.collect.ImmutableSet.of;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * Class for storing {@link org.eclipse.che.api.workspace.server.model.impl.stack.Stack} icon data
 *
 * @author Alexander Andrienko
 */
public class StackIcon {

    private static final Set<String> VALID_MEDIA_TYPES = of("image/jpeg", "image/png", "image/gif", "image/svg+xml");
    private static final int         LIMIT_SIZE        = 1024 * 1024;

    @Expose
    private String stackId;
    @Expose
    private String name;
    @Expose
    private String mediaType;
    @Expose(serialize = false, deserialize = false)
    private byte[] data;

    public StackIcon(StackIcon stackIcon, byte[] data) {
        this(stackIcon.getStackId(), stackIcon.getName(), stackIcon.getMediaType(), data);
    }

    public StackIcon(String stackId, String name, String mediaType, @Nullable byte[] data) {
        requireNonNull(stackId);
        requireNonNull(name);
        requireNonNull(mediaType);

        if (data.length > LIMIT_SIZE) {
            throw new IllegalArgumentException("Maximum upload size exceeded 1 Mb limit");
        }
        this.data = data;

        if (!VALID_MEDIA_TYPES.stream().anyMatch(elem -> elem.equals(mediaType))) {
            String errorMessage = format("Media type '%s' is unsupported. Supported media types: '%s'", mediaType, VALID_MEDIA_TYPES);
            throw new IllegalArgumentException(errorMessage);
        }
        this.mediaType = mediaType;

        this.name = name;
        this.stackId = stackId;
    }

    public String getStackId() {
        return stackId;
    }

    public String getName() {
        return name;
    }

    public String getMediaType() {
        return mediaType;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof StackIcon)) {
            return false;
        }
        StackIcon another = (StackIcon)obj;
        return Objects.equal(stackId, another.stackId)
               && Objects.equal(name, another.name)
               && Objects.equal(mediaType, another.mediaType)
               && Arrays.equals(getData(), another.getData());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(stackId);
        hash = 31 * hash + Objects.hashCode(name);
        hash = 31 * hash + Objects.hashCode(mediaType);
        hash = 31 * hash + Arrays.hashCode(getData());
        return hash;
    }
}
