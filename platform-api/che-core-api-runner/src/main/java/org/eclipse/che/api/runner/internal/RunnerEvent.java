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
package org.eclipse.che.api.runner.internal;

import org.eclipse.che.api.core.notification.EventOrigin;

/**
 * @author andrew00x
 */
@EventOrigin("runner")
public class RunnerEvent {
    public enum EventType {
        /** Application launching process started. */
        PREPARATION_STARTED("preparation started"),
        /** Application started. */
        STARTED("started"),
        /** Application stopped. */
        STOPPED("stopped"),
        /** Running process is terminated due to exceeded max allowed queue time. */
        RUN_TASK_QUEUE_TIME_EXCEEDED("run_task_queue_time_exceeded"),
        /** Running process is added in queue. */
        RUN_TASK_ADDED_IN_QUEUE("run_task_added_in_queue"),
        /** Start of application canceled. */
        CANCELED("canceled"),
        /** Error occurs while starting or stopped an application. */
        ERROR("error"),
        /**
         * Gets new logged message from an application.
         *
         * @see ApplicationLogger
         */
        MESSAGE_LOGGED("message_logged");

        private final String value;

        private EventType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    /*
    Seems we can't guaranty correct order of messages on the client (browser) side, that means we need to wrap each line with simple object
    that keeps line's number.
     */
    public static class LoggedMessage {
        private String message;
        private int    lineNum;

        public LoggedMessage(String message, int lineNum) {
            this.message = message;
            this.lineNum = lineNum;
        }

        public LoggedMessage() {
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public int getLineNum() {
            return lineNum;
        }

        public void setLineNum(int lineNum) {
            this.lineNum = lineNum;
        }

        @Override
        public String toString() {
            return "LoggedMessage{" +
                   "message='" + message + '\'' +
                   ", lineNum=" + lineNum +
                   '}';
        }
    }

    public static RunnerEvent preparationStartedEvent(long processId, String workspace, String project) {
        return new RunnerEvent(EventType.PREPARATION_STARTED, processId, workspace, project);
    }

    public static RunnerEvent startedEvent(long processId, String workspace, String project) {
        return new RunnerEvent(EventType.STARTED, processId, workspace, project);
    }

    public static RunnerEvent canceledEvent(long processId, String workspace, String project) {
        return new RunnerEvent(EventType.CANCELED, processId, workspace, project);
    }

    public static RunnerEvent stoppedEvent(long processId, String workspace, String project) {
        return new RunnerEvent(EventType.STOPPED, processId, workspace, project);
    }

    public static RunnerEvent queueTerminatedEvent(long processId, String workspace, String project) {
        return new RunnerEvent(EventType.RUN_TASK_QUEUE_TIME_EXCEEDED, processId, workspace, project);
    }

    public static RunnerEvent queueStartedEvent(long processId, String workspace, String project) {
        return new RunnerEvent(EventType.RUN_TASK_ADDED_IN_QUEUE, processId, workspace, project);
    }

    public static RunnerEvent errorEvent(long processId, String workspace, String project, String message) {
        return new RunnerEvent(EventType.ERROR, processId, workspace, project, message);
    }

    public static RunnerEvent messageLoggedEvent(long processId, String workspace, String project, LoggedMessage message) {
        return new RunnerEvent(EventType.MESSAGE_LOGGED, processId, workspace, project, message);
    }

    /** Event type. */
    private EventType     type;
    /** Id of application process that produces the event. */
    private long          processId;
    /** Id of workspace that produces the event. */
    private String        workspace;
    /** Name of project that produces the event. */
    private String        project;
    /** Error message. */
    private String        error;
    /** Message associated with this event. Makes sense only for {@link EventType#MESSAGE_LOGGED} or {@link EventType#ERROR} events. */
    private LoggedMessage message;

    RunnerEvent(EventType type, long processId, String workspace, String project, LoggedMessage message) {
        this.type = type;
        this.processId = processId;
        this.workspace = workspace;
        this.project = project;
        this.message = message;
    }

    RunnerEvent(EventType type, long processId, String workspace, String project, String error) {
        this.type = type;
        this.processId = processId;
        this.workspace = workspace;
        this.project = project;
        this.error = error;
    }

    RunnerEvent(EventType type, long processId, String workspace, String project) {
        this.type = type;
        this.processId = processId;
        this.workspace = workspace;
        this.project = project;
    }

    public RunnerEvent() {
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public long getProcessId() {
        return processId;
    }

    public void setProcessId(long processId) {
        this.processId = processId;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public LoggedMessage getMessage() {
        return message;
    }

    public void setMessage(LoggedMessage message) {
        this.message = message;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "RunnerEvent{" +
               "type=" + type +
               ", processId=" + processId +
               ", workspace='" + workspace + '\'' +
               ", project='" + project + '\'' +
               ", message='" + message + '\'' +
               ", error='" + error + '\'' +
               '}';
    }
}
