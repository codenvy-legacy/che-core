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

import org.eclipse.che.api.core.notification.EventOrigin;

/**
 * @author andrew00x
 */
@EventOrigin("builder")
public class BuilderEvent {
    public enum EventType {
        /** Build time is started. */
        BUILD_TIME_STARTED("build_time_begin"),
        /** Build starts. */
        BEGIN("begin"),
        /** Build ends. */
        DONE("done"),
        /** Building process is terminated due to exceeded max allowed queue time. */
        BUILD_TASK_QUEUE_TIME_EXCEEDED("build_task_queue_time_exceeded"),
        /** Building process is added in queue. */
        BUILD_TASK_ADDED_IN_QUEUE("build_task_added_in_queue"),
        /** Build canceled. */
        CANCELED("canceled"),
        /**
         * Gets new logged message from the builder.
         *
         * @see BuildLogger
         */
        MESSAGE_LOGGED("messageLogged");

        private final String value;

        private EventType(String value) {
            this.value = value;
        }

        public String value() {
            return value;
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

    public static BuilderEvent beginEvent(long taskId, String workspace, String project) {
        return new BuilderEvent(EventType.BEGIN, taskId, workspace, project);
    }

    public static BuilderEvent doneEvent(long taskId, String workspace, String project) {
        return new BuilderEvent(EventType.DONE, taskId, workspace, project);
    }

    public static BuilderEvent canceledEvent(long taskId, String workspace, String project) {
        return new BuilderEvent(EventType.CANCELED, taskId, workspace, project);
    }

    public static BuilderEvent doneEvent(long taskId, String workspace, String project, boolean reused) {
        return new BuilderEvent(EventType.DONE, taskId, workspace, project, reused);
    }

    public static BuilderEvent terminatedEvent(long taskId, String workspace, String project) {
        return new BuilderEvent(EventType.BUILD_TASK_QUEUE_TIME_EXCEEDED, taskId, workspace, project);
    }

    public static BuilderEvent queueStartedEvent(long taskId, String workspace, String project) {
        return new BuilderEvent(EventType.BUILD_TASK_ADDED_IN_QUEUE, taskId, workspace, project);
    }

    public static BuilderEvent messageLoggedEvent(long taskId, String workspace, String project, LoggedMessage message) {
        return new BuilderEvent(EventType.MESSAGE_LOGGED, taskId, workspace, project, message);
    }

    public static BuilderEvent buildTimeStartedEvent(long taskId, String workspace, String project, long startTime) {
        return new BuilderEvent(EventType.BUILD_TIME_STARTED, taskId, workspace, project, new LoggedMessage(Long.toString(startTime), 0));
    }

    /** Event type. */
    private EventType     type;
    /** Id of build task that produces the event. */
    private long          taskId;
    /** Id of workspace that produces the event. */
    private String        workspace;
    /** Name of project that produces the event. */
    private String        project;
    /** Message associated with this event. Makes sense only for {@link EventType#MESSAGE_LOGGED} events. */
    private LoggedMessage message;
    /** Indicates if build result was reused. */
    private boolean       reused;

    BuilderEvent(EventType type, long taskId, String workspace, String project, LoggedMessage message) {
        this.type = type;
        this.taskId = taskId;
        this.workspace = workspace;
        this.project = project;
        this.message = message;
    }

    BuilderEvent(EventType type, long taskId, String workspace, String project) {
        this.type = type;
        this.taskId = taskId;
        this.workspace = workspace;
        this.project = project;
    }

    BuilderEvent(EventType type, long taskId, String workspace, String project, boolean reused) {
        this.type = type;
        this.taskId = taskId;
        this.workspace = workspace;
        this.project = project;
        this.reused = reused;
    }

    public BuilderEvent() {
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
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

    public boolean isReused() {
        return reused;
    }

    public void setReused(boolean reused) {
        this.reused = reused;
    }

    @Override
    public String toString() {
        return "BuilderEvent{" +
               "type=" + type +
               ", taskId=" + taskId +
               ", workspace='" + workspace + '\'' +
               ", project='" + project + '\'' +
               ", message='" + message + '\'' +
               '}';
    }
}
