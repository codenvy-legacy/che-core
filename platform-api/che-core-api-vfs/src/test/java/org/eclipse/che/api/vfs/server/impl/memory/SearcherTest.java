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
package org.eclipse.che.api.vfs.server.impl.memory;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.eclipse.che.api.vfs.server.VirtualFile;
import org.eclipse.che.api.vfs.server.search.LuceneSearcher;
import org.eclipse.che.api.vfs.shared.dto.Item;
import org.eclipse.che.api.vfs.shared.dto.ItemList;
import org.eclipse.che.commons.lang.Pair;
import org.everrest.core.impl.ContainerResponse;
import org.everrest.core.impl.EnvironmentContext;
import org.everrest.core.tools.ByteArrayContainerResponseWriter;

/**
 * @author andrew00x
 */
public class SearcherTest extends MemoryFileSystemTest {
    private Pair<String[], String>[] queryToResult;
    private VirtualFile              searchTestFolder;
    private String                   searchTestPath;
    private String                   file1;
    private String                   file2;
    private String                   file3;

    private LuceneSearcher  searcher;
    private SearcherManager searcherManager;

    @SuppressWarnings("unchecked")
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        searchTestFolder = mountPoint.getRoot().createFolder("SearcherTest");
        searcher = (LuceneSearcher)mountPoint.getSearcherProvider().getSearcher(mountPoint, true);
        searcherManager = new SearcherManager(searcher.getIndexWriter(), true, new SearcherFactory());

        VirtualFile searchTestFolder = this.searchTestFolder.createFolder("SearcherTest_Folder");
        searchTestPath = searchTestFolder.getPath();

        file1 = searchTestFolder.createFile("SearcherTest_File01", "text/xml", new ByteArrayInputStream("to be or not to be".getBytes()))
                                .getPath();

        file2 = searchTestFolder.createFile("SearcherTest_File02", MediaType.TEXT_PLAIN, new ByteArrayInputStream("to be or not to be".getBytes()))
                                .getPath();

        VirtualFile folder = searchTestFolder.createFolder("folder01");
        String folder1 = folder.getPath();
        file3 = folder.createFile("SearcherTest_File03", MediaType.TEXT_PLAIN, new ByteArrayInputStream("to be or not to be".getBytes())).getPath();
        
        String file4 = searchTestFolder.createFile("SearcherTest_File04", MediaType.TEXT_PLAIN, new ByteArrayInputStream("(1+1):2=1 is right".getBytes())).getPath();
        String file5 = searchTestFolder.createFile("SearcherTest_File05", MediaType.TEXT_PLAIN, new ByteArrayInputStream("Copyright (c) 2012-2015 * All rights reserved".getBytes())).getPath();

        queryToResult = new Pair[16];
        // text
        queryToResult[0] = new Pair<>(new String[]{file1, file2, file3}, "text=to%20be%20or%20not%20to%20be");
        queryToResult[1] = new Pair<>(new String[]{file1, file2, file3}, "text=to%20be%20or");
        // text + media type
        queryToResult[2] = new Pair<>(new String[]{file2, file3}, "text=to%20be%20or&mediaType=text/plain");
        queryToResult[3] = new Pair<>(new String[]{file1}, "text=to%20be%20or&mediaType=text/xml");
        // text + name
        queryToResult[4] = new Pair<>(new String[]{file2}, "text=to%20be%20or&name=*File02");
        queryToResult[5] = new Pair<>(new String[]{file1, file2, file3}, "text=to%20be%20or&name=SearcherTest*");
        // text + path
        queryToResult[6] = new Pair<>(new String[]{file3}, "text=to%20be%20or&path=" + folder1);
        queryToResult[7] = new Pair<>(new String[]{file1, file2, file3}, "text=to%20be%20or&path=" + searchTestPath);
        // name + media type
        queryToResult[8] = new Pair<>(new String[]{file2, file3, file4, file5}, "name=SearcherTest*&mediaType=text/plain");
        queryToResult[9] = new Pair<>(new String[]{file1}, "name=SearcherTest*&mediaType=text/xml");
        // text is a "contains" query
        queryToResult[10] = new Pair<>(new String[]{file4, file5}, "text=/.*right.*/");
        queryToResult[11] = new Pair<>(new String[]{file5}, "text=/.*rights.*/");
        // text is a regular expression
        queryToResult[12] = new Pair<>(new String[]{file4, file5}, "text=/.*\\(.*\\).*/");
        queryToResult[13] = new Pair<>(new String[]{file5}, "text=/.*\\([a-z]\\).*/");
        // text contains special characters
        queryToResult[14] = new Pair<>(new String[]{file4}, "text=\\(1\\%2B1\\)\\:2=1");
        queryToResult[15] = new Pair<>(new String[]{file5}, "text=\\(c\\)%202012\\-2015%20\\*");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void testSearch() throws Exception {
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        for (Pair<String[], String> pair : queryToResult) {
            ContainerResponse response = doSearchRequest(null, null, defaultHeaderMap(), pair.second.getBytes(), writer, null);
            assertEquals("Error: " + response.getEntity(), 200, response.getStatus());
            List<Item> result = ((ItemList)response.getEntity()).getItems();
            assertEquals(String.format(
                    "Expected %d but found %d for query %s", pair.first.length, result.size(), pair.second),
                         pair.first.length,
                         result.size());
            List<String> resultPaths = new ArrayList<>(result.size());
            result.stream().forEach((Item item) -> {
                resultPaths.add(item.getPath());
            });
            List<String> copy = new ArrayList<>(resultPaths);
            copy.removeAll(Arrays.asList(pair.first));
            assertTrue(String.format("Expected result is %s but found %s", Arrays.toString(pair.first), resultPaths), copy.isEmpty());
            writer.reset();
        }
    }
    
    
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void testSearchNegativeBadMaxItemsValue() throws Exception {
        ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
        Pair<String[], String> pair = queryToResult[0];
        // maxItems should be >= -1 to be valid, a value higher then 1000 is still considered valid, but result will still be capped to at most 1000 values.
        ContainerResponse response = doSearchRequest(-6, null, defaultHeaderMap(), pair.second.getBytes(), responseWriter, null);
        // expecting request to fail due to bad maxItems query parameter value
        assertEquals("Error: " + response.getEntity(), 500, response.getStatus());
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void testSearchNegativeBadSkipCountValue() throws Exception {
        ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
        Pair<String[], String> pair = queryToResult[0];
        // skipCount value should be a natural number < maxItems
        ContainerResponse response = doSearchRequest(5, 7, defaultHeaderMap(), pair.second.getBytes(), responseWriter, null);
        // expecting request to fail due to bad maxItems query parameter value
        assertEquals("Error: " + response.getEntity(), 409, response.getStatus());
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void testSearchMaxItemsLimitation() throws Exception {
        ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
        Pair<String[], String> pair = queryToResult[0];
        ContainerResponse response = doSearchRequest(2, null, defaultHeaderMap(), pair.second.getBytes(), responseWriter, null);
        // expecting request to fail due to bad maxItems query parameter value
        assertEquals("Error: " + response.getEntity(), 200, response.getStatus());
         List<Item> result = ((ItemList)response.getEntity()).getItems();
        // excpected items size to be limited by maxItem query parameter
        assertEquals(2, result.size());
        // search result is actually 3 items long, expecting the flag hasMoreItems to be true
        assertEquals(true, ((ItemList)response.getEntity()).isHasMoreItems());
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void testSearchSkipCountLimitation() throws Exception {
        ByteArrayContainerResponseWriter responseWriter = new ByteArrayContainerResponseWriter();
        Pair<String[], String> pair = queryToResult[0];
        ContainerResponse response = doSearchRequest(3, 2, defaultHeaderMap(), pair.second.getBytes(), responseWriter, null);
        // expecting request to fail due to bad maxItems query parameter value
        assertEquals("Error: " + response.getEntity(), 200, response.getStatus());
         List<Item> result = ((ItemList)response.getEntity()).getItems();
        // excpected items size to be limited by maxItem query parameter, and 2 items should be skiped, yielding only 1 item in result
        assertEquals(1, result.size());
        // search result is actually 3 items long, but skipCount has limited the result to 1 item, thus, expecting the flag hasMoreItems to be false
        assertEquals(false, ((ItemList)response.getEntity()).isHasMoreItems());
    }

    public void testDelete() throws Exception {
        searcherManager.maybeRefresh();
        IndexSearcher luceneSearcher = searcherManager.acquire();
        TopDocs topDocs = luceneSearcher.search(new TermQuery(new Term("path", file1)), 10);
        assertEquals(1, topDocs.totalHits);
        searcherManager.release(luceneSearcher);

        mountPoint.getVirtualFile(file1).delete(null);
        searcherManager.maybeRefresh();
        luceneSearcher = searcherManager.acquire();
        topDocs = luceneSearcher.search(new TermQuery(new Term("path", file1)), 10);
        assertEquals(0, topDocs.totalHits);
        searcherManager.release(luceneSearcher);
    }

    public void testDelete2() throws Exception {
        searcherManager.maybeRefresh();
        IndexSearcher luceneSearcher = searcherManager.acquire();
        TopDocs topDocs = luceneSearcher.search(new PrefixQuery(new Term("path", searchTestPath)), 10);
        assertEquals(5, topDocs.totalHits);
        searcherManager.release(luceneSearcher);

        mountPoint.getVirtualFile(searchTestPath).delete(null);
        searcherManager.maybeRefresh();
        luceneSearcher = searcherManager.acquire();
        topDocs = luceneSearcher.search(new PrefixQuery(new Term("path", searchTestPath)), 10);
        assertEquals(0, topDocs.totalHits);
        searcherManager.release(luceneSearcher);
    }

    public void testAdd() throws Exception {
        searcherManager.maybeRefresh();
        IndexSearcher luceneSearcher = searcherManager.acquire();
        TopDocs topDocs = luceneSearcher.search(new PrefixQuery(new Term("path", searchTestPath)), 10);
        assertEquals(5, topDocs.totalHits);
        searcherManager.release(luceneSearcher);
        mountPoint.getVirtualFile(searchTestPath).createFile("new_file", MediaType.TEXT_PLAIN, new ByteArrayInputStream(DEFAULT_CONTENT_BYTES));

        searcherManager.maybeRefresh();
        luceneSearcher = searcherManager.acquire();
        topDocs = luceneSearcher.search(new PrefixQuery(new Term("path", searchTestPath)), 10);
        assertEquals(6, topDocs.totalHits);
        searcherManager.release(luceneSearcher);
    }

    public void testUpdate() throws Exception {
        searcherManager.maybeRefresh();
        IndexSearcher luceneSearcher = searcherManager.acquire();
        TopDocs topDocs = luceneSearcher.search(
                new QueryParser("text", new SimpleAnalyzer()).parse("updated"), 10);
        assertEquals(0, topDocs.totalHits);
        searcherManager.release(luceneSearcher);
        mountPoint.getVirtualFile(file2).updateContent(MediaType.TEXT_PLAIN, new ByteArrayInputStream("updated content".getBytes()), null);

        searcherManager.maybeRefresh();
        luceneSearcher = searcherManager.acquire();
        topDocs = luceneSearcher.search(new QueryParser("text", new SimpleAnalyzer()).parse("updated"), 10);
        assertEquals(1, topDocs.totalHits);
        searcherManager.release(luceneSearcher);
    }

    public void testMove() throws Exception {
        searcherManager.maybeRefresh();
        IndexSearcher luceneSearcher = searcherManager.acquire();
        String destination = searchTestFolder.createFolder("___destination").getPath();
        String expected = destination + '/' + "SearcherTest_File03";
        TopDocs topDocs = luceneSearcher.search(new PrefixQuery(new Term("path", expected)), 10);
        assertEquals(0, topDocs.totalHits);
        searcherManager.release(luceneSearcher);
        mountPoint.getVirtualFile(file3).moveTo(mountPoint.getVirtualFile(destination), null, false, null);

        searcherManager.maybeRefresh();
        luceneSearcher = searcherManager.acquire();
        topDocs = luceneSearcher.search(new PrefixQuery(new Term("path", expected)), 10);
        assertEquals(1, topDocs.totalHits);
        topDocs = luceneSearcher.search(new PrefixQuery(new Term("path", file3)), 10);
        assertEquals(0, topDocs.totalHits);
        searcherManager.release(luceneSearcher);
    }

    public void testCopy() throws Exception {
        searcherManager.maybeRefresh();
        IndexSearcher luceneSearcher = searcherManager.acquire();
        String destination = searchTestFolder.createFolder("___destination").getPath();
        String expected = destination + '/' + "SearcherTest_File03";
        TopDocs topDocs = luceneSearcher.search(new PrefixQuery(new Term("path", expected)), 10);
        assertEquals(0, topDocs.totalHits);
        searcherManager.release(luceneSearcher);
        mountPoint.getVirtualFile(file3).copyTo(mountPoint.getVirtualFile(destination), null, false);

        searcherManager.maybeRefresh();
        luceneSearcher = searcherManager.acquire();
        topDocs = luceneSearcher.search(new PrefixQuery(new Term("path", expected)), 10);
        assertEquals(1, topDocs.totalHits);
        topDocs = luceneSearcher.search(new PrefixQuery(new Term("path", file3)), 10);
        assertEquals(1, topDocs.totalHits);
        searcherManager.release(luceneSearcher);
    }

    public void testRename() throws Exception {
        String newName = "___renamed";
        searcherManager.maybeRefresh();
        IndexSearcher luceneSearcher = searcherManager.acquire();
        TopDocs topDocs = luceneSearcher.search(new PrefixQuery(new Term("path", file3)), 10);
        assertEquals(1, topDocs.totalHits);
        searcherManager.release(luceneSearcher);
        mountPoint.getVirtualFile(file2).rename(newName, null, null);

        searcherManager.maybeRefresh();
        luceneSearcher = searcherManager.acquire();
        topDocs = luceneSearcher.search(new PrefixQuery(new Term("path", searchTestPath + '/' + newName)), 10);
        assertEquals(1, topDocs.totalHits);
        topDocs = luceneSearcher.search(new PrefixQuery(new Term("path", file2)), 10);
        assertEquals(0, topDocs.totalHits);
        searcherManager.release(luceneSearcher);
    }
    
    // ----------- UTILITY METHODS ----------- 
    
    /**
     * This method provides search request logic shared among multiple tests
     * @param maxItems value for 'maxItems' query parameter
     * @param skipCount value for 'skipCount' query parameter
     * @param headers map containing headers of request
     * @param content the byte[] content to search in
     * @param writer the response writer
     * @param env the environment context
     * @throws Exception 
     */
    private ContainerResponse doSearchRequest(Integer maxItems, Integer skipCount, Map<String, List<String>> headers, byte[] content, ByteArrayContainerResponseWriter writer, EnvironmentContext env) throws Exception {
        
        UriBuilder builder = UriBuilder.fromPath(SERVICE_URI + "search");
        // setting maxItems query parameter
        if(maxItems != null){
            builder = builder.queryParam("maxItems", maxItems.toString());
        }
        // setting skipCount query parameter
        if(skipCount != null){
            builder = builder.queryParam("skipCount", skipCount.toString());
        }
        String requestPath = builder.build().toString();
        return launcher.service(HttpMethod.POST, requestPath, BASE_URI, headers, content, writer, env);
    }
    
    private Map<String, List<String>> defaultHeaderMap(){
        Map<String, List<String>> theHeadersMap = new HashMap<>(1);
        theHeadersMap.put(HttpHeaders.CONTENT_TYPE, Arrays.asList(MediaType.APPLICATION_FORM_URLENCODED));
        return theHeadersMap;
    }
}
