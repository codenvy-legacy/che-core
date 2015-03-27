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

import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.util.RateExceedDetector;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Publishes application's outputs to the EventService.
 *
 * @author andrew00x
 */
public class ApplicationLogsPublisher extends DelegateApplicationLogger {
    private final AtomicInteger      lineCounter;
    private final EventService       eventService;
    private final long               processId;
    private final String             workspace;
    private final String             project;
    private final RateExceedDetector maxEventRateChecker;

    private boolean outputEnabled = true;

    int maxLogsRate = 60;

    public ApplicationLogsPublisher(ApplicationLogger delegate,
                                    EventService eventService,
                                    long processId,
                                    String workspace,
                                    String project) {
        super(delegate);
        this.eventService = eventService;
        this.processId = processId;
        this.workspace = workspace;
        this.project = project;
        lineCounter = new AtomicInteger(1);
        maxEventRateChecker = new RateExceedDetector(maxLogsRate);
    }

    @Override
    public void writeLine(String line) throws IOException {
        if (outputEnabled) {
            double rate;
            if (maxEventRateChecker.updateAndCheckRate() && (rate = maxEventRateChecker.getRate()) > maxLogsRate) {
                outputEnabled = false;
                final String message = String.format(
                        "[WARNING] Application '%s' has exceeded output rate of %.2f messages / second. Application output has been disabled.",
                        project.startsWith("/") ? project.substring(1) : project, rate);
                eventService.publish(RunnerEvent.messageLoggedEvent(processId, workspace, project,
                                                                    new RunnerEvent.LoggedMessage(message, lineCounter.getAndIncrement())));
                return;
            }
            if (line != null) {
                eventService.publish(RunnerEvent.messageLoggedEvent(processId, workspace, project,
                                                                    new RunnerEvent.LoggedMessage(line, lineCounter.getAndIncrement())));
            }
        }
        super.writeLine(line);
    }
}
