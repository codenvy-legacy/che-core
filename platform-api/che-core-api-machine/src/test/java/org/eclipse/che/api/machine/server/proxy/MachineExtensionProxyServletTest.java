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
package org.eclipse.che.api.machine.server.proxy;

import org.eclipse.che.api.machine.server.MachineManager;
import org.eclipse.che.api.machine.server.impl.MachineImpl;
import org.eclipse.che.api.machine.server.impl.ServerImpl;
import org.eclipse.che.api.machine.server.spi.InstanceMetadata;
import org.eclipse.che.api.machine.shared.Server;
import org.eclipse.che.commons.lang.IoUtil;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.everrest.assured.util.AvailablePortFinder;
import org.everrest.test.mock.MockHttpServletRequest;
import org.everrest.test.mock.MockHttpServletResponse;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class MachineExtensionProxyServletTest {
    private static final String MACHINE_ID              = "machine123";
    private static final int    EXTENSIONS_API_PORT     = 4301;
    private static final String PROXY_ENDPOINT          = "http://localhost:8080";
    private static final String DESTINATION_BASEPATH    = "/java/";
    private static final String DEFAULT_RESPONSE_ENTITY = "{\"key1\":\"value1\",\"key2\":\"value2\"}";

    private Map<String, Server> machineServers;

    private MachineManager machineManager;

    private MachineImpl machine;

    private InstanceMetadata instanceMetadata;

    private MachineExtensionProxyServlet proxyServlet;

    private org.eclipse.jetty.server.Server jettyServer;

    private ExtensionApiResponse extensionApiResponse;

    private ExtensionApiRequest extensionApiRequest;

    // Todo
    // send entity to destination
    // check that proxy doesn't copy hop-by-hop headers
    // check headers send to destination
    // check used destination url
    // machine does not exist
    // request url does not contain machine id
    // no server on destination side
    // all type of response codes from destination side
    // https to http proxy
    // json object in response
    // json object in request
    // html in response
    // including cookies and http-only cookies
    // secure cookies for https
    // read entity from error stream of destination response
    // check that cookies are not saved in proxy between requests
    // responses on exceptions
    // responses on missing machine id in utl

    @BeforeClass
    public void setUpClass() throws Exception {
        jettyServer = new org.eclipse.jetty.server.Server(AvailablePortFinder.getNextAvailable(10000 + new Random().nextInt(10000)));
        jettyServer.setHandler(new ExtensionApiHandler());
        jettyServer.start();

        final Connector connector = jettyServer.getConnectors()[0];
        machineServers = Collections.<String, Server>singletonMap(String.valueOf(EXTENSIONS_API_PORT),
                                                                  new ServerImpl(null,
                                                                                 "localhost:" + connector.getPort(),
                                                                                 null));
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
        machineManager = mock(MachineManager.class);

        machine = mock(MachineImpl.class);

        instanceMetadata = mock(InstanceMetadata.class);

        extensionApiResponse = spy(new ExtensionApiResponse());

        extensionApiRequest = new ExtensionApiRequest();

        proxyServlet = new MachineExtensionProxyServlet(4301, machineManager);
    }

    @AfterClass
    public void tearDown() throws Exception {
        jettyServer.stop();
    }

    @Test(dataProvider = "methodProvider")
    public void shouldBeAbleToProxyRequestWithDifferentMethod(String method) throws Exception {
        when(machineManager.getMachine(MACHINE_ID)).thenReturn(machine);
        when(machine.getMetadata()).thenReturn(instanceMetadata);
        when(instanceMetadata.getServers()).thenReturn(machineServers);

        MockHttpServletRequest mockRequest =
                new MockHttpServletRequest(PROXY_ENDPOINT + "/api/ext/" + MACHINE_ID + DESTINATION_BASEPATH,
                                           new ByteArrayInputStream(new byte[0]),
                                           0,
                                           method,
                                           Collections.<String, List<String>>emptyMap());

        MockHttpServletResponse mockResponse = new MockHttpServletResponse();

        proxyServlet.service(mockRequest, mockResponse);

        assertEquals(mockResponse.getStatus(), 200);
    }

    @DataProvider(name = "methodProvider")
    public String[][] methodProvider() {
        return new String[][]{{"GET"}, {"PUT"}, {"POST"}, {"DELETE"}, {"OPTIONS"}};
    }

    @Test
    public void shouldCopyEntityFromResponse() throws Exception {
        when(machineManager.getMachine(MACHINE_ID)).thenReturn(machine);
        when(machine.getMetadata()).thenReturn(instanceMetadata);
        when(instanceMetadata.getServers()).thenReturn(machineServers);

        MockHttpServletRequest mockRequest =
                new MockHttpServletRequest(PROXY_ENDPOINT + "/api/ext/" + MACHINE_ID + DESTINATION_BASEPATH,
                                           new ByteArrayInputStream(new byte[0]),
                                           0,
                                           "GET",
                                           Collections.<String, List<String>>emptyMap());

        MockHttpServletResponse mockResponse = new MockHttpServletResponse();

        proxyServlet.service(mockRequest, mockResponse);

        assertEquals(mockResponse.getStatus(), 200);
        assertEquals(mockResponse.getOutputContent(), DEFAULT_RESPONSE_ENTITY);
    }

    @Test
    public void shouldBeAbleToProxyWithRightPath() throws Exception {
        when(machineManager.getMachine(MACHINE_ID)).thenReturn(machine);
        when(machine.getMetadata()).thenReturn(instanceMetadata);
        when(instanceMetadata.getServers()).thenReturn(machineServers);

        final String destPath = DESTINATION_BASEPATH + "codeassistant/index";

        MockHttpServletRequest mockRequest =
                new MockHttpServletRequest(PROXY_ENDPOINT + "/che/ext/" + MACHINE_ID + destPath,
                                           new ByteArrayInputStream(new byte[0]),
                                           0,
                                           "GET",
                                           Collections.<String, List<String>>emptyMap());

        MockHttpServletResponse mockResponse = new MockHttpServletResponse();

        proxyServlet.service(mockRequest, mockResponse);

        assertEquals(mockResponse.getStatus(), 200);
        assertEquals(extensionApiRequest.uri, destPath);
    }

    @Test
    public void shouldProxyWithQueryString() throws Exception {
        when(machineManager.getMachine(MACHINE_ID)).thenReturn(machine);
        when(machine.getMetadata()).thenReturn(instanceMetadata);
        when(instanceMetadata.getServers()).thenReturn(machineServers);

        final String query = "key1=value1&key2=value2&key2=value3";

        MockHttpServletRequest mockRequest =
                new MockHttpServletRequest(PROXY_ENDPOINT + "/api/ext/" + MACHINE_ID + DESTINATION_BASEPATH + "?" + query,
                                           new ByteArrayInputStream(new byte[0]),
                                           0,
                                           "GET",
                                           Collections.<String, List<String>>emptyMap());

        MockHttpServletResponse mockResponse = new MockHttpServletResponse();

        proxyServlet.service(mockRequest, mockResponse);

        assertEquals(mockResponse.getStatus(), 200);
        assertEquals(extensionApiRequest.query, query);
    }

    @Test
    public void shouldProxyResponseWithError() throws Exception {
        when(machineManager.getMachine(MACHINE_ID)).thenReturn(machine);
        when(machine.getMetadata()).thenReturn(instanceMetadata);
        when(instanceMetadata.getServers()).thenReturn(machineServers);

        MockHttpServletRequest mockRequest =
                new MockHttpServletRequest(PROXY_ENDPOINT + "/api/ext/" + MACHINE_ID + "/not/existing/path",
                                           new ByteArrayInputStream(new byte[0]),
                                           0,
                                           "GET",
                                           Collections.<String, List<String>>emptyMap());

        MockHttpServletResponse mockResponse = new MockHttpServletResponse();

        proxyServlet.service(mockRequest, mockResponse);

        assertEquals(mockResponse.getStatus(), 404);
        assertEquals(mockResponse.getOutputContent(), "Che service not found");
    }

    @Test
    public void shouldCopyHeadersFromResponse() throws Exception {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Accept-Ranges", Collections.singletonList("bytes"));
        headers.put("Allow", Collections.singletonList("GET, HEAD, PUT"));
        headers.put("ETag", Collections.singletonList("xyzzy"));
        headers.put("Expires", Collections.singletonList("Thu, 01 Dec 2020 16:00:00 GMT"));
        headers.put("Last-Modified", Collections.singletonList("Tue, 15 Nov 1994 12:45:26 GMT"));
        headers.put("Retry-After", Collections.singletonList("120"));
        headers.put("Upgrade", Collections.singletonList("HTTP/2.0"));

        when(machineManager.getMachine(MACHINE_ID)).thenReturn(machine);
        when(machine.getMetadata()).thenReturn(instanceMetadata);
        when(instanceMetadata.getServers()).thenReturn(machineServers);
        when(extensionApiResponse.getHeaders()).thenReturn(headers);

        MockHttpServletRequest mockRequest =
                new MockHttpServletRequest(PROXY_ENDPOINT + "/api/ext/" + MACHINE_ID + DESTINATION_BASEPATH,
                                           new ByteArrayInputStream(new byte[0]),
                                           0,
                                           "POST",
                                           Collections.<String, List<String>>emptyMap());

        MockHttpServletResponse mockResponse = new MockHttpServletResponse();

        proxyServlet.service(mockRequest, mockResponse);

        assertEquals(mockResponse.getStatus(), 200);

        final Map<String, List<String>> actualHeaders = getHeaders(mockResponse);
        actualHeaders.remove("content-length");
        actualHeaders.remove("server");
        actualHeaders.remove("date");
        // fixme WTF?
        actualHeaders.remove(null);

        assertEqualsHeaders(actualHeaders, headers);
    }

    @Test
    public void shouldCopyHeadersFromRequest() throws Exception {
        final Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Encoding", Collections.singletonList("gzip"));
        headers.put("Content-Language", Collections.singletonList("mi, en"));
        headers.put("Content-Type", Collections.singletonList("text/html; charset=ISO-8859-4"));
        headers.put("Date", Collections.singletonList("Tue, 15 Nov 1994 08:12:31 GMT"));
        headers.put("From", Collections.singletonList("webmaster@w3.org"));
        headers.put("Accept", Collections.singletonList("*/*"));
        headers.put("Accept-Charset", Collections.singletonList("iso-8859-5, unicode-1-1;q=0.8"));
        headers.put("Accept-Encoding", Collections.singletonList("compress, gzip"));
        headers.put("Accept-Language", Collections.singletonList("da, en-gb;q=0.8, en;q=0.7"));
        headers.put("Referer", Collections.singletonList("http://www.w3.org/hypertext/DataSources/Overview.html"));
        headers.put("Max-Forwards", Collections.singletonList("5"));
        headers.put("If-Modified-Since", Collections.singletonList("Sat, 29 Oct 2016 19:43:31 GMT"));
        headers.put("If-Match", Collections.singletonList("xyzzy"));
        headers.put("Host", Collections.singletonList("www.w3.org"));
        headers.put("User-Agent", Collections
                .singletonList("curl/7.22.0 (x86_64-pc-linux-gnu) libcurl/7.22.0 OpenSSL/1.0.1 zlib/1.2.3.4 libidn/1.23 librtmp/2.3"));
        headers.put("Connection", Collections.singletonList("close"));
        headers.put("Content-Length", Collections.singletonList("0"));
        headers.put("X-Requested-With", Collections.singletonList("XMLHttpRequest"));
        headers.put("Cookie", Collections.singletonList("JSESSIONID=D06F9296FE0D3A48519836666E668893; logged_in=true"));

        when(machineManager.getMachine(MACHINE_ID)).thenReturn(machine);
        when(machine.getMetadata()).thenReturn(instanceMetadata);
        when(instanceMetadata.getServers()).thenReturn(machineServers);

        MockHttpServletRequest mockRequest =
                new MockHttpServletRequest(PROXY_ENDPOINT + "/api/ext/" + MACHINE_ID + DESTINATION_BASEPATH,
                                           new ByteArrayInputStream(new byte[0]),
                                           0,
                                           "POST",
                                           headers);

        final MockHttpServletResponse mockResponse = new MockHttpServletResponse();

        proxyServlet.service(mockRequest, mockResponse);

        assertEquals(mockResponse.getStatus(), 200);

        // should not be copied to headers of request to extension API
        headers.remove("Connection");
        // proxy will set it separately
        extensionApiRequest.headers.remove("Connection");
        // fixme jetty return 127.0.0.1:jettyPort
        // I suppose we need add X-Forwarded-* headers to fix it
        headers.remove("Host");
        extensionApiRequest.headers.remove("Host");

        assertEqualsHeaders(extensionApiRequest.headers, headers);
    }

    /**
     * Header name is case insensitive in accordance to spec. So we can't compare arrays via equals method.
     *
     * @param actual
     *         map of actual headers
     * @param expected
     *         map of expected headers
     */
    private void assertEqualsHeaders(Map<String, List<String>> actual, Map<String, List<String>> expected) {
        assertEquals(actual.size(), expected.size());

        final Set<String> expectedHeadersKeys = expected.keySet();

        for (Map.Entry<String, List<String>> actualHeader : actual.entrySet()) {
            List<String> expectedHeaderValues = null;
            if (expectedHeadersKeys.contains(actualHeader.getKey())) {
                expectedHeaderValues = expected.get(actualHeader.getKey());
            } else {
                for (String expectedHeaderKey : expectedHeadersKeys) {
                    if (expectedHeaderKey.equalsIgnoreCase(actualHeader.getKey())) {
                        expectedHeaderValues = expected.get(expectedHeaderKey);
                        break;
                    }
                }
            }
            if (expectedHeaderValues != null) {
                assertEquals(actualHeader.getValue(), expectedHeaderValues);
            } else {
                fail("Expected headers don't contain header:" + actualHeader.getKey());
            }
        }
    }

    private Map<String, List<String>> getHeaders(HttpServletRequest req) {
        final Map<String, List<String>> result = new HashMap<>();

        final Enumeration<String> headerNames = req.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            result.put(headerName, new ArrayList<String>());

            final Enumeration<String> headerValues = req.getHeaders(headerName);
            while (headerValues.hasMoreElements()) {
                String headerValue = headerValues.nextElement();
                result.get(headerName).add(headerValue);
            }
        }

        return result;
    }

    private Map<String, List<String>> getHeaders(HttpServletResponse resp) {
        final Map<String, List<String>> result = new HashMap<>();

        for (String headerName : resp.getHeaderNames()) {
            result.put(headerName, new ArrayList<>(resp.getHeaders(headerName)));
        }

        return result;
    }

    private class ExtensionApiHandler extends AbstractHandler {
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                throws IOException, ServletException {
            if (!target.startsWith(DESTINATION_BASEPATH)) {
                response.setStatus(404);
                response.getWriter().format("Che service not found").close();
                return;
            }

            // to be able validate request received by extension API server
            extensionApiRequest.headers = getHeaders(request);
            extensionApiRequest.method = request.getMethod();
            extensionApiRequest.uri = request.getRequestURI();
            extensionApiRequest.query = request.getQueryString();
            try (InputStream is = request.getInputStream()) {
                extensionApiRequest.entity = IoUtil.readStream(is);
            }

            response.setStatus(extensionApiResponse.getStatus());
            for (Map.Entry<String, List<String>> header : extensionApiResponse.getHeaders().entrySet()) {
                for (String headerValue : header.getValue()) {
                    response.addHeader(header.getKey(), headerValue);
                }
            }
            response.getWriter().print(extensionApiResponse.getEntity());

            baseRequest.setHandled(true);
        }
    }

    private static class ExtensionApiResponse {
        int getStatus() {
            return 200;
        }

        String getEntity() {
            return DEFAULT_RESPONSE_ENTITY;
        }

        Map<String, List<String>> getHeaders() {
            return Collections.singletonMap("Content-type", Collections.singletonList("application/json"));
        }
    }

    private static class ExtensionApiRequest {
        Map<String, List<String>> headers;
        String                    entity;
        String                    method;
        String                    uri;
        String                    query;
    }
}