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
package org.eclipse.che.api.factory;

import org.eclipse.che.api.account.server.dao.AccountDao;
import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.factory.dto.Factory;
import org.eclipse.che.api.user.server.dao.PreferenceDao;
import org.eclipse.che.api.user.server.dao.UserDao;

import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link FactoryAcceptValidatorImpl} and {@link FactoryCreateValidatorImpl}
 */
@Listeners(value = {MockitoTestNGListener.class})
public class FactoryCreateAndAcceptValidatorsImplsTest {

    @Mock
    private AccountDao accountDao;

    @Mock
    private UserDao userDao;

    @Mock
    private PreferenceDao preferenceDao;

    @Mock
    private Factory factoryUrl;

    private FactoryAcceptValidatorImpl acceptValidator;

    private FactoryCreateValidatorImpl createValidator;

    @BeforeMethod
    public void setUp() throws Exception {

        acceptValidator = new FactoryAcceptValidatorImpl(accountDao, userDao, preferenceDao);
        createValidator = new FactoryCreateValidatorImpl(accountDao, userDao, preferenceDao);
    }

    @Test
    public void testValidateOnCreate() throws ApiException {
        FactoryCreateValidatorImpl spy = spy(createValidator);
        doNothing().when(spy).validateSource(any(Factory.class));
        doNothing().when(spy).validateAccountId(any(Factory.class));
        doNothing().when(spy).validateProjectName(any(Factory.class));
        doNothing().when(spy).validateModules(any(Factory.class));
        doNothing().when(spy).validateCurrentTimeBeforeSinceUntil(any(Factory.class));
        doNothing().when(spy).validateProjectActions(any(Factory.class));
        doNothing().when(spy).validateWorkspace(any(Factory.class));
        doNothing().when(spy).validateCreator(any(Factory.class));
        doNothing().when(spy).validateProjectRunnerNames(any(Factory.class));

        //main invoke
        spy.validateOnCreate(factoryUrl);

        verify(spy).validateSource(any(Factory.class));
        verify(spy).validateAccountId(any(Factory.class));
        verify(spy).validateProjectName(any(Factory.class));
        verify(spy).validateModules(any(Factory.class));
        verify(spy).validateCurrentTimeBeforeSinceUntil(any(Factory.class));
        verify(spy).validateOnCreate(any(Factory.class));
        verify(spy).validateProjectActions(any(Factory.class));
        verify(spy).validateCreator(any(Factory.class));
        verify(spy).validateWorkspace(any(Factory.class));
        verify(spy).validateProjectRunnerNames(any(Factory.class));
        verifyNoMoreInteractions(spy);
    }

    @Test
    public void testOnAcceptNonEncoded() throws ApiException {
        FactoryAcceptValidatorImpl spy = spy(acceptValidator);
        doNothing().when(spy).validateSource(any(Factory.class));
        doNothing().when(spy).validateProjectName(any(Factory.class));
        doNothing().when(spy).validateModules(any(Factory.class));
        doNothing().when(spy).validateCurrentTimeBetweenSinceUntil(any(Factory.class));
        doNothing().when(spy).validateProjectActions(any(Factory.class));
        doNothing().when(spy).validateWorkspace(any(Factory.class));
        doNothing().when(spy).validateCreator(any(Factory.class));

        //main invoke
        spy.validateOnAccept(factoryUrl, false);

        verify(spy).validateSource(any(Factory.class));
        verify(spy).validateProjectName(any(Factory.class));
        verify(spy).validateCurrentTimeBetweenSinceUntil(any(Factory.class));
        verify(spy).validateOnAccept(any(Factory.class), eq(false));
        verify(spy).validateModules(any(Factory.class));
        verify(spy).validateProjectActions(any(Factory.class));
        verify(spy).validateWorkspace(any(Factory.class));
        verify(spy).validateCreator(any(Factory.class));
        verifyNoMoreInteractions(spy);
    }

    @Test
    public void testOnAcceptEncoded() throws ApiException {
        FactoryAcceptValidatorImpl spy = spy(acceptValidator);
        doNothing().when(spy).validateCurrentTimeBetweenSinceUntil(any(Factory.class));
        doNothing().when(spy).validateProjectActions(any(Factory.class));
        doNothing().when(spy).validateModules(any(Factory.class));
        doNothing().when(spy).validateWorkspace(any(Factory.class));
        doNothing().when(spy).validateCreator(any(Factory.class));

        //main invoke
        spy.validateOnAccept(factoryUrl, true);

        verify(spy).validateCurrentTimeBetweenSinceUntil(any(Factory.class));
        verify(spy).validateOnAccept(any(Factory.class), eq(true));
        verify(spy).validateModules(any(Factory.class));
        verify(spy).validateProjectActions(any(Factory.class));
        verify(spy).validateWorkspace(any(Factory.class));
        verify(spy).validateCreator(any(Factory.class));
        verifyNoMoreInteractions(spy);
    }



}
