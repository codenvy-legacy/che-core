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
package org.eclipse.che.api.vfs.server.impl.file.inotify;

import org.eclipse.che.api.core.util.ProcessUtil;
import org.eclipse.che.api.vfs.server.impl.file.FileWatcherNotificationListener;
import org.eclipse.che.api.vfs.server.impl.file.FileWatcherTestTree;
import org.eclipse.che.commons.lang.IoUtil;
import org.eclipse.che.commons.lang.NameGenerator;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class InotifyToolsFileWatcherTest {
    private File                    testDirectory;
    private InotifyToolsFileWatcher fileWatcher;
    private FileWatcherTestTree     fileWatcherTestTree;

    @Before
    public void setUp() throws Exception {
        File targetDir = new File(Thread.currentThread().getContextClassLoader().getResource(".").getPath()).getParentFile();
        testDirectory = new File(targetDir, NameGenerator.generate("watcher-", 4));
        assertTrue(testDirectory.mkdir());
        fileWatcherTestTree = new FileWatcherTestTree(testDirectory);
    }

    @After
    public void tearDown() throws Exception {
        if (fileWatcher != null) {
            fileWatcher.shutdown();
        }
        IoUtil.deleteRecursive(testDirectory);
    }

    @Test
    public void watchesCreateNotRecursively() throws Exception {
        File ignored = new File(testDirectory, "ignored");
        assertTrue(ignored.mkdir());

        FileWatcherNotificationListener notificationListener = aNotificationListener();
        fileWatcher = new InotifyToolsFileWatcher(testDirectory, newArrayList(), notificationListener, false);
        fileWatcher.startup();

        Thread.sleep(300);

        fileWatcherTestTree.createDirectory("ignored");
        fileWatcherTestTree.createFile("ignored");
        Set<String> created = newHashSet(fileWatcherTestTree.createFile(""), fileWatcherTestTree.createFile(""));

        Thread.sleep(300);

        verify(notificationListener, never()).errorOccurred(eq(testDirectory), any(Throwable.class));
        verify(notificationListener, never()).pathDeleted(eq(testDirectory), anyString(), anyBoolean());

        ArgumentCaptor<String> createdEvents = ArgumentCaptor.forClass(String.class);
        verify(notificationListener, times(created.size())).pathCreated(eq(testDirectory), createdEvents.capture(), anyBoolean());
        assertEquals(created, newHashSet(createdEvents.getAllValues()));
    }

    @Test
    public void watchesUpdateNotRecursively() throws Exception {
        File ignored = new File(testDirectory, "ignored");
        assertTrue(ignored.mkdir());
        String notifiedFile = fileWatcherTestTree.createFile("");
        String ignoredFile = fileWatcherTestTree.createFile("ignored");

        FileWatcherNotificationListener notificationListener = aNotificationListener();
        fileWatcher = new InotifyToolsFileWatcher(testDirectory, newArrayList(), notificationListener, false);
        fileWatcher.startup();

        Thread.sleep(300);

        fileWatcherTestTree.updateFile(notifiedFile);
        fileWatcherTestTree.updateFile(ignoredFile);
        Set<String> updated = newHashSet(notifiedFile);

        Thread.sleep(300);

        verify(notificationListener, never()).errorOccurred(eq(testDirectory), any(Throwable.class));
        verify(notificationListener, never()).pathCreated(eq(testDirectory), anyString(), anyBoolean());
        verify(notificationListener, never()).pathDeleted(eq(testDirectory), anyString(), anyBoolean());

        ArgumentCaptor<String> updatedEvents = ArgumentCaptor.forClass(String.class);
        verify(notificationListener, times(1)).pathUpdated(eq(testDirectory), updatedEvents.capture(), anyBoolean());
        assertEquals(updated, newHashSet(updatedEvents.getAllValues()));
    }

    @Test
    public void watchesDeleteNotRecursively() throws Exception {
        File ignored = new File(testDirectory, "ignored");
        assertTrue(ignored.mkdir());
        String notifiedDir = fileWatcherTestTree.createDirectory("");
        String notifiedFile = fileWatcherTestTree.createFile("");
        String ignoredDir = fileWatcherTestTree.createDirectory("ignored");
        String ignoredFile = fileWatcherTestTree.createFile("ignored");

        FileWatcherNotificationListener notificationListener = aNotificationListener();
        fileWatcher = new InotifyToolsFileWatcher(testDirectory, newArrayList(), notificationListener, false);
        fileWatcher.startup();

        Thread.sleep(300);

        fileWatcherTestTree.delete(ignoredDir);
        fileWatcherTestTree.delete(ignoredFile);
        fileWatcherTestTree.delete(notifiedDir);
        fileWatcherTestTree.delete(notifiedFile);

        Set<String> deleted = newHashSet(notifiedDir, notifiedFile);

        Thread.sleep(300);

        verify(notificationListener, never()).errorOccurred(eq(testDirectory), any(Throwable.class));
        verify(notificationListener, never()).pathCreated(eq(testDirectory), anyString(), anyBoolean());
        verify(notificationListener, never()).pathUpdated(eq(testDirectory), anyString(), anyBoolean());

        ArgumentCaptor<String> deletedEvents = ArgumentCaptor.forClass(String.class);
        verify(notificationListener, times(2)).pathDeleted(eq(testDirectory), deletedEvents.capture(), anyBoolean());
        assertEquals(deleted, newHashSet(deletedEvents.getAllValues()));
    }

    @Test
    public void watchesCreateRecursively() throws Exception {
        File watched = new File(testDirectory, "watched");
        assertTrue(watched.mkdir());

        FileWatcherNotificationListener notificationListener = aNotificationListener();
        fileWatcher = new InotifyToolsFileWatcher(testDirectory, newArrayList(), notificationListener, true);
        fileWatcher.startup();

        Thread.sleep(300);

        Set<String> created = newHashSet(fileWatcherTestTree.createDirectory(""),
                                         fileWatcherTestTree.createFile(""),
                                         fileWatcherTestTree.createDirectory("watched"),
                                         fileWatcherTestTree.createFile("watched"));

        Thread.sleep(300);

        verify(notificationListener, never()).errorOccurred(eq(testDirectory), any(Throwable.class));
        verify(notificationListener, never()).pathDeleted(eq(testDirectory), anyString(), anyBoolean());

        ArgumentCaptor<String> createdEvents = ArgumentCaptor.forClass(String.class);
        verify(notificationListener, times(4)).pathCreated(eq(testDirectory), createdEvents.capture(), anyBoolean());
        assertEquals(created, newHashSet(createdEvents.getAllValues()));
    }

    @Ignore
    @Test
    public void watchesCreateRecursivelyAndStartsWatchNewlyCreatedDirectories() throws Exception {
        FileWatcherNotificationListener notificationListener = aNotificationListener();
        fileWatcher = new InotifyToolsFileWatcher(testDirectory, newArrayList(), notificationListener, true);
        fileWatcher.startup();

        Thread.sleep(300);

        List<String> created = fileWatcherTestTree.createTree("", 4, 4);

        Thread.sleep(1000);

        verify(notificationListener, never()).errorOccurred(eq(testDirectory), any(Throwable.class));
        verify(notificationListener, never()).pathDeleted(eq(testDirectory), anyString(), anyBoolean());

        ArgumentCaptor<String> createdEvents = ArgumentCaptor.forClass(String.class);
        verify(notificationListener, times(created.size())).pathCreated(eq(testDirectory), createdEvents.capture(), anyBoolean());
        assertEquals(newHashSet(created), newHashSet(createdEvents.getAllValues()));
    }

    @Test
    public void watchesUpdateRecursively() throws Exception {
        File watched = new File(testDirectory, "watched");
        assertTrue(watched.mkdir());
        String notifiedFile1 = fileWatcherTestTree.createFile("");
        String notifiedFile2 = fileWatcherTestTree.createFile("watched");
        Set<String> updated = newHashSet(notifiedFile1, notifiedFile2);

        FileWatcherNotificationListener notificationListener = aNotificationListener();
        fileWatcher = new InotifyToolsFileWatcher(testDirectory, newArrayList(), notificationListener, true);
        fileWatcher.startup();

        Thread.sleep(300);

        fileWatcherTestTree.updateFile(notifiedFile1);
        fileWatcherTestTree.updateFile(notifiedFile2);

        Thread.sleep(300);

        verify(notificationListener, never()).errorOccurred(eq(testDirectory), any(Throwable.class));
        verify(notificationListener, never()).pathCreated(eq(testDirectory), anyString(), anyBoolean());
        verify(notificationListener, never()).pathDeleted(eq(testDirectory), anyString(), anyBoolean());

        ArgumentCaptor<String> updatedEvents = ArgumentCaptor.forClass(String.class);
        verify(notificationListener, times(2)).pathUpdated(eq(testDirectory), updatedEvents.capture(), anyBoolean());
        assertEquals(updated, newHashSet(updatedEvents.getAllValues()));
    }

    @Test
    public void watchesDeleteRecursively() throws Exception {
        File watched = new File(testDirectory, "watched");
        assertTrue(watched.mkdir());
        String deletedDir1 = fileWatcherTestTree.createDirectory("watched");
        String deletedFile1 = fileWatcherTestTree.createFile("watched");
        Set<String> deleted = newHashSet("watched", deletedDir1, deletedFile1);

        FileWatcherNotificationListener notificationListener = aNotificationListener();
        fileWatcher = new InotifyToolsFileWatcher(testDirectory, newArrayList(), notificationListener, true);
        fileWatcher.startup();

        Thread.sleep(300);

        fileWatcherTestTree.delete("watched");

        Thread.sleep(300);

        verify(notificationListener, never()).errorOccurred(eq(testDirectory), any(Throwable.class));
        verify(notificationListener, never()).pathCreated(eq(testDirectory), anyString(), anyBoolean());
        verify(notificationListener, never()).pathUpdated(eq(testDirectory), anyString(), anyBoolean());

        ArgumentCaptor<String> deletedEvents = ArgumentCaptor.forClass(String.class);
        verify(notificationListener, times(3)).pathDeleted(eq(testDirectory), deletedEvents.capture(), anyBoolean());
        assertEquals(deleted, newHashSet(deletedEvents.getAllValues()));
    }

    @Test
    public void triesRestart() throws Exception {
        FileWatcherNotificationListener notificationListener = aNotificationListener();
        fileWatcher = new InotifyToolsFileWatcher(testDirectory, newArrayList(), notificationListener, false);
        fileWatcher.startup();

        int watcherProcessTerminatedCount = 3;
        for (int i = 0; i < watcherProcessTerminatedCount; i++) {
            Thread.sleep(100);
            ProcessUtil.kill(fileWatcher.getWatcherProcess());
        }

        verify(notificationListener, times(3)).started(eq(testDirectory));
    }

    @Test
    public void triesRestartNotMoreThan10Times() throws Exception {
        FileWatcherNotificationListener notificationListener = aNotificationListener();
        fileWatcher = new InotifyToolsFileWatcher(testDirectory, newArrayList(), notificationListener, false);
        fileWatcher.startup();

        int watcherProcessTerminatedCount = 13;
        for (int i = 0; i < watcherProcessTerminatedCount; i++) {
            Thread.sleep(100);
            ProcessUtil.kill(fileWatcher.getWatcherProcess());
        }

        verify(notificationListener, times(10)).started(eq(testDirectory));
    }

    @Test
    public void notifiesNotificationListenerWhenErrorOccurs() throws Exception {
        RuntimeException error = new RuntimeException();
        FileWatcherNotificationListener notificationListener = aNotificationListener();
        doThrow(error).when(notificationListener).pathCreated(eq(testDirectory), anyString(), anyBoolean());
        fileWatcher = new InotifyToolsFileWatcher(testDirectory, newArrayList(), notificationListener, false);
        fileWatcher.startup();

        Thread.sleep(100);
        fileWatcherTestTree.createFile("");
        Thread.sleep(100);

        verify(notificationListener).errorOccurred(eq(testDirectory), eq(error));
    }

    private FileWatcherNotificationListener aNotificationListener() {
        return mock(FileWatcherNotificationListener.class);
    }
}