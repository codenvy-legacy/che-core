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

/**
 * Simple been to manage Running async REST Services
 *
 * @author <a href="mailto:evidolob@exoplatform.com">Evgen Vidolob</a>
 * @version $Id: Sep 19, 2011 evgen $
 */
public class Job {
    public enum JobStatus {
        STARTED, FINISHED, ERROR
    }

    private String id;

    private JobStatus status;

    private String startMessage;

    private String finishMessage;

    private Throwable error;

    public Job(String id, JobStatus status) {
        this.id = id;
        this.status = status;
    }

    /** @return the startMessage */
    public String getStartMessage() {
        return startMessage;
    }

    /**
     * @param startMessage
     *         the startMessage to set
     */
    public void setStartMessage(String startMessage) {
        this.startMessage = startMessage;
    }

    /** @return the finishMessage */
    public String getFinishMessage() {
        return finishMessage;
    }

    /**
     * @param finishMessage
     *         the finishMessage to set
     */
    public void setFinishMessage(String finishMessage) {
        this.finishMessage = finishMessage;
    }

    /** @return the error */
    public Throwable getError() {
        return error;
    }

    /**
     * @param error
     *         the error to set
     */
    public void setError(Throwable error) {
        this.error = error;
    }

    /** @return the id */
    public String getId() {
        return id;
    }

    /** @return the status */
    public JobStatus getStatus() {
        return status;
    }
}