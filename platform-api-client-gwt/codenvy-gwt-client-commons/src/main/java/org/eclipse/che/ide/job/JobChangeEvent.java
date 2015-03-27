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
package org.eclipse.che.ide.job;

import com.google.gwt.event.shared.GwtEvent;

/**
 * @author <a href="mailto:evidolob@exoplatform.com">Evgen Vidolob</a>
 * @version $Id: Sep 19, 2011 evgen $
 */
public class JobChangeEvent extends GwtEvent<JobChangeHandler> {
    public static final GwtEvent.Type<JobChangeHandler> TYPE = new Type<JobChangeHandler>();

    private Job job;

    /**
     * Create event.
     *
     * @param job
     */
    public JobChangeEvent(Job job) {
        super();
        this.job = job;
    }

    /** {@inheritDoc} */
    @Override
    public com.google.gwt.event.shared.GwtEvent.Type<JobChangeHandler> getAssociatedType() {
        return TYPE;
    }

    /** {@inheritDoc} */
    @Override
    protected void dispatch(JobChangeHandler handler) {
        handler.onJobChangeHandler(this);
    }

    /** @return the job */
    public Job getJob() {
        return job;
    }
}