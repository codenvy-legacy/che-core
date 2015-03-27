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

/**
 * Build listener abstraction. Implementation of this interface may be registered in {@code Builder} with method {@link
 * Builder#addBuildListener(BuildListener)}.
 *
 * @author andrew00x
 * @see Builder#addBuildListener(BuildListener)
 * @see Builder#removeBuildListener(BuildListener)
 */
public interface BuildListener {
    /** Builder invokes this method when build process starts. */
    void begin(BuildTask task);

    /** Builder invokes this method when build process ends. */
    void end(BuildTask task);
}
