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
package org.eclipse.che.api.git.gwt.client.handler;

import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.git.gwt.client.GitLocalizationConstant;
import org.eclipse.che.ide.job.Job;
import org.eclipse.che.ide.job.JobChangeEvent;
import org.eclipse.che.ide.job.RequestStatusHandlerBase;

import javax.annotation.Nonnull;

/**
 * @author <a href="mailto:evidolob@exoplatform.com">Evgen Vidolob</a>
 * @version $Id: Oct 31, 2011 evgen $
 */
public class AddRequestHandler extends RequestStatusHandlerBase {
    private GitLocalizationConstant constant;

    /**
     * Create handler.
     *
     * @param projectName
     * @param eventBus
     * @param constant
     */
    public AddRequestHandler(@Nonnull String projectName, @Nonnull EventBus eventBus, @Nonnull GitLocalizationConstant constant) {
        super(projectName, eventBus);
        this.constant = constant;
    }

    /** {@inheritDoc} */
    @Override
    public void requestInProgress(String id) {
        Job job = new Job(id, Job.JobStatus.STARTED);
        job.setStartMessage(constant.addStarted(projectName));
        eventBus.fireEvent(new JobChangeEvent(job));
    }

    /** {@inheritDoc} */
    @Override
    public void requestFinished(String id) {
        Job job = new Job(id, Job.JobStatus.FINISHED);
        job.setFinishMessage(constant.addFinished(projectName));
        eventBus.fireEvent(new JobChangeEvent(job));
    }
}