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
package org.eclipse.che.api.vfs.server.search;

import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.vfs.server.LazyIterator;
import org.eclipse.che.api.vfs.server.MountPoint;
import org.eclipse.che.api.vfs.server.VirtualFile;
import org.eclipse.che.api.vfs.server.VirtualFileFilter;
import org.eclipse.che.api.vfs.server.util.MediaTypeFilter;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedList;
import java.util.Set;

/**
 * Lucene based searcher.
 *
 * @author andrew00x
 */
public abstract class LuceneSearcher implements Searcher {
    private static final Logger LOG          = LoggerFactory.getLogger(LuceneSearcher.class);
    private static final int    RESULT_LIMIT = 1000;

    private final VirtualFileFilter filter;

    private IndexWriter     luceneIndexWriter;
    private SearcherManager searcherManager;
    private boolean         closed;

    public LuceneSearcher(Set<String> indexedMediaTypes) {
        this(new MediaTypeFilter(indexedMediaTypes));
    }

    public LuceneSearcher(VirtualFileFilter filter) {
        this.filter = filter;
    }

    protected Analyzer makeAnalyzer() {
        return new Analyzer() {
			@Override
			protected TokenStreamComponents createComponents(String fieldName) {
				Tokenizer tokenizer = new WhitespaceTokenizer();
				TokenStream filter = new LowerCaseFilter(tokenizer);
				return new TokenStreamComponents(tokenizer, filter);
			}
		};
    }

    protected abstract Directory makeDirectory() throws ServerException;

    /**
     * Init lucene index. Need call this method if index directory is clean. Scan all files in virtual filesystem and add to index.
     *
     * @param mountPoint
     *         MountPoint
     * @throws ServerException
     *         if any virtual filesystem error
     */
    public void init(MountPoint mountPoint) throws ServerException {
        doInit();
        addTree(mountPoint.getRoot());
    }

    protected final synchronized void doInit() throws ServerException {
        try {
            luceneIndexWriter = new IndexWriter(makeDirectory(), new IndexWriterConfig(makeAnalyzer()));
            searcherManager = new SearcherManager(luceneIndexWriter, true, new SearcherFactory());
        } catch (IOException e) {
            throw new ServerException(e);
        }
    }

    public synchronized void close() {
        if (!closed) {
            try {
                IOUtils.close(getIndexWriter(), getIndexWriter().getDirectory(), searcherManager);
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
            closed = true;
        }
    }

    public synchronized IndexWriter getIndexWriter() {
        return luceneIndexWriter;
    }

    @Override
    public String[] search(QueryExpression query) throws ServerException {
        final BooleanQuery luceneQuery = new BooleanQuery();
        final String name = query.getName();
        final String path = query.getPath();
        final String mediaType = query.getMediaType();
        final String text = query.getText();
        if (path != null) {
            luceneQuery.add(new PrefixQuery(new Term("path", path)), BooleanClause.Occur.MUST);
        }
        if (name != null) {
            luceneQuery.add(new WildcardQuery(new Term("name", name)), BooleanClause.Occur.MUST);
        }
        if (mediaType != null) {
            luceneQuery.add(new TermQuery(new Term("mediatype", mediaType)), BooleanClause.Occur.MUST);
        }
        if (text != null) {
            QueryParser qParser = new QueryParser("text", makeAnalyzer());
            try {
                luceneQuery.add(qParser.parse(text), BooleanClause.Occur.MUST);
            } catch (ParseException e) {
                throw new ServerException(e.getMessage());
            }
        }
        IndexSearcher luceneSearcher = null;
        try {
            searcherManager.maybeRefresh();
            luceneSearcher = searcherManager.acquire();
            final TopDocs topDocs = luceneSearcher.search(luceneQuery, RESULT_LIMIT);
            if (topDocs.totalHits > RESULT_LIMIT) {
                throw new ServerException(String.format("Too many (%d) matched results found. ", topDocs.totalHits));
            }
            final String[] result = new String[topDocs.scoreDocs.length];
            for (int i = 0, length = result.length; i < length; i++) {
                result[i] = luceneSearcher.doc(topDocs.scoreDocs[i].doc).getField("path").stringValue();
            }
            return result;
        } catch (IOException e) {
            throw new ServerException(e.getMessage(), e);
        } finally {
            try {
                searcherManager.release(luceneSearcher);
            } catch (IOException e) {
                LOG.error(e.getMessage());
            }
        }
    }

    @Override
    public final void add(VirtualFile virtualFile) throws ServerException {
        doAdd(virtualFile);
    }

    protected void doAdd(VirtualFile virtualFile) throws ServerException {
        if (virtualFile.isFolder()) {
            addTree(virtualFile);
        } else {
            addFile(virtualFile);
        }
    }

    protected void addTree(VirtualFile tree) throws ServerException {
        final long start = System.currentTimeMillis();
        final LinkedList<VirtualFile> q = new LinkedList<>();
        q.add(tree);
        int indexedFiles = 0;
        while (!q.isEmpty()) {
            final VirtualFile folder = q.pop();
            if (folder.exists()) {
                LazyIterator<VirtualFile> children = folder.getChildren(VirtualFileFilter.ALL);
                while (children.hasNext()) {
                    final VirtualFile child = children.next();
                    if (child.isFolder()) {
                        q.push(child);
                    } else {
                        addFile(child);
                        indexedFiles++;
                    }
                }
            }
        }
        final long end = System.currentTimeMillis();
        LOG.debug("Indexed {} files from {}, time: {} ms", indexedFiles, tree.getPath(), (end - start));
    }

    protected void addFile(VirtualFile virtualFile) throws ServerException {
        if (virtualFile.exists()) {
            try (Reader fContentReader = filter.accept(virtualFile) ? new BufferedReader(
                    new InputStreamReader(virtualFile.getContent().getStream())) : null) {
                getIndexWriter().updateDocument(new Term("path", virtualFile.getPath()), createDocument(virtualFile, fContentReader));
            } catch (OutOfMemoryError oome) {
                close();
                throw oome;
            } catch (IOException e) {
                throw new ServerException(e.getMessage(), e);
            } catch (ForbiddenException e) {
                throw new ServerException(e.getServiceError());
            }
        }
    }

    @Override
    public final void delete(String path, boolean isFile) throws ServerException {
        try {
            if (isFile) {
                Term term = new Term("path", path);
                getIndexWriter().deleteDocuments(term);
            } else {
                Term term = new Term("path", path + "/");
                getIndexWriter().deleteDocuments(new PrefixQuery(term));
            }
        } catch (OutOfMemoryError oome) {
            close();
            throw oome;
        } catch (IOException e) {
            throw new ServerException(e.getMessage(), e);
        }
    }

    @Override
    public final void update(VirtualFile virtualFile) throws ServerException {
        doUpdate(new Term("path", virtualFile.getPath()), virtualFile);
    }

    protected void doUpdate(Term deleteTerm, VirtualFile virtualFile) throws ServerException {
        try (Reader fContentReader = filter.accept(virtualFile) ? new BufferedReader(
                new InputStreamReader(virtualFile.getContent().getStream())) : null) {
            getIndexWriter().updateDocument(deleteTerm, createDocument(virtualFile, fContentReader));
        } catch (OutOfMemoryError oome) {
            close();
            throw oome;
        } catch (IOException e) {
            throw new ServerException(e.getMessage(), e);
        } catch (ForbiddenException e) {
            throw new ServerException(e.getServiceError());
        }
    }

    protected Document createDocument(VirtualFile virtualFile, Reader inReader) throws ServerException {
        final Document doc = new Document();
        doc.add(new StringField("path", virtualFile.getPath(), Field.Store.YES));
        doc.add(new StringField("name", virtualFile.getName(), Field.Store.YES));
        doc.add(new StringField("mediatype", getMediaType(virtualFile), Field.Store.YES));
        if (inReader != null) {
            doc.add(new TextField("text", inReader));
        }
        return doc;
    }

    /** Get virtual file media type. Any additional parameters (e.g. 'charset') are removed. */
    private String getMediaType(VirtualFile virtualFile) throws ServerException {
        String mediaType = virtualFile.getMediaType();
        final int paramStartIndex = mediaType.indexOf(';');
        if (paramStartIndex != -1) {
            mediaType = mediaType.substring(0, paramStartIndex).trim();
        }
        return mediaType;
    }

}
