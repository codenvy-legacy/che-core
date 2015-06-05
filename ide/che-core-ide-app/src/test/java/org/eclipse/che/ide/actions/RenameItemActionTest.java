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
package org.eclipse.che.ide.actions;

import org.eclipse.che.api.runner.dto.ApplicationProcessDescriptor;
import org.eclipse.che.api.runner.gwt.client.RunnerServiceClient;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.Resources;
import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.api.project.tree.generic.StorableNode;
import org.eclipse.che.ide.api.selection.Selection;
import org.eclipse.che.ide.api.selection.SelectionAgent;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.part.projectexplorer.ProjectListStructure;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;
import org.eclipse.che.ide.ui.dialogs.CancelCallback;
import org.eclipse.che.ide.ui.dialogs.ConfirmCallback;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.ui.dialogs.InputCallback;
import org.eclipse.che.ide.ui.dialogs.input.InputDialog;
import org.eclipse.che.ide.ui.dialogs.input.InputValidator;
import org.eclipse.che.ide.ui.dialogs.message.MessageDialog;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Method;
import java.util.Iterator;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.eclipse.che.api.runner.ApplicationStatus.NEW;
import static org.eclipse.che.api.runner.ApplicationStatus.RUNNING;

/**
 *
 * @author Andrienko Alexander
 */
@RunWith(MockitoJUnitRunner.class)
public class RenameItemActionTest {

    private static final String TEXT          = "some text";
    private static final String STOP_RUN      = "stop running process";

    //mocks for constructor
    @Mock
    private Resources                resources;
    @Mock
    private AnalyticsEventLogger     eventLogger;
    @Mock
    private SelectionAgent           selectionAgent;
    @Mock
    private CoreLocalizationConstant localization;
    @Mock
    private RunnerServiceClient      runnerServiceClient;
    @Mock
    private DtoUnmarshallerFactory   dtoUnmarshallerFactory;
    @Mock
    private DialogFactory            dialogFactory;
    @Mock
    private AppContext               appContext;

    @Mock
    private Selection<StorableNode>                             selection;
    @Mock
    private StorableNode                                        selectedNode;
    @Mock
    private TreeNode                                            selectedParent;
    @Mock
    private TreeNode                                            treeNode;
    @Mock
    private ActionEvent                                         e;
    @Mock
    private MessageDialog                                       messageDialog;
    @Mock
    private Unmarshallable<Array<ApplicationProcessDescriptor>> unmarshaller;
    @Mock
    private Array<ApplicationProcessDescriptor>                 descriptors;
    @Mock
    private Iterable<ApplicationProcessDescriptor>              iterable;
    @Mock
    private Iterator<ApplicationProcessDescriptor>              iterator;
    @Mock
    private ApplicationProcessDescriptor                        applicationProcessDescriptor;
    @Mock
    private ProjectListStructure.ProjectNode                    projectNode;
    @Mock
    private InputDialog                                         inputDialog;

    @Captor
    private ArgumentCaptor<InputCallback>                                             inputCallbackArgCaptor;
    @Captor
    private ArgumentCaptor<AsyncRequestCallback<Array<ApplicationProcessDescriptor>>> processDescriptorArgCaptor;

    @InjectMocks
    private RenameItemAction renameItemAction;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        doReturn(selection).when(selectionAgent).getSelection();

        when(localization.closeProjectBeforeRenaming()).thenReturn(TEXT);
        when(localization.stopProcessesBeforeRenamingProject()).thenReturn(STOP_RUN);
        when(localization.renameNodeDialogTitle()).thenReturn(TEXT);
        when(localization.renameProjectDialogTitle()).thenReturn(TEXT);
        when(localization.renameDialogNewNameLabel()).thenReturn(TEXT);

        when(dialogFactory.createMessageDialog(eq(""), eq(TEXT), isNull(ConfirmCallback.class))).thenReturn(messageDialog);
        when(dialogFactory.createMessageDialog("", STOP_RUN, null)).thenReturn(messageDialog);
        when(dialogFactory.createInputDialog(eq(TEXT),
                                             eq(TEXT),
                                             eq(TEXT),
                                             eq(0),
                                             eq(TEXT.length()),
                                             inputCallbackArgCaptor.capture(),
                                             isNull(CancelCallback.class))).thenReturn(inputDialog);

        when(dtoUnmarshallerFactory.newArrayUnmarshaller(ApplicationProcessDescriptor.class)).thenReturn(unmarshaller);

        when(selection.getHeadElement()).thenReturn(projectNode);
        when(projectNode.getParent()).thenReturn(treeNode);
        when(projectNode.getPath()).thenReturn(TEXT);
        when(projectNode.getName()).thenReturn(TEXT);

        when(descriptors.asIterable()).thenReturn(iterable);

        when(iterable.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(true).thenReturn(false);
        when(iterator.next()).thenReturn(applicationProcessDescriptor);
    }

    @Test
    public void actionShouldNotBePerformedBecauseSelectionIsNull() {
        when(selectionAgent.getSelection()).thenReturn(null);
        verify(localization).renameItemActionText();
        verify(localization).renameItemActionDescription();
        verify(resources).rename();

        renameItemAction.actionPerformed(e);

        verify(eventLogger).log(renameItemAction);
        verify(selectionAgent).getSelection();
        verifyNoMoreInteractions(resources,
                                 localization,
                                 runnerServiceClient,
                                 dtoUnmarshallerFactory,
                                 dialogFactory,
                                 appContext);
    }

    @Test
    public void constructorShouldBeInitialized() {
        verify(localization).renameItemActionText();
        verify(localization).renameItemActionDescription();
        verify(resources).rename();
    }

    @Test
    public void actionShouldNotBePerformedBecauseSelectedNodeIsNull() {
        when(selection.getHeadElement()).thenReturn(null);
        verify(localization).renameItemActionText();
        verify(localization).renameItemActionDescription();
        verify(resources).rename();

        renameItemAction.actionPerformed(e);

        verify(eventLogger).log(renameItemAction);
        verify(selectionAgent).getSelection();
        verify(selection).getHeadElement();
        verifyNoMoreInteractions(resources,
                                 localization,
                                 runnerServiceClient,
                                 dtoUnmarshallerFactory,
                                 dialogFactory,
                                 appContext);
    }

    @Test
    public void shouldShowMessageCloseProjectBeforeRenaming() {
        when(selection.getHeadElement()).thenReturn(selectedNode);
        when(selectedNode.getParent()).thenReturn(selectedParent);

        renameItemAction.actionPerformed(e);

        verify(eventLogger).log(renameItemAction);
        verify(selectionAgent).getSelection();
        verify(selection).getHeadElement();
        verify(selectedParent).getParent();
        verify(localization).closeProjectBeforeRenaming();
        verify(dialogFactory).createMessageDialog(eq(""), eq(TEXT), isNull(ConfirmCallback.class));
        verify(messageDialog).show();
    }

    @Test
    public void shouldShowDialogStopRunningProcessWhenWeHaveNewRunProcess() throws Exception {
        when(applicationProcessDescriptor.getStatus()).thenReturn(NEW);

        renameItemAction.actionPerformed(e);

        verify(eventLogger).log(renameItemAction);
        verify(selectionAgent).getSelection();
        verify(selection).getHeadElement();
        verify(projectNode).getParent();

        verify(dtoUnmarshallerFactory).newArrayUnmarshaller(ApplicationProcessDescriptor.class);
        verify(runnerServiceClient).getRunningProcesses(eq(TEXT), processDescriptorArgCaptor.capture());
        AsyncRequestCallback<Array<ApplicationProcessDescriptor>> callback = processDescriptorArgCaptor.getValue();
        Method method = callback.getClass().getDeclaredMethod("onSuccess", Object.class);
        method.setAccessible(true);
        method.invoke(callback, descriptors);

        verify(descriptors).asIterable();
        verify(applicationProcessDescriptor).getStatus();
        verify(localization).stopProcessesBeforeRenamingProject();
        verify(dialogFactory).createMessageDialog("", STOP_RUN, null);
        verify(messageDialog).show();
    }

    @Test
    public void shouldShowDialogStopRunningProcessWhenWeHaveRunningProcess() throws Exception {
        when(applicationProcessDescriptor.getStatus()).thenReturn(RUNNING);

        renameItemAction.actionPerformed(e);

        verify(eventLogger).log(renameItemAction);
        verify(selectionAgent).getSelection();
        verify(selection).getHeadElement();
        verify(projectNode).getParent();

        verify(dtoUnmarshallerFactory).newArrayUnmarshaller(ApplicationProcessDescriptor.class);
        verify(runnerServiceClient).getRunningProcesses(eq(TEXT), processDescriptorArgCaptor.capture());
        AsyncRequestCallback<Array<ApplicationProcessDescriptor>> callback = processDescriptorArgCaptor.getValue();
        Method method = callback.getClass().getDeclaredMethod("onSuccess", Object.class);
        method.setAccessible(true);
        method.invoke(callback, descriptors);

        verify(descriptors).asIterable();
        verify(applicationProcessDescriptor, times(2)).getStatus();
        verify(localization).stopProcessesBeforeRenamingProject();
        verify(dialogFactory).createMessageDialog("", STOP_RUN, null);
        verify(messageDialog).show();
    }

    @Test
    public void shouldShowAskForRenameDialog() throws Exception {
        renameItemAction.actionPerformed(e);

        verify(eventLogger).log(renameItemAction);
        verify(selectionAgent).getSelection();
        verify(selection).getHeadElement();
        verify(projectNode).getParent();

        verify(dtoUnmarshallerFactory).newArrayUnmarshaller(ApplicationProcessDescriptor.class);
        verify(runnerServiceClient).getRunningProcesses(eq(TEXT), processDescriptorArgCaptor.capture());
        AsyncRequestCallback<Array<ApplicationProcessDescriptor>> callback = processDescriptorArgCaptor.getValue();
        Method method = callback.getClass().getDeclaredMethod("onSuccess", Object.class);
        method.setAccessible(true);
        method.invoke(callback, descriptors);

        verify(descriptors).asIterable();
        verify(applicationProcessDescriptor, times(2)).getStatus();

        verify(projectNode, times(3)).getName();
        verify(localization).renameProjectDialogTitle();
        verify(dialogFactory).createInputDialog(eq(TEXT),
                                                eq(TEXT),
                                                eq(TEXT),
                                                eq(0),
                                                eq(TEXT.length()),
                                                inputCallbackArgCaptor.capture(),
                                                isNull(CancelCallback.class));
        verify(inputDialog).withValidator(any(InputValidator.class));
        verify(inputDialog).show();
    }

}
