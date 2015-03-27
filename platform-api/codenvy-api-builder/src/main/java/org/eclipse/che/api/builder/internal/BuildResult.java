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
package org.eclipse.che.api.builder.internal;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents result of build or analysis dependencies process.
 *
 * @author andrew00x
 */
public class BuildResult {
    private final boolean success;

    private List<java.io.File> artifacts;
    private java.io.File       report;

    public BuildResult(boolean success, List<java.io.File> artifacts, java.io.File report) {
        this.success = success;
        if (artifacts != null) {
            this.artifacts = new ArrayList<>(artifacts);
        } else {
            this.artifacts = null;
        }
        this.report = report;
    }

    public BuildResult(boolean success, List<java.io.File> artifacts) {
        this(success, artifacts, null);
    }

    public BuildResult(boolean success, java.io.File report) {
        this(success, null, report);
    }

    public BuildResult(boolean success) {
        this(success, null, null);
    }

    /**
     * Reports whether build process successful or failed.
     *
     * @return {@code true} if build successful and {@code false} otherwise
     */
    public boolean isSuccessful() {
        return success;
    }

    /** Gets build artifacts. */
    public List<java.io.File> getResults() {
        if (artifacts == null) {
            artifacts = new ArrayList<>();
        }
        return artifacts;
    }

    /**
     * Reports whether build report is available or not. In case if this method returns {@code false} method {@link #getBuildReport()}
     * always returns {@code null}.
     *
     * @return {@code true} if build report is available and {@code false} otherwise
     */
    public boolean hasBuildReport() {
        return null != report;
    }

    /**
     * Provides report about build process. If {@code Builder} does not support reports or report for particular build is not available
     * this method always returns {@code null}.
     *
     * @return report about build or {@code null}
     */
    public java.io.File getBuildReport() {
        return report;
    }

    public void setBuildReport(java.io.File report) {
        this.report = report;
    }
}
