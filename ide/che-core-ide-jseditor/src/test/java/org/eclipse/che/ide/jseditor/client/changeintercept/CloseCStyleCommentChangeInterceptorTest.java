/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.jseditor.client.changeintercept;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doReturn;

import org.eclipse.che.ide.jseditor.client.document.ReadOnlyDocument;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.junit.Ignore;

import org.eclipse.che.ide.jseditor.client.text.TextPosition;


/**
 * Test of the c-style bloc comment close interceptor.
 */
@RunWith(MockitoJUnitRunner.class)
public class CloseCStyleCommentChangeInterceptorTest {

    @Mock
    private ReadOnlyDocument document;

    @InjectMocks
    private CloseCStyleCommentChangeInterceptor interceptor;

    /**
     * The input is a normal /* &#42;&#47; comment without leading spaces.
     */
    @Ignore
    @Test
    public void testNotFirstLineNoLeadingSpaces() {
        doReturn("").when(document).getLineContent(0);
        doReturn("/*").when(document).getLineContent(1);
        doReturn(" *").when(document).getLineContent(2);
        final TextChange input = new TextChange.Builder().from(new TextPosition(1, 2))
                                                         .to(new TextPosition(2, 2))
                                                         .insert("\n *")
                                                         .build();
        final TextChange output = interceptor.processChange(input, document);
        assertNotNull(output);
        final TextChange expected = new TextChange.Builder().from(new TextPosition(1, 2))
                                                            .to(new TextPosition(3, 3))
                                                            .insert("\n * \n */")
                                                            .build();
        Assert.assertEquals(expected, output);
    }

    @Ignore
    @Test
    public void testFirstLineNoLeadingSpaces() {
        doReturn("/*").when(document).getLineContent(0);
        doReturn(" *").when(document).getLineContent(1);
        final  TextChange input = new TextChange.Builder().from(new TextPosition(0, 2))
                                                          .to(new TextPosition(1, 2))
                                                          .insert("\n *")
                                                          .build();
        final TextChange output = interceptor.processChange(input, document);
        assertNotNull(output);
        final TextChange expected = new TextChange.Builder().from(new TextPosition(0, 2))
                                                            .to(new TextPosition(2, 3))
                                                            .insert("\n * \n */")
                                                            .build();
        Assert.assertEquals(expected, output);
    }

    @Test
    public void testStartNotEmptyLine() {
        doReturn("whatever").when(document).getLineContent(0);
        doReturn("s/*").when(document).getLineContent(1);
        doReturn(" *").when(document).getLineContent(2);
        final  TextChange input = new TextChange.Builder().from(new TextPosition(1, 3))
                                                          .to(new TextPosition(2, 2))
                                                          .insert("\n *")
                                                          .build();
        final TextChange output = interceptor.processChange(input, document);
        assertNull(output);
    }

    @Ignore
    @Test
    public void test3LeadingSpaces() {
        testWithLeading("   ");
    }

    @Ignore
    @Test
    public void testLeadingTab() {
        testWithLeading("\t");
    }

    @Ignore
    @Test
    public void testLeadingMixed() {
        testWithLeading(" \t");
    }

    private void testWithLeading(final String lead) {
        doReturn(lead + "/*").when(document).getLineContent(1);
        doReturn(lead + " *").when(document).getLineContent(2);
        final  TextChange input = new TextChange.Builder().from(new TextPosition(1, 2 + lead.length()))
                                                          .to(new TextPosition(2, 2 + lead.length()))
                                                          .insert("\n" + lead + " *")
                                                          .build();
        final TextChange output = interceptor.processChange(input, document);
        assertNotNull(output);
        final TextChange expected = new TextChange.Builder().from(new TextPosition(1, 2 + lead.length()))
                                                            .to(new TextPosition(3, 3  + lead.length()))
                                                            .insert("\n" + lead + " * "+"\n" + lead + " */")
                                                            .build();
        Assert.assertEquals(expected, output);
    }

    @Ignore
    @Test
    public void testAddWithComment() {
        doReturn("/*").when(document).getLineContent(0);
        doReturn("/*").when(document).getLineContent(1);
        doReturn(" *").when(document).getLineContent(2);
        final  TextChange input = new TextChange.Builder().from(new TextPosition(1, 2))
                                                          .to(new TextPosition(2, 2))
                                                          .insert("\n *")
                                                          .build();
        final TextChange output = interceptor.processChange(input, document);
        assertNull(output);
    }

    @Ignore
    @Test
    public void testJavadocStyleComment() {
        doReturn("/**").when(document).getLineContent(0);
        doReturn(" *").when(document).getLineContent(1);
        final  TextChange input = new TextChange.Builder().from(new TextPosition(0, 3))
                                                          .to(new TextPosition(1, 2))
                                                          .insert("\n *")
                                                          .build();
        final TextChange output = interceptor.processChange(input, document);
        assertNotNull(output);
        final TextChange expected = new TextChange.Builder().from(new TextPosition(0, 3))
                                                            .to(new TextPosition(2, 3))
                                                            .insert("\n * \n */")
                                                            .build();
        Assert.assertEquals(expected, output);
    }

    @Test
    public void testPasteWholeCommentStart() {
        doReturn("/**").when(document).getLineContent(0);
        doReturn(" *").when(document).getLineContent(1);
        final  TextChange input = new TextChange.Builder().from(new TextPosition(0, 0))
                                                          .to(new TextPosition(1, 2))
                                                          .insert("/**\n *")
                                                          .build();
        final TextChange output = interceptor.processChange(input, document);
        assertNull(output);
    }

    @Test
    public void testCloseComment() {
        doReturn("/**").when(document).getLineContent(0);
        doReturn(" *").when(document).getLineContent(1);
        final  TextChange input = new TextChange.Builder().from(new TextPosition(0, 0))
                                                          .to(new TextPosition(1, 2))
                                                          .insert("/**\n *")
                                                          .build();
        final TextChange output = interceptor.processChange(input, document);
        assertNull(output);
    }

}
