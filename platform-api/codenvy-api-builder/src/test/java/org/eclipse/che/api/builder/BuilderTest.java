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
package org.eclipse.che.api.builder;

import org.eclipse.che.api.builder.internal.BuildListener;
import org.eclipse.che.api.builder.internal.BuildLogger;
import org.eclipse.che.api.builder.internal.BuildResult;
import org.eclipse.che.api.builder.internal.BuildTask;
import org.eclipse.che.api.builder.internal.Builder;
import org.eclipse.che.api.builder.internal.BuilderConfiguration;
import org.eclipse.che.api.builder.internal.DelegateBuildLogger;
import org.eclipse.che.api.builder.internal.SourceManagerListener;
import org.eclipse.che.api.builder.internal.SourcesManager;
import org.eclipse.che.api.builder.dto.BuildRequest;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.util.CommandLine;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.commons.lang.IoUtil;
import org.eclipse.che.dto.server.DtoFactory;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

/** @author andrew00x */
public class BuilderTest {

    // Simple test for main Builder components. Don't run any real build processes.

    public static class MyBuilder extends Builder {
        MyDelegateBuildLogger logger;

        public MyBuilder(File root, int numberOfWorkers, int queueSize, int cleanBuildResultDelay) {
            super(root, numberOfWorkers, queueSize, cleanBuildResultDelay, new EventService());
        }

        @Override
        public String getName() {
            return "my";
        }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        protected BuildResult getTaskResult(FutureBuildTask task, boolean successful) {
            return new BuildResult(successful);
        }

        @Override
        protected CommandLine createCommandLine(BuilderConfiguration config) {
            return new CommandLine("echo", "test"); // display line of text
        }

        @Override
        protected BuildLogger createBuildLogger(BuilderConfiguration buildConfiguration, java.io.File logFile) throws BuilderException {
            return logger = new MyDelegateBuildLogger(super.createBuildLogger(buildConfiguration, logFile));
        }

        @Override
        public SourcesManager getSourcesManager() {
            return new SourcesManager() {
                @Override
                public void getSources(BuildLogger logger, String workspace, String project, String sourcesUrl, File workDir) throws IOException {
                    // Don't need for current set of tests.
                }

                @Override
                public java.io.File getDirectory() {
                    return getSourcesDirectory();
                }

                @Override
                public boolean addListener(SourceManagerListener listener) {
                    return false;
                }

                @Override
                public boolean removeListener(SourceManagerListener listener) {
                    return false;
                }
            };
        }
    }

    public static class MyDelegateBuildLogger extends DelegateBuildLogger {
        private StringBuilder buff = new StringBuilder();

        public MyDelegateBuildLogger(BuildLogger delegate) {
            super(delegate);
        }

        @Override
        public void writeLine(String line) throws IOException {
            if (line != null) {
                if (buff.length() > 0) {
                    buff.append('\n');
                }
                buff.append(line);
            }
            super.writeLine(line);
        }

        public String getLogsAsString() {
            return buff.toString();
        }
    }

    private java.io.File repo;
    private MyBuilder    builder;

    @BeforeTest
    public void setUp() throws Exception {
        repo = createRepository();
        builder = new MyBuilder(repo, Runtime.getRuntime().availableProcessors(), 100, 3600);
        builder.start();
    }

    @AfterTest
    public void tearDown() {
        builder.stop();
        Assert.assertTrue(IoUtil.deleteRecursive(repo), "Unable remove test directory");
    }

    static java.io.File createRepository() throws Exception {
        java.io.File root = new java.io.File(System.getProperty("workDir"), "repo");
        if (!(root.exists() || root.mkdirs())) {
            Assert.fail("Unable create test directory");
        }
        return root;
    }

    @Test
    public void testRunTask() throws Exception {
        final BuildRequest buildRequest = DtoFactory.getInstance().createDto(BuildRequest.class);
        buildRequest.setBuilder("my");
        buildRequest.setSourcesUrl("http://localhost/a" /* ok for test, nothing download*/);
        buildRequest.setProjectDescriptor(DtoFactory.getInstance().createDto(ProjectDescriptor.class)
                                                    .withName("my_project")
                                                    .withType("my_type"));
        final BuildTask task = builder.perform(buildRequest);
        waitForTask(task);
        Assert.assertEquals(builder.logger.getLogsAsString(), "test");
    }

    @Test
    public void testBuildListener() throws Exception {
        final boolean[] beginFlag = new boolean[]{false};
        final boolean[] endFlag = new boolean[]{false};
        final BuildListener listener = new BuildListener() {
            @Override
            public void begin(BuildTask task) {
                beginFlag[0] = true;
            }

            @Override
            public void end(BuildTask task) {
                endFlag[0] = true;
            }
        };
        Assert.assertTrue(builder.addBuildListener(listener));
        final BuildRequest buildRequest = DtoFactory.getInstance().createDto(BuildRequest.class);
        buildRequest.setBuilder("my");
        buildRequest.setSourcesUrl("http://localhost/a" /* ok for test, nothing download*/);
        buildRequest.setProjectDescriptor(DtoFactory.getInstance().createDto(ProjectDescriptor.class)
                                                    .withName("my_project")
                                                    .withType("my_type"));
        final BuildTask task = builder.perform(buildRequest);
        waitForTask(task);
        Assert.assertTrue(beginFlag[0]);
        Assert.assertTrue(endFlag[0]);
        Assert.assertTrue(builder.removeBuildListener(listener));
    }

    private void waitForTask(BuildTask task) throws Exception {
        final long end = System.currentTimeMillis() + 5000;
        synchronized (this) {
            while (!task.isDone()) {
                wait(100);
                if (System.currentTimeMillis() > end) {
                    Assert.fail("timeout");
                }
            }
        }
    }
}
