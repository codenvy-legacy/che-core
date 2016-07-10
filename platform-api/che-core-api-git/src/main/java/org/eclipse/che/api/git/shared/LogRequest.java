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
package org.eclipse.che.api.git.shared;

import org.eclipse.che.dto.shared.DTO;
import java.util.List;

/**
 * Request to get commit logs.
 *
 * @author andrew00x
 */
@DTO
public interface LogRequest extends GitRequest {
    /** @return revision range since */
    String getRevisionRangeSince();
	/** @set revision range since */
    void setRevisionRangeSince(String revisionRangeSince);
    LogRequest withRevisionRangeSince(String v);


    /** @return revision range until */
    String getRevisionRangeUntil();
	/** @set revision range until */
    void setRevisionRangeUntil(String revisionRangeUntil);
    LogRequest withRevisionRangeUntil(String v);


    /** @return the integer value of the number of commits that will be skipped when calling log API */
    int getSkip();
    /**  set the integer value of the number of commits that will be skipped when calling log API */
    void setSkip(int skip);
    LogRequest withSkip(int v);


    /** @return the integer value of the number of commits that will be returned when calling log API */
    int getMaxCount();
    /**  set the integer value of the number of commits that will be returned when calling log API */
    void setMaxCount(int maxCount);
    LogRequest withMaxCount(int v);


    /** @return get the file/folder path used when calling the log API */
    String getFilePath();
    /** set the file/folder path used when calling the log API */
    void setFilePath(String filePath);
    LogRequest withFilePath(String filePath);
}