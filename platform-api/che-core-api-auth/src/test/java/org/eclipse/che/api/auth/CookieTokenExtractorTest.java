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
package org.eclipse.che.api.auth;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

@Listeners(MockitoTestNGListener.class)
public class CookieTokenExtractorTest {

    @Mock
    HttpServletRequest request;


    @Test
    public void shouldExtractFromQueryParamFirst() {
        //given
        CookiesTokenExtractor extractor = new CookiesTokenExtractor();
        when(request.getQueryString()).thenReturn("token=0239845");
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("session-access-key", "4356435")});

        //when
        String actual = extractor.getToken(request);
        //then
        assertEquals(actual, "0239845");
    }

    @Test
    public void shouldExtractFromSessionCookie() {
        //given
        CookiesTokenExtractor extractor = new CookiesTokenExtractor();
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("session-access-key", "4356435")});

        //when
        String actual = extractor.getToken(request);
        //then
        assertEquals(actual, "4356435");
    }

    @Test
    public void shouldReturnNullIfNoCookieIsSet() {
        //given
        CookiesTokenExtractor extractor = new CookiesTokenExtractor();

        //when
        String actual = extractor.getToken(request);
        //then
        assertNull(actual);
    }

    @Test
    public void shouldReturnNullIfNoCookieIsSetWithOtherName() {
        //given
        CookiesTokenExtractor extractor = new CookiesTokenExtractor();
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("session-key", "4356435")});

        //when
        String actual = extractor.getToken(request);
        //then
        assertNull(actual);
    }

}