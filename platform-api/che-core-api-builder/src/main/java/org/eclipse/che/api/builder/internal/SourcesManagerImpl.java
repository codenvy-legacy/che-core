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

import org.eclipse.che.api.builder.dto.BaseBuilderRequest;
import org.eclipse.che.api.core.util.ValueHolder;
import org.eclipse.che.commons.json.JsonHelper;
import org.eclipse.che.commons.json.JsonParseException;
import org.eclipse.che.commons.lang.IoUtil;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.commons.lang.ZipUtils;
import com.google.common.hash.Hashing;
import com.google.common.io.CharStreams;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.apache.commons.fileupload.MultipartStream;
import org.everrest.core.impl.header.HeaderParameterParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of SourcesManager that stores sources locally and gets only updated files over virtual file system RESt API.
 *
 * @author andrew00x
 * @author Eugene Voevodin
 */
// TODO: make singleton
public class SourcesManagerImpl implements SourcesManager {
    private static final Logger LOG = LoggerFactory.getLogger(SourcesManagerImpl.class);

    private final java.io.File                        directory;
    private final ConcurrentMap<String, Future<Void>> tasks;
    private final AtomicReference<String>             projectKeyHolder;
    private final Set<SourceManagerListener>          listeners;
    private final ScheduledExecutorService            executor;

    private static final long KEEP_PROJECT_TIME = TimeUnit.MINUTES.toMillis(30);
    private static final int  CONNECT_TIMEOUT   = (int)TimeUnit.MINUTES.toMillis(4);//This time is chosen empirically and
    private static final int  READ_TIMEOUT      = (int)TimeUnit.MINUTES.toMillis(4);//necessary for some large projects. See IDEX-1957.

    public SourcesManagerImpl(java.io.File directory) {
        this.directory = directory;
        tasks = new ConcurrentHashMap<>();
        projectKeyHolder = new AtomicReference<>();
        executor = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder().setNameFormat(getClass().getSimpleName() + "_FileCleaner").setDaemon(true).build());
        listeners = new CopyOnWriteArraySet<>();
    }

    public void start() { // TODO: guice must do this
        executor.scheduleAtFixedRate(createSchedulerTask(), 5, 5, TimeUnit.MINUTES);
    }

    public void stop() { // TODO: guice must do this
        listeners.clear();
        executor.shutdown();
    }

    public void getSources(BuildLogger logger, BuilderConfiguration configuration) throws IOException {
        final BaseBuilderRequest request = configuration.getRequest();
        getSources(logger, request.getWorkspace(), request.getProject(), request.getSourcesUrl(), configuration.getWorkDir());
    }

    @Override
    public void getSources(BuildLogger logger, String workspace, String project, final String sourcesUrl, java.io.File workDir)
            throws IOException {
        // Directory for sources. Keep sources to avoid download whole project before build.
        // This directory is not permanent and may be removed at any time.
        final java.io.File srcDir = new java.io.File(directory, workspace + java.io.File.separatorChar + project);
        // Temporary directory where we copy sources before build.
        final String key = workspace + project;
        try {
            synchronized (this) {
                while (key.equals(projectKeyHolder.get())) {
                    wait();
                }
            }
        } catch (InterruptedException e) {
            LOG.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
        // Avoid multiple threads download source of the same project.
        Future<Void> future = tasks.get(key);
        final ValueHolder<IOException> errorHolder = new ValueHolder<>();
        if (future == null) {
            final FutureTask<Void> newFuture = new FutureTask<>(new Runnable() {
                @Override
                public void run() {
                    try {
                        download(sourcesUrl, srcDir);
                    } catch (IOException e) {
                        LOG.error(e.getMessage(), e);
                        errorHolder.set(e);
                    }
                }
            }, null);
            future = tasks.putIfAbsent(key, newFuture);
            if (future == null) {
                future = newFuture;
                try {
                    // Need a bit time before to publish sources download start message via websocket
                    // as client may not have already subscribed to the channel so early in build task execution
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    LOG.error(e.getMessage(), e);
                }
                logger.writeLine("[INFO] Injecting source code into builder...");
                newFuture.run();
                logger.writeLine("[INFO] Source code injection finished"
                                 + "\n[INFO] ------------------------------------------------------------------------");
            }
        }
        try {
            future.get(); // Block thread until download is completed.
            final IOException ioError = errorHolder.get();
            if (ioError != null) {
                throw ioError;
            }
            IoUtil.copy(srcDir, workDir, IoUtil.ANY_FILTER);
            for (SourceManagerListener listener : listeners) {
                listener.afterDownload(new SourceManagerEvent(workspace, project, sourcesUrl, workDir));
            }
            if (!srcDir.setLastModified(System.currentTimeMillis())) {
                LOG.error("Unable update modification date of {} ", srcDir);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            // Runnable does not throw checked exceptions.
            final Throwable cause = e.getCause();
            if (cause instanceof Error) {
                throw (Error)cause;
            } else {
                throw (RuntimeException)cause;
            }
        } finally {
            tasks.remove(key);
        }
    }

    static final OutputStream DEV_NULL = new OutputStream() {
        public void write(byte[] b, int off, int len) {
        }

        public void write(int b) {
        }

        public void write(byte[] b) throws IOException {
        }
    };

    private void download(String downloadUrl, java.io.File downloadTo) throws IOException {
        HttpURLConnection conn = null;
        try {
            final LinkedList<java.io.File> q = new LinkedList<>();
            q.add(downloadTo);
            final long start = System.currentTimeMillis();
            final List<Pair<String, String>> md5sums = new LinkedList<>();
            while (!q.isEmpty()) {
                java.io.File current = q.pop();
                java.io.File[] list = current.listFiles();
                if (list != null) {
                    for (java.io.File f : list) {
                        if (f.isDirectory()) {
                            q.push(f);
                        } else {
                            md5sums.add(Pair.of(com.google.common.io.Files.hash(f, Hashing.md5()).toString(),
                                                downloadTo.toPath().relativize(f.toPath()).toString()
                                                          .replace("\\", "/"))); //Replacing of "\" is need for windows support
                        }
                    }
                }
            }
            final long end = System.currentTimeMillis();
            if (md5sums.size() > 0) {
                LOG.debug("count md5sums of {} files, time: {}ms", md5sums.size(), (end - start));
            }
            conn = (HttpURLConnection)new URL(downloadUrl).openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            if (!md5sums.isEmpty()) {
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-type", "text/plain");
                conn.setRequestProperty("Accept", "multipart/form-data");
                conn.setDoOutput(true);
                try (OutputStream output = conn.getOutputStream();
                     Writer writer = new OutputStreamWriter(output)) {
                    for (Pair<String, String> pair : md5sums) {
                        writer.write(pair.first);
                        writer.write(' ');
                        writer.write(pair.second);
                        writer.write('\n');
                    }
                }
            }
            final int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                final String contentType = conn.getHeaderField("content-type");
                if (contentType.startsWith("multipart/form-data")) {
                    final HeaderParameterParser headerParameterParser = new HeaderParameterParser();
                    final String boundary = headerParameterParser.parse(contentType).get("boundary");
                    try (InputStream in = conn.getInputStream()) {
                        MultipartStream multipart = new MultipartStream(in, boundary.getBytes());
                        boolean hasMore = multipart.skipPreamble();
                        while (hasMore) {
                            final Map<String, List<String>> headers =
                                    parseChunkHeader(CharStreams.readLines(new StringReader(multipart.readHeaders())));
                            final List<String> contentDisposition = headers.get("content-disposition");
                            final String name = headerParameterParser.parse(contentDisposition.get(0)).get("name");
                            if ("updates".equals(name)) {
                                int length = -1;
                                List<String> contentLengthHeader = headers.get("content-length");
                                if (contentLengthHeader != null && !contentLengthHeader.isEmpty()) {
                                    length = Integer.parseInt(contentLengthHeader.get(0));
                                }
                                if (length < 0 || length > 204800) {
                                    java.io.File tmp = java.io.File.createTempFile("tmp", ".zip", directory);
                                    try {
                                        try (FileOutputStream fOut = new FileOutputStream(tmp)) {
                                            multipart.readBodyData(fOut);
                                        }
                                        ZipUtils.unzip(tmp, downloadTo);
                                    } finally {
                                        if (tmp.exists()) {
                                            tmp.delete();
                                        }
                                    }
                                } else {
                                    final ByteArrayOutputStream bOut = new ByteArrayOutputStream(length);
                                    multipart.readBodyData(bOut);
                                    ZipUtils.unzip(new ByteArrayInputStream(bOut.toByteArray()), downloadTo);
                                }
                            } else if ("removed-paths".equals(name)) {
                                final ByteArrayOutputStream bOut = new ByteArrayOutputStream();
                                multipart.readBodyData(bOut);
                                final String[] removed =
                                        JsonHelper.fromJson(new ByteArrayInputStream(bOut.toByteArray()), String[].class, null);
                                for (String path : removed) {
                                    java.io.File f = new java.io.File(downloadTo, path);
                                    if (!f.delete()) {
                                        throw new IOException(String.format("Unable delete %s", path));
                                    }
                                }
                            } else {
                                // To /dev/null :)
                                multipart.readBodyData(DEV_NULL);
                            }
                            hasMore = multipart.readBoundary();
                        }
                    }
                } else {
                    try (InputStream in = conn.getInputStream()) {
                        ZipUtils.unzip(in, downloadTo);
                    }
                }
            } else if (responseCode != HttpURLConnection.HTTP_NO_CONTENT) {
                throw new IOException(String.format("Invalid response status %d from remote server. ", responseCode));
            }
        } catch (ParseException | JsonParseException e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private Map<String, List<String>> parseChunkHeader(List<String> rawHeaders) throws IOException {
        final Map<String, List<String>> headers = new HashMap<>();
        for (String field : rawHeaders) {
            if (field.isEmpty()) {
                continue;
            }
            String name;
            String value = null;
            int colonPos = field.indexOf(':');
            if (colonPos > 0) {
                name = field.substring(0, colonPos).trim().toLowerCase();
                value = field.substring(colonPos + 1).trim();
            } else {
                name = field.trim().toLowerCase();
            }
            List<String> values = headers.get(name);
            if (values == null) {
                headers.put(name, values = new LinkedList<>());
            }
            if (value != null) {
                values.add(value);
            }
        }
        return headers;
    }

    @Override
    public java.io.File getDirectory() {
        return directory;
    }

    @Override
    public boolean addListener(SourceManagerListener listener) {
        return listeners.add(listener);
    }

    @Override
    public boolean removeListener(SourceManagerListener listener) {
        return listeners.remove(listener);
    }

    /**
     * Create runnable task that will check last files modifications and remove any of them if it needed.
     *
     * @return runnable task for scheduler
     */
    private Runnable createSchedulerTask() {
        return new Runnable() {
            @Override
            public void run() {
                //get list of workspaces
                java.io.File[] workspaces = directory.listFiles();
                for (java.io.File workspace : workspaces) {
                    //get list of workspace projects
                    java.io.File[] projects = workspace.listFiles();
                    for (java.io.File project : projects) {
                        String key = workspace.getName() + project.getName();
                        //if project is not downloading
                        if (tasks.get(key) == null) {
                            projectKeyHolder.set(key);
                            try {
                                final long lastModifiedMillis = project.lastModified();
                                if ((System.currentTimeMillis() - lastModifiedMillis) >= KEEP_PROJECT_TIME) {
                                    IoUtil.deleteRecursive(project);
                                    LOG.debug("Remove project {} that is unused since {}", project, lastModifiedMillis);
                                }
                            } finally {
                                projectKeyHolder.set(null);
                                synchronized (SourcesManagerImpl.this) {
                                    SourcesManagerImpl.this.notify();
                                }
                            }
                        }
                    }
                }
            }
        };
    }
}
