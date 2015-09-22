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
package org.eclipse.che.ide.createworkspace;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwtmockito.GwtMockitoTestRunner;

import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.machine.shared.dto.recipe.RecipeDescriptor;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.createworkspace.CreateWorkSpaceView.ActionDelegate;
import org.eclipse.che.ide.createworkspace.CreateWorkSpaceView.HidePopupCallBack;
import org.eclipse.che.ide.createworkspace.tagentry.TagEntry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

import static org.eclipse.che.ide.createworkspace.CreateWorkspaceViewImpl.RECIPE_URL;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dmitry Shnurenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class CreateWorkspaceViewImplTest {

    //constructor mocks
    @Mock
    private CoreLocalizationConstant      locale;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private org.eclipse.che.ide.Resources resources;
    @Mock
    private FlowPanel                     tagsPanel;
    @Mock
    private TagEntryFactory               tagFactory;
    @Mock
    private PopupPanel                    popupPanel;

    //additional mocks
    @Mock
    private RecipeDescriptor descriptor;
    @Mock
    private TagEntry         tag;
    @Mock
    private ActionDelegate   delegate;
    @Mock
    private KeyUpEvent       keyUpEvent;

    @InjectMocks
    private CreateWorkspaceViewImpl view;

    @Before
    public void setUp() {
        view.setDelegate(delegate);
    }

    @Test
    public void popupPanelShouldBeSettingUp() {
        verify(popupPanel).setStyleName(anyString());
        verify(resources.coreCss()).createWsTagsPopup();
        verify(popupPanel).addDomHandler(Matchers.<ClickHandler>anyObject(), eq(ClickEvent.getType()));
    }

    @Test
    public void recipeUrlAndNameShouldBeSet() {
        verify(view.recipeURL).setText(RECIPE_URL);
        verify(view.wsName).setText(anyString());
        verify(locale).createWsDefaultName();
    }

    @Test
    public void workspaceNameShouldBeSet() {
        view.setWorkspaceName("test");

        verify(view.wsName).setText("test");
    }

    @Test
    public void recipeUrlShouldBeReturned() {
        view.getRecipeUrl();

        verify(view.recipeURL).getText();
    }

    @Test
    public void tagsShouldBeReturned() {
        when(view.tags.getValue()).thenReturn("test test ");

        List<String> tags = view.getTags();

        assertThat("test", is(equalTo(tags.get(0))));
    }

    @Test
    public void workspaceNameShouldBeReturned() {
        view.getWorkspaceName();

        verify(view.wsName).getText();
    }

    @Test
    public void recipesShouldBeShown() {
        when(tagFactory.create(descriptor)).thenReturn(tag);

        view.showRecipes(Arrays.asList(descriptor));

        verify(tagsPanel).clear();

        verify(tagFactory).create(descriptor);

        verify(tag).setDelegate(view);
        verify(tag).setStyles();

        verify(view.tags).getAbsoluteLeft();
        verify(view.tags).getAbsoluteTop();
        verify(view.tags).getOffsetHeight();

        verify(popupPanel).setWidget(tagsPanel);
        verify(popupPanel).setPopupPosition(anyInt(), anyInt());
        verify(popupPanel).show();
    }

    @Test
    public void tagShouldBeSelected() {
        Link link = mock(Link.class);
        when(tag.getDescriptor()).thenReturn(descriptor);
        when(descriptor.getLink(anyString())).thenReturn(link);

        view.onTagClicked(tag);

        verify(tag).getDescriptor();
        verify(descriptor).getLink("get recipe script");
        verify(link).getHref();

        verify(view.recipeURL).setValue(anyString());
        verify(view.recipeURL).setTitle(anyString());

        verify(delegate).onRecipeUrlChanged();
    }

    @Test
    public void urlErrorVisibilityShouldBeChanged() {
        view.setVisibleUrlError(true);

        verify(view.recipeUrlError).setVisible(true);
    }

    @Test
    public void tagsErrorVisibilityShouldBeChanged() {
        view.setVisibleTagsError(true);

        verify(view.tagsError).setVisible(true);
    }

    @Test
    public void nameErrorVisibilityShouldBeChanged() {
        view.setVisibleNameError(true);

        verify(view.nameError).setVisible(true);
    }

    @Test
    public void tagsShouldBeChanged() {
        when(view.tags.getText()).thenReturn("test");

        view.onTagsChanged(keyUpEvent);

        verify(view.tags).getText();

        verify(view.tagsError).setVisible(true);

        verify(delegate).onTagsChanged(Matchers.<HidePopupCallBack>anyObject());
    }

    @Test
    public void recipeUrlShouldBeChanged() {
        view.onRecipeUrlChanged(keyUpEvent);

        verify(delegate).onRecipeUrlChanged();
    }

    @Test
    public void workspaceNameShouldBeChanged() {
        view.onWorkspaceNameChanged(keyUpEvent);

        verify(view.wsName).getText();
        verify(delegate).onNameChanged(anyString());
    }

}