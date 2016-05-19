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
package org.eclipse.che.ide.jseditor.client.partition;

import java.util.List;

import org.eclipse.che.ide.api.text.TypedRegion;
import org.eclipse.che.ide.jseditor.client.events.DocumentChangeHandler;
import org.eclipse.che.ide.jseditor.client.document.UseDocumentHandle;

/**
 * Interface for document partitioner.<br>
 * Partitioners parse a document and splits it in partitions, which are contiguous zones of text
 * of a specific type (for example comment, literal string, code etc.).
 */
public interface DocumentPartitioner extends DocumentChangeHandler, UseDocumentHandle {

    /** The identifier of the default partition content type. */
    public static final String DEFAULT_CONTENT_TYPE = "__dftl_partition_content_type";

    void initialize();

    void release();

    /**
     * Returns the set of all legal content types of this partitioner. I.e. any result delivered by this partitioner may not contain a
     * content type which would not be included in this method's result.
     *
     * @return the set of legal content types
     */
    List<String> getLegalContentTypes();

    /**
     * Returns the content type of the partition containing the given offset in the connected document. There must be a document connected
     * to this partitioner.
     *
     * @param offset the offset in the connected document
     * @return the content type of the offset's partition
     */
    String getContentType(int offset);

    /**
     * Returns the partitioning of the given range of the connected document. There must be a document connected to this partitioner.
     *
     * @param offset the offset of the range of interest
     * @param length the length of the range of interest
     * @return the partitioning of the range
     */
    List<TypedRegion> computePartitioning(int offset, int length);

    /**
     * Returns the partition containing the given offset of the connected document. There must be a document connected to this partitioner.
     *
     * @param offset the offset for which to determine the partition
     * @return the partition containing the offset
     */
    TypedRegion getPartition(int offset);
}
