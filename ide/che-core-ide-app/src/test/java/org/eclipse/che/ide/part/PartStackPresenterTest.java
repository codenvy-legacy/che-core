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
package org.eclipse.che.ide.part;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.ide.api.constraints.Anchor;
import org.eclipse.che.ide.api.constraints.Constraints;
import org.eclipse.che.ide.api.parts.PartPresenter;
import org.eclipse.che.ide.api.parts.PartStackView;
import org.eclipse.che.ide.api.parts.PropertyListener;
import org.eclipse.che.ide.api.parts.base.BasePresenter;
import org.eclipse.che.ide.workspace.WorkBenchPartController;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.vectomatic.dom.svg.ui.SVGImage;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testing {@link PartStackPresenter} functionality.
 *
 * @author Roman Nikitenko
 */
@RunWith(MockitoJUnitRunner.class)
public class PartStackPresenterTest {
    private static final String BUILDER_TITLE = "builder";
    private static final String RUNNER_TITLE  = "runner";

    @Captor
    private ArgumentCaptor<AsyncCallback<Void>> asyncCallbackCaptor;

    @Mock
    PartStackView                            view;
    @Mock
    EventBus                                 eventBus;
    @Mock
    PartStackPresenter.PartStackEventHandler partStackHandler;
    @Mock
    WorkBenchPartController                  workBenchPartController;
    @InjectMocks
    PartStackPresenter                       presenter;

    PartPresenter part         = mock(PartPresenter.class);
    PartPresenter first        = mock(PartPresenter.class);
    PartPresenter last         = mock(PartPresenter.class);
    PartPresenter before       = mock(PartPresenter.class);
    PartPresenter after        = mock(PartPresenter.class);
    PartPresenter beforeBefore = mock(PartPresenter.class);
    PartPresenter afterAfter   = mock(PartPresenter.class);

    PartPresenter withoutConstr      = mock(PartPresenter.class);
    Constraints   firstConstr        = new Constraints(Anchor.FIRST, null);
    Constraints   lastConstr         = new Constraints(Anchor.LAST, null);
    Constraints   beforeConstr       = new Constraints(Anchor.BEFORE, RUNNER_TITLE);
    Constraints   afterConstr        = new Constraints(Anchor.AFTER, BUILDER_TITLE);
    Constraints   beforeBeforeConstr = new Constraints(Anchor.BEFORE, BUILDER_TITLE);
    Constraints   afterAfterConstr   = new Constraints(Anchor.AFTER, RUNNER_TITLE);

    @Before
    public void setUp() {
        when(before.getTitle()).thenReturn(BUILDER_TITLE);
        when(after.getTitle()).thenReturn(RUNNER_TITLE);
    }

    @Test
    public void testGo() throws Exception {
        AcceptsOneWidget container = mock(AcceptsOneWidget.class);

        presenter.go(container);

        verify(container).setWidget(eq(view));
        verify(view, never()).setActiveTab(anyInt());
    }

    @Test
    public void testGoWhenActivePartExists() throws Exception {
        AcceptsOneWidget container = mock(AcceptsOneWidget.class);
        presenter.setActivePart(mock(PartPresenter.class));

        presenter.go(container);

        verify(container).setWidget(eq(view));
        verify(view, times(2)).setActiveTab(anyInt());
    }

    @Test
    public void testAddPartWhenPartAlreadyExists() throws Exception {
        presenter.parts.add(part);

        presenter.addPart(part);

        verify(view).setActiveTab(anyInt());
        verify(part, never()).addPropertyListener((PropertyListener)anyObject());
        verify(part, never()).go((AcceptsOneWidget)anyObject());
        verify(part, never()).onOpen();
        verify(view, never()).addTab((SVGImage)anyObject(), anyString(), anyString(), (IsWidget)anyObject(), anyBoolean());
    }

    @Test
    public void testAddPartWhenPartInstanceOfBasePresenter() throws Exception {
        BasePresenter part = mock(BasePresenter.class);
        when(view.addTab((SVGImage)anyObject(), anyString(), anyString(), (IsWidget)anyObject(), anyBoolean()))
                .thenReturn(mock(PartStackView.TabItem.class));

        presenter.addPart(part);

        verify(part).setPartStack(eq(presenter));
        assertThat(presenter.parts).hasSize(1).containsExactly(part);
        assertThat(presenter.constraints).hasSize(1);
    }

    @Test
    public void testAddPart() throws Exception {
        when(view.addTab((SVGImage)anyObject(), anyString(), anyString(), (IsWidget)anyObject(), anyBoolean()))
                .thenReturn(mock(PartStackView.TabItem.class));

        presenter.addPart(part);

        assertThat(presenter.parts).hasSize(1).containsExactly(part);
        assertThat(presenter.constraints).hasSize(1);
        verify(part).addPropertyListener(eq(presenter.propertyListener));
        verify(part).getTitleSVGImage();
        verify(view).addTab((SVGImage)anyObject(), anyString(), anyString(), (IsWidget)anyObject(), anyBoolean());
        verify(part).go((AcceptsOneWidget)anyObject());
        verify(part).onOpen();
    }

    @Test
    public void testSetActivePartWhenPartIsNull() throws Exception {
        presenter.setActivePart(null);

        verify(view).setActiveTab(eq(-1));
        verify(workBenchPartController).setHidden(eq(true));
    }

    @Test
    public void testSetActivePartWhenPartIsNotNull() throws Exception {
        presenter.parts.add(part);
        presenter.setActivePart(part);

        verify(view, never()).setActiveTab(eq(-1));
        verify(workBenchPartController, never()).setHidden(eq(true));
        verify(workBenchPartController).setHidden(eq(false));
        verify(workBenchPartController).setSize(anyDouble());
        verify(partStackHandler).onRequestFocus(eq(presenter));
    }

    @Test
    public void testHidePart() {
        reset(view);
        presenter.activePart = part;

        presenter.hidePart(part);

        verify(view).setActiveTab(eq(-1));
        Assert.assertNull(presenter.activePart);
    }

    @Test
    public void testRemovePartWhenSuccess() {
        presenter.parts.add(part);

        presenter.removePart(part);

        verify(part).onClose(asyncCallbackCaptor.capture());
        AsyncCallback<Void> asyncCallback = asyncCallbackCaptor.getValue();
        asyncCallback.onSuccess(null);

        verify(view).removeTab(eq(0));
        assertThat(presenter.parts).isEmpty();
        assertThat(presenter.constraints).isEmpty();
        verify(part).removePropertyListener((PropertyListener)anyObject());
    }

    @Test
    public void testRemoveActivePart() {
        presenter.parts.add(part);
        presenter.activePart = part;

        presenter.removePart(part);

        verify(part).onClose(asyncCallbackCaptor.capture());
        AsyncCallback<Void> asyncCallback = asyncCallbackCaptor.getValue();
        asyncCallback.onSuccess(null);

        verify(view).setActiveTab(eq(-1));
        verify(view).removeTab(eq(0));
        assertThat(presenter.parts).isEmpty();
        assertThat(presenter.constraints).isEmpty();
        verify(part).removePropertyListener((PropertyListener)anyObject());
    }

    @Test
    public void testSortPartsWhenFirstAndLastAdded() {
        when(view.addTab((SVGImage)anyObject(), anyString(), anyString(), (IsWidget)anyObject(), anyBoolean()))
                .thenReturn(mock(PartStackView.TabItem.class));

        presenter.addPart(last, lastConstr);
        presenter.addPart(first, firstConstr);

        assertThat(presenter.getSortedParts()).hasSize(2).containsExactly(first, last);
        verify(view, times(2)).setTabpositions((List<Integer>)anyObject());
    }

    @Test
    public void testSortPartsWhenFirstAndWithoutConstrAdded() {
        when(view.addTab((SVGImage)anyObject(), anyString(), anyString(), (IsWidget)anyObject(), anyBoolean()))
                .thenReturn(mock(PartStackView.TabItem.class));

        presenter.addPart(withoutConstr, null);
        presenter.addPart(first, firstConstr);


        assertThat(presenter.getSortedParts()).hasSize(2).containsExactly(first, withoutConstr);
        verify(view, times(2)).setTabpositions((List<Integer>)anyObject());
    }

    @Test
    public void testSortPartsWhenLastAndWithoutConstrAdded() {
        when(view.addTab((SVGImage)anyObject(), anyString(), anyString(), (IsWidget)anyObject(), anyBoolean()))
                .thenReturn(mock(PartStackView.TabItem.class));

        presenter.addPart(last, lastConstr);
        presenter.addPart(withoutConstr, null);

        assertThat(presenter.getSortedParts()).hasSize(2).containsExactly(withoutConstr, last);
        verify(view, times(2)).setTabpositions((List<Integer>)anyObject());
    }

    @Test
    public void testSortPartsWhenFirstLastAndWithoutConstrAdded() {
        when(view.addTab((SVGImage)anyObject(), anyString(), anyString(), (IsWidget)anyObject(), anyBoolean()))
                .thenReturn(mock(PartStackView.TabItem.class));

        presenter.addPart(last, lastConstr);
        presenter.addPart(withoutConstr, null);
        presenter.addPart(first, firstConstr);

        assertThat(presenter.getSortedParts()).hasSize(3).containsExactly(first, withoutConstr, last);
        verify(view, times(3)).setTabpositions((List<Integer>)anyObject());
    }

    @Test
    public void testSortPartsWhenBeforeAndAfterAdded() {
        when(view.addTab((SVGImage)anyObject(), anyString(), anyString(), (IsWidget)anyObject(), anyBoolean()))
                .thenReturn(mock(PartStackView.TabItem.class));

        presenter.addPart(withoutConstr, null);
        presenter.addPart(after, afterConstr);
        presenter.addPart(before, beforeConstr);

        assertThat(presenter.getSortedParts()).hasSize(3).containsExactly(withoutConstr, before, after);
        verify(view, times(3)).setTabpositions((List<Integer>)anyObject());
    }

    @Test
    public void testSortPartsWhen2BeforeAdded() {
        when(view.addTab((SVGImage)anyObject(), anyString(), anyString(), (IsWidget)anyObject(), anyBoolean()))
                .thenReturn(mock(PartStackView.TabItem.class));

        presenter.addPart(after, afterConstr);
        presenter.addPart(last, lastConstr);
        presenter.addPart(withoutConstr, null);
        presenter.addPart(first, firstConstr);
        presenter.addPart(before, beforeConstr);
        presenter.addPart(beforeBefore, beforeBeforeConstr);

        assertThat(presenter.getSortedParts()).hasSize(6).containsExactly(first, withoutConstr, beforeBefore, before, after, last);
        verify(view, times(6)).setTabpositions((List<Integer>)anyObject());
    }

    @Test
    public void testSortPartsWhen2AfterAdded() {
        when(view.addTab((SVGImage)anyObject(), anyString(), anyString(), (IsWidget)anyObject(), anyBoolean()))
                .thenReturn(mock(PartStackView.TabItem.class));

        presenter.addPart(after, afterConstr);
        presenter.addPart(last, lastConstr);
        presenter.addPart(withoutConstr, null);
        presenter.addPart(first, firstConstr);
        presenter.addPart(before, beforeConstr);
        presenter.addPart(afterAfter, afterAfterConstr);

        assertThat(presenter.getSortedParts()).hasSize(6).containsExactly(first, withoutConstr, before, after, afterAfter, last);
        verify(view, times(6)).setTabpositions((List<Integer>)anyObject());
    }

    @Test
    public void testSortPartsWhenAllKindConstrAdded() {
        when(view.addTab((SVGImage)anyObject(), anyString(), anyString(), (IsWidget)anyObject(), anyBoolean()))
                .thenReturn(mock(PartStackView.TabItem.class));

        presenter.addPart(beforeBefore, beforeBeforeConstr);
        presenter.addPart(last, lastConstr);
        presenter.addPart(withoutConstr, null);
        presenter.addPart(afterAfter, afterAfterConstr);
        presenter.addPart(after, afterConstr);
        presenter.addPart(first, firstConstr);
        presenter.addPart(before, beforeConstr);

        assertThat(presenter.getSortedParts()).hasSize(7).containsExactly(first, withoutConstr, beforeBefore, before, after, afterAfter, last);
        verify(view, times(7)).setTabpositions((List<Integer>)anyObject());
    }
}
