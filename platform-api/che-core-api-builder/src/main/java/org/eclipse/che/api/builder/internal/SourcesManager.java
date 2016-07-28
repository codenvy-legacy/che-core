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
package org.eclipse.che.api.builder.internal;

import java.io.File;
import java.io.IOException;

import com.google.inject.ImplementedBy;

/**
 * Manages build sources.
 *
 * @author andrew00x
 * @author Eugene Voevodin
 */
@ImplementedBy(SourcesManagerImpl.class)
public interface SourcesManager {
    /**
     * Get build sources. Sources are copied to the directory <code>workDir</code>.
     *
     * @param workspace
     *         workspace
     * @param project
     *         project
     * @param sourcesUrl
     *         sources url
     * @param sourcesDir
     *         The sources directory of the builder
     * @param workDir
     *         directory where sources will be copied
     */
    void getSources(BuildLogger logger, String workspace, String project, String sourcesUrl, File sourcesDir, File workDir) throws IOException;

    boolean addListener(SourceManagerListener listener);

    boolean removeListener(SourceManagerListener listener);
}
