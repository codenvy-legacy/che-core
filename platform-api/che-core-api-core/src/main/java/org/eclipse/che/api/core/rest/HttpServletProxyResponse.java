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
package org.eclipse.che.api.core.rest;

import org.eclipse.che.commons.lang.Pair;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** @author andrew00x */
public final class HttpServletProxyResponse implements HttpOutputMessage {
    private final HttpServletResponse httpServletResponse;

    private String                                   contentType;
    private Map<Pattern, List<Pair<String, String>>> rewriteMap;

    public HttpServletProxyResponse(HttpServletResponse httpServletResponse) {
        this.httpServletResponse = httpServletResponse;
    }

    public HttpServletProxyResponse(HttpServletResponse httpServletResponse, Map<Pattern, List<Pair<String, String>>> rewriteMap) {
        this.httpServletResponse = httpServletResponse;
        this.rewriteMap = rewriteMap;
    }

    @Override
    public void setStatus(int status) {
        httpServletResponse.setStatus(status);
    }

    @Override
    public void setContentType(String contentType) {
        setHttpHeader(HttpHeaders.CONTENT_TYPE, contentType);
    }

    @Override
    public void addHttpHeader(String name, String value) {
        if ("content-type".equals(name.toLowerCase())) {
            contentType = value;
        }
        httpServletResponse.addHeader(name, value);
    }

    @Override
    public void setHttpHeader(String name, String value) {
        if ("content-type".equals(name.toLowerCase())) {
            contentType = value;
        }
        httpServletResponse.setHeader(name, value);
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (contentType != null && rewriteMap != null) {
            for (Map.Entry<Pattern, List<Pair<String, String>>> rewriteMapEntry : rewriteMap.entrySet()) {
                Matcher matcher = rewriteMapEntry.getKey().matcher(contentType);
                if (matcher.matches()) {
                    return new RewriteOutputStream(httpServletResponse, rewriteMapEntry.getValue());
                }
            }
        }
        return httpServletResponse.getOutputStream();
    }

    public void setRewriteMap(Map<Pattern, List<Pair<String, String>>> rewriteMap) {
        this.rewriteMap = rewriteMap;
    }

    private class RewriteOutputStream extends OutputStream {

        final HttpServletResponse        httpServletResponse;
        final List<Pair<String, String>> rewriteRules;
        ByteArrayOutputStream cache;
        Writer                writer;

        RewriteOutputStream(HttpServletResponse httpServletResponse, List<Pair<String, String>> rewriteRules) {
            this.httpServletResponse = httpServletResponse;
            this.rewriteRules = rewriteRules;
        }

        @Override
        public void write(int b) throws IOException {
            if (cache == null) {
                cache = new ByteArrayOutputStream();
            }
            cache.write(b);
            if (b == '\n' || b == '\r') {
                final String translatedLine = translateLine();
                getWriter().write(translatedLine);
                cache.reset();
            }
        }

        String translateLine() {
            String translatedLine = cache.toString();
            for (Pair<String, String> rewriteRule : rewriteRules) {
                translatedLine = translatedLine.replaceAll(rewriteRule.first, rewriteRule.second);
            }
            return translatedLine;
        }

        Writer getWriter() throws IOException {
            if (writer == null) {
                writer = httpServletResponse.getWriter();
            }
            return writer;
        }

        @Override
        public void flush() throws IOException {
            if (cache != null) {
                final String translatedLine = translateLine();
                final Writer myWriter = getWriter();
                myWriter.write(translatedLine);
                writer.flush();
                cache.reset();
            } else if (writer != null) {
                writer.flush();
            }
        }

        @Override
        public void close() throws IOException {
            flush();
            if (writer != null) {
                writer.close();
            }
        }
    }
}
