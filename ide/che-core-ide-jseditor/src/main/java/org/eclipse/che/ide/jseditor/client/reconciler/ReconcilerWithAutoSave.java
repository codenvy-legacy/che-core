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
package org.eclipse.che.ide.jseditor.client.reconciler;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import org.eclipse.che.ide.api.editor.EditorInput;
import org.eclipse.che.ide.api.text.Region;
import org.eclipse.che.ide.api.text.RegionImpl;
import org.eclipse.che.ide.api.text.TypedRegion;
import org.eclipse.che.ide.jseditor.client.document.Document;
import org.eclipse.che.ide.jseditor.client.document.DocumentHandle;
import org.eclipse.che.ide.jseditor.client.events.DocumentChangeEvent;
import org.eclipse.che.ide.jseditor.client.partition.DocumentPartitioner;
import org.eclipse.che.ide.jseditor.client.texteditor.TextEditor;
import org.eclipse.che.ide.util.loging.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Default implementation of {@link Reconciler}.
 * Also this implementation provide autosave function.
 * Avtosave will performed before 'reconcile'.
 */
/*
   Maybe this class not proper place for autosave function, but for this issue: https://jira.codenvycorp.com/browse/IDEX-2099
   we need to save file content before 'reconcile'.
 */
public class ReconcilerWithAutoSave implements Reconciler {

    private static final int DELAY = 1000;


    private final Map<String, ReconcilingStrategy> strategies;

    private final String partition;

    private final DocumentPartitioner partitioner;

    private DirtyRegionQueue dirtyRegionQueue;

    private final Timer timer = new Timer() {

        @Override
        public void run() {
            save();
        }
    };

    private DocumentHandle documentHandle;
    private TextEditor     editor;

    @AssistedInject
    public ReconcilerWithAutoSave(@Assisted final String partition,
                                  @Assisted final DocumentPartitioner partitioner) {
        this.partition = partition;
        strategies = new HashMap<>();
        this.partitioner = partitioner;
    }

    private void reconcilerDocumentChanged() {
        for (String key : strategies.keySet()) {
            ReconcilingStrategy reconcilingStrategy = strategies.get(key);
            reconcilingStrategy.setDocument(documentHandle.getDocument());
        }

        timer.cancel();
        timer.schedule(DELAY);
    }

    @Override
    public void install(TextEditor editor) {
        this.editor = editor;
        this.dirtyRegionQueue = new DirtyRegionQueue();
        reconcilerDocumentChanged();
    }

    @Override
    public void uninstall() {
        timer.cancel();
    }

    private void save() {
        editor.doSave(new AsyncCallback<EditorInput>() {
            @Override
            public void onFailure(Throwable throwable) {
                Log.error(ReconcilerWithAutoSave.class, throwable);
            }

            @Override
            public void onSuccess(EditorInput editorInput) {
                final DirtyRegion region = dirtyRegionQueue.removeNextDirtyRegion();
                process(region);

            }
        });
    }

    /**
     * Processes a dirty region. If the dirty region is <code>null</code> the whole document is consider being dirty. The dirty region is
     * partitioned by the document and each partition is handed over to a reconciling strategy registered for the partition's content type.
     *
     * @param dirtyRegion the dirty region to be processed
     */
    protected void process(final DirtyRegion dirtyRegion) {

        Region region = dirtyRegion;

        if (region == null) {
            region = new RegionImpl(0, getDocument().getContents().length());
        }

        final List<TypedRegion> regions = computePartitioning(region.getOffset(),
                                                              region.getLength());

        for (final TypedRegion r : regions) {
            final ReconcilingStrategy strategy = getReconcilingStrategy(r.getType());
            if (strategy == null) {
                continue;
            }

            if (dirtyRegion != null) {
                strategy.reconcile(dirtyRegion, r);
            } else {
                strategy.reconcile(r);
            }
        }
    }

    /**
     * Computes and returns the partitioning for the given region of the input document of the reconciler's connected text viewer.
     * 
     * @param offset the region offset
     * @param length the region length
     * @return the computed partitioning
     */
    private List<TypedRegion> computePartitioning(final int offset, final int length) {
        return partitioner.computePartitioning(offset, length);
    }

    /**
     * Returns the input document of the text view this reconciler is installed on.
     * 
     * @return the reconciler document
     */
    protected Document getDocument() {
        return documentHandle.getDocument();
    }

    /**
     * Creates a dirty region for a document event and adds it to the queue.
     * 
     * @param event the document event for which to create a dirty region
     */
    private void createDirtyRegion(final DocumentChangeEvent event) {
        if (event.getLength() == 0 && event.getText() != null) {
            // Insert
            dirtyRegionQueue.addDirtyRegion(new DirtyRegion(event.getOffset(),
                                                            event.getText().length(),
                                                            DirtyRegion.INSERT,
                                                            event.getText()));

        } else if (event.getText() == null || event.getText().length() == 0) {
            // Remove
            dirtyRegionQueue.addDirtyRegion(new DirtyRegion(event.getOffset(),
                                                            event.getLength(),
                                                            DirtyRegion.REMOVE,
                                                            null));

        } else {
            // Replace (Remove + Insert)
            dirtyRegionQueue.addDirtyRegion(new DirtyRegion(event.getOffset(),
                                                            event.getLength(),
                                                            DirtyRegion.REMOVE,
                                                            null));
            dirtyRegionQueue.addDirtyRegion(new DirtyRegion(event.getOffset(),
                                                            event.getText().length(),
                                                            DirtyRegion.INSERT,
                                                            event.getText()));
        }
    }

    @Override
    public ReconcilingStrategy getReconcilingStrategy(final String contentType) {
        return strategies.get(contentType);
    }

    @Override
    public String getDocumentPartitioning() {
        return partition;
    }

    public void addReconcilingStrategy(final String contentType, final ReconcilingStrategy strategy) {
        strategies.put(contentType, strategy);
    }

    @Override
    public void onDocumentChange(final DocumentChangeEvent event) {
        if (documentHandle == null || !documentHandle.isSameAs(event.getDocument())) {
            return;
        }
        createDirtyRegion(event);
        timer.cancel();
        timer.schedule(DELAY);
    }

    @Override
    public void setDocumentHandle(final DocumentHandle handle) {
        this.documentHandle = handle;
    }

    @Override
    public DocumentHandle getDocumentHandle() {
        return this.documentHandle;
    }

}
