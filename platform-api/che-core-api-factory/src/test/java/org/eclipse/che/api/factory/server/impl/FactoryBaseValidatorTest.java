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
package org.eclipse.che.api.factory.server.impl;

import com.google.common.collect.ImmutableMap;

import org.eclipse.che.api.account.server.dao.AccountDao;
import org.eclipse.che.api.account.server.dao.Member;
import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.factory.server.FactoryConstants;
import org.eclipse.che.api.factory.server.builder.FactoryBuilder;
import org.eclipse.che.api.factory.shared.dto.Action;
import org.eclipse.che.api.factory.shared.dto.Author;
import org.eclipse.che.api.factory.shared.dto.Factory;
import org.eclipse.che.api.factory.shared.dto.Ide;
import org.eclipse.che.api.factory.shared.dto.OnAppClosed;
import org.eclipse.che.api.factory.shared.dto.OnAppLoaded;
import org.eclipse.che.api.factory.shared.dto.OnProjectOpened;
import org.eclipse.che.api.factory.shared.dto.Policies;
import org.eclipse.che.api.user.server.dao.PreferenceDao;
import org.eclipse.che.api.user.server.dao.User;
import org.eclipse.che.api.user.server.dao.UserDao;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.api.workspace.shared.dto.SourceStorageDto;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceConfigDto;
import org.eclipse.che.dto.server.DtoFactory;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@Listeners(value = {MockitoTestNGListener.class})
public class FactoryBaseValidatorTest {
    private static final String VALID_REPOSITORY_URL = "http://github.com/codenvy/cloudide";
    private static final String ID                   = "id";

    @Mock
    private AccountDao accountDao;

    @Mock
    private UserDao userDao;

    @Mock
    private PreferenceDao preferenceDao;

    @Mock
    private FactoryBuilder builder;

    @Mock
    private HttpServletRequest request;

    private TesterFactoryBaseValidator validator;

    private Member member;

    private Factory factory;

    private DtoFactory dto = DtoFactory.getInstance();


    @BeforeMethod
    public void setUp() throws ParseException, NotFoundException, ServerException {
        factory = dto.createDto(Factory.class)
                     .withV("4.0")
                     .withCreator(dto.createDto(Author.class)
                                     .withAccountId(ID)
                                     .withUserId("userid"));

        User user = new User().withId("userid");

        member = new Member().withUserId("userid")
                             .withRoles(Arrays.asList("account/owner"));
        when(accountDao.getMembers(anyString())).thenReturn(Arrays.asList(member));
        when(userDao.getById("userid")).thenReturn(user);


        validator = new TesterFactoryBaseValidator(accountDao, preferenceDao);
    }

    @Test
    public void shouldBeAbleToValidateFactoryUrlObject() throws ApiException {
        factory = prepareFactoryWithGivenStorage("git", VALID_REPOSITORY_URL);
        validator.validateSource(factory);
        validator.validateProjectNames(factory);
        validator.validateAccountId(factory);
    }

    @Test
    public void shouldBeAbleToValidateFactoryUrlObjectIfStorageIsESBWSO2() throws ApiException {
        factory = prepareFactoryWithGivenStorage("esbwso2", VALID_REPOSITORY_URL);
        validator.validateSource(factory);
        validator.validateProjectNames(factory);
        validator.validateAccountId(factory);
    }

    @Test(expectedExceptions = ApiException.class,
          expectedExceptionsMessageRegExp =
                  "The parameter project.storage.location has a value submitted http://codenvy.com/git/04%2 with a value that is " +
                  "unexpected. " +
                  "For more information, please visit http://docs.codenvy.com/user/project-lifecycle/#configuration-reference")
    public void shouldNotValidateIfStorageLocationContainIncorrectEncodedSymbol() throws ApiException {
        // given
        factory = prepareFactoryWithGivenStorage("git", "http://codenvy.com/git/04%2");

        // when, then
        validator.validateSource(factory);
    }


    @Test
    public void shouldValidateIfStorageLocationIsCorrectSsh() throws ApiException {
        // given
        factory = prepareFactoryWithGivenStorage("git", "ssh://codenvy@review.gerrithub.io:29418/codenvy/exampleProject");

        // when, then
        validator.validateSource(factory);
    }

    @Test
    public void shouldValidateIfStorageLocationIsCorrectHttps() throws ApiException {
        // given
        factory = prepareFactoryWithGivenStorage("git","https://github.com/codenvy/example.git");

        // when, then
        validator.validateSource(factory);
    }

    @Test(dataProvider = "badAdvancedFactoryUrlProvider", expectedExceptions = ApiException.class)
    public void shouldNotValidateIfStorageOrStorageLocationIsInvalid(Factory factory) throws ApiException {
        validator.validateSource(factory);
    }

    @DataProvider(name = "badAdvancedFactoryUrlProvider")
    public Object[][] invalidParametersFactoryUrlProvider() throws UnsupportedEncodingException {
        Factory adv1 = prepareFactoryWithGivenStorage("notagit", VALID_REPOSITORY_URL);
        Factory adv2 = prepareFactoryWithGivenStorage("git", null);
        Factory adv3 = prepareFactoryWithGivenStorage("git", "");
        return new Object[][]{
                {adv1},// invalid vcs
                {adv2},// invalid vcsurl
                {adv3}// invalid vcsurl
        };
    }

    @Test(dataProvider = "invalidProjectNamesProvider", expectedExceptions = ApiException.class,
            expectedExceptionsMessageRegExp = "Project name must contain only Latin letters, digits or these following special characters" +
                                              " -._.")
    public void shouldThrowFactoryUrlExceptionIfProjectNameInvalid(String projectName) throws Exception {
        // given
        factory.withWorkspace(dto.createDto(WorkspaceConfigDto.class)
                                 .withProjects(Collections.singletonList(dto.createDto(
                                         ProjectConfigDto.class)
                                                                            .withType("type")
                                                                            .withName(projectName))));
        // when, then
        validator.validateProjectNames(factory);
    }

    @Test(dataProvider = "validProjectNamesProvider")
    public void shouldBeAbleToValidateValidProjectName(String projectName) throws Exception {
        // given
        factory.withWorkspace(dto.createDto(WorkspaceConfigDto.class)
                                 .withProjects(Collections.singletonList(dto.createDto(
                                         ProjectConfigDto.class)
                                                                            .withType("type")
                                                                            .withName(projectName))));
        // when, then
        validator.validateProjectNames(factory);
    }

    @DataProvider(name = "validProjectNamesProvider")
    public Object[][] validProjectNames() {
        return new Object[][]{
                {"untitled"},
                {"Untitled"},
                {"untitled.project"},
                {"untitled-project"},
                {"untitled_project"},
                {"untitled01"},
                {"000011111"},
                {"0untitled"},
                {"UU"},
                {"untitled-proj12"},
                {"untitled.pro....111"},
                {"SampleStruts"}
        };
    }

    @DataProvider(name = "invalidProjectNamesProvider")
    public Object[][] invalidProjectNames() {
        return new Object[][]{
                {"-untitled"},
                {"untitled->3"},
                {"untitled__2%"},
                {"untitled_!@#$%^&*()_+?><"}
        };
    }

    @Test
    public void shouldBeAbleToValidateIfOrgIdIsValid() throws ApiException, ParseException {
        validator.validateAccountId(factory);
    }

    @Test
    public void shouldBeAbleToValidateIfOrgIdAndOwnerAreValid()
            throws ApiException, ParseException {
        // when, then
        validator.validateAccountId(factory);
    }

    @Test(expectedExceptions = ApiException.class)
    public void shouldNotValidateIfAccountDoesNotExist() throws ApiException {
        when(accountDao.getMembers(anyString())).thenReturn(Collections.<Member>emptyList());

        validator.validateAccountId(factory);
    }

    @Test(expectedExceptions = ApiException.class, expectedExceptionsMessageRegExp = "You are not authorized to use this accountId.")
    public void shouldNotValidateIfFactoryOwnerIsNotOrgidOwner()
            throws ApiException, ParseException {
        Member wrongMember = member;
        wrongMember.setUserId("anotheruserid");
        when(accountDao.getMembers(anyString())).thenReturn(Arrays.asList(wrongMember));

        // when, then
        validator.validateAccountId(factory);
    }

    @Test
    public void shouldValidateIfCurrentTimeBeforeSinceUntil() throws ConflictException {
        Long currentTime = new Date().getTime();

        factory.withPolicies(dto.createDto(Policies.class)
                                .withValidSince(currentTime + 10000L)
                                .withValidUntil(currentTime + 20000L)
                            );
        validator.validateCurrentTimeBeforeSinceUntil(factory);
    }

    @Test(expectedExceptions = ApiException.class,
            expectedExceptionsMessageRegExp = FactoryConstants.INVALID_VALIDSINCE_MESSAGE)
    public void shouldNotValidateIfValidSinceBeforeCurrent() throws ApiException {
        factory.withPolicies(dto.createDto(Policies.class)
                                .withValidSince(1L)
                            );
        validator.validateCurrentTimeBeforeSinceUntil(factory);
    }

    @Test(expectedExceptions = ApiException.class,
            expectedExceptionsMessageRegExp = FactoryConstants.INVALID_VALIDUNTIL_MESSAGE)
    public void shouldNotValidateIfValidUntilBeforeCurrent() throws ApiException {
        factory.withPolicies(dto.createDto(Policies.class)
                                .withValidUntil(1L)
                            );
        validator.validateCurrentTimeBeforeSinceUntil(factory);
    }

    @Test(expectedExceptions = ApiException.class,
            expectedExceptionsMessageRegExp = FactoryConstants.INVALID_VALIDSINCEUNTIL_MESSAGE)
    public void shouldNotValidateIfValidUntilBeforeValidSince() throws ApiException {
        factory.withPolicies(dto.createDto(Policies.class)
                                .withValidSince(2L)
                                .withValidUntil(1L)
                            );

        validator.validateCurrentTimeBeforeSinceUntil(factory);
    }

    @Test(expectedExceptions = ApiException.class,
            expectedExceptionsMessageRegExp = FactoryConstants.ILLEGAL_FACTORY_BY_VALIDUNTIL_MESSAGE)
    public void shouldNotValidateIfValidUntilBeforeCurrentTime() throws ApiException {
        Long currentTime = new Date().getTime();
        factory.withPolicies(dto.createDto(Policies.class)
                                .withValidUntil(currentTime - 10000L)
                            );


        validator.validateCurrentTimeBetweenSinceUntil(factory);
    }

    @Test
    public void shouldValidateIfCurrentTimeBetweenValidUntilSince() throws ApiException {
        Long currentTime = new Date().getTime();

        factory.withPolicies(dto.createDto(Policies.class)
                                .withValidSince(currentTime - 10000L)
                                .withValidUntil(currentTime + 10000L)
                            );

        validator.validateCurrentTimeBetweenSinceUntil(factory);
    }

    @Test(expectedExceptions = ApiException.class,
            expectedExceptionsMessageRegExp = FactoryConstants.ILLEGAL_FACTORY_BY_VALIDSINCE_MESSAGE)
    public void shouldNotValidateIfValidUntilSinceAfterCurrentTime() throws ApiException {
        Long currentTime = new Date().getTime();
        factory.withPolicies(dto.createDto(Policies.class)
                                .withValidSince(currentTime + 10000L)
                            );

        validator.validateCurrentTimeBetweenSinceUntil(factory);
    }


    @Test
    public void shouldValidateTrackedParamsIfOrgIdIsMissingButOnPremisesTrue() throws Exception {
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        Factory factory = dtoFactory.createDto(Factory.class);
        factory.withV("4.0")
               .withPolicies(dtoFactory.createDto(Policies.class)
                                       .withValidSince(System.currentTimeMillis() + 1_000_000)
                                       .withValidUntil(System.currentTimeMillis() + 10_000_000)
                                       .withRefererHostname("codenvy.com"));
        validator = new TesterFactoryBaseValidator(accountDao, preferenceDao);

        validator.validateAccountId(factory);
    }


    @Test(expectedExceptions = ConflictException.class)
    public void shouldNotValidateOpenfileActionIfInWrongSectionOnAppClosed() throws Exception {
        //given
        validator = new TesterFactoryBaseValidator(accountDao, preferenceDao);
        List<Action> actions = Arrays.asList(dto.createDto(Action.class).withId("openFile"));
        Ide ide = dto.createDto(Ide.class).withOnAppClosed(dto.createDto(OnAppClosed.class).withActions(actions));
        Factory factoryWithAccountId = dto.clone(factory).withIde(ide);
        //when
        validator.validateProjectActions(factoryWithAccountId);
    }

    @Test(expectedExceptions = ConflictException.class)
    public void shouldNotValidateFindReplaceActionIfInWrongSectionOnAppLoaded() throws Exception {
        //given
        validator = new TesterFactoryBaseValidator(accountDao, preferenceDao);
        List<Action> actions = Arrays.asList(dto.createDto(Action.class).withId("findReplace"));
        Ide ide = dto.createDto(Ide.class).withOnAppLoaded(dto.createDto(OnAppLoaded.class).withActions(actions));
        Factory factoryWithAccountId = dto.clone(factory).withIde(ide);
        //when
        validator.validateProjectActions(factoryWithAccountId);
    }

    @Test(expectedExceptions = ConflictException.class)
    public void shouldNotValidateIfOpenfileActionInsufficientParams() throws Exception {
        //given
        validator = new TesterFactoryBaseValidator(accountDao, preferenceDao);
        List<Action> actions = Arrays.asList(dto.createDto(Action.class).withId("openFile"));
        Ide ide = dto.createDto(Ide.class).withOnProjectOpened(dto.createDto(OnProjectOpened.class).withActions(actions));
        Factory factoryWithAccountId = dto.clone(factory).withIde(ide);
        //when
        validator.validateProjectActions(factoryWithAccountId);
    }

    @Test(expectedExceptions = ConflictException.class)
    public void shouldNotValidateIfOpenWelcomePageActionInsufficientParams() throws Exception {
        //given
        validator = new TesterFactoryBaseValidator(accountDao, preferenceDao);
        List<Action> actions = Arrays.asList(dto.createDto(Action.class).withId("openWelcomePage"));
        Ide ide = dto.createDto(Ide.class).withOnAppLoaded((dto.createDto(OnAppLoaded.class).withActions(actions)));
        Factory factoryWithAccountId = dto.clone(factory).withIde(ide);
        //when
        validator.validateProjectActions(factoryWithAccountId);
    }

    @Test(expectedExceptions = ConflictException.class)
    public void shouldNotValidateIfFindReplaceActionInsufficientParams() throws Exception {
        //given
        validator = new TesterFactoryBaseValidator(accountDao, preferenceDao);
        Map<String, String> params = new HashMap<>();
        params.put("in", "pom.xml");
        // find is missing!
        params.put("replace", "123");
        List<Action> actions = Arrays.asList(dto.createDto(Action.class).withId("findReplace").withProperties(params));
        Ide ide = dto.createDto(Ide.class).withOnProjectOpened(
                dto.createDto(OnProjectOpened.class).withActions(actions));
        Factory factoryWithAccountId = dto.clone(factory).withIde(ide);
        //when
        validator.validateProjectActions(factoryWithAccountId);
    }

    @Test
    public void shouldValidateFindReplaceAction() throws Exception {
        //given
        validator = new TesterFactoryBaseValidator(accountDao, preferenceDao);
        Map<String, String> params = new HashMap<>();
        params.put("in", "pom.xml");
        params.put("find", "123");
        params.put("replace", "456");
        List<Action> actions = Arrays.asList(dto.createDto(Action.class).withId("findReplace").withProperties(params));
        Ide ide = dto.createDto(Ide.class).withOnProjectOpened(
                dto.createDto(OnProjectOpened.class).withActions(actions));
        Factory factoryWithAccountId = dto.clone(factory).withIde(ide);
        //when
        validator.validateProjectActions(factoryWithAccountId);
    }

    @Test
    public void shouldValidateOpenfileAction() throws Exception {
        //given
        validator = new TesterFactoryBaseValidator(accountDao, preferenceDao);
        Map<String, String> params = new HashMap<>();
        params.put("file", "pom.xml");
        List<Action> actions = Arrays.asList(dto.createDto(Action.class).withId("openFile").withProperties(params));
        Ide ide = dto.createDto(Ide.class).withOnProjectOpened(
                dto.createDto(OnProjectOpened.class).withActions(actions));
        Factory factoryWithAccountId = dto.clone(factory).withIde(ide);
        //when
        validator.validateProjectActions(factoryWithAccountId);
    }


    @DataProvider(name = "trackedFactoryParameterWithoutValidAccountId")
    public Object[][] trackedFactoryParameterWithoutValidAccountId() throws URISyntaxException, IOException, NoSuchMethodException {
        return new Object[][]{
                {
                        dto.createDto(Factory.class)
                           .withV("4.0")
                           .withIde(dto.createDto(Ide.class)
                                       .withOnAppLoaded(dto.createDto(OnAppLoaded.class)
                                                           .withActions(singletonList(dto.createDto(Action.class)
                                                                                         .withId("openWelcomePage")
                                                                                         .withProperties(
                                                                                                 ImmutableMap
                                                                                                         .<String,
                                                                                                                 String>builder()
                                                                                                         .put("authenticatedTitle",
                                                                                                              "title")
                                                                                                         .put("authenticatedIconUrl",
                                                                                                              "url")
                                                                                                         .put("authenticatedContentUrl",
                                                                                                              "url")
                                                                                                         .put("nonAuthenticatedTitle",
                                                                                                              "title")
                                                                                                         .put("nonAuthenticatedIconUrl",
                                                                                                              "url")
                                                                                                         .put("nonAuthenticatedContentUrl",
                                                                                                              "url")
                                                                                                         .build()))
                                                                       )))},

                {dto.createDto(Factory.class).withV("4.0").withPolicies(dto.createDto(Policies.class).withValidSince(10000L))},
                {dto.createDto(Factory.class).withV("4.0").withPolicies(dto.createDto(Policies.class)
                                                                           .withValidUntil(10000L))},
                {dto.createDto(Factory.class).withV("4.0").withPolicies(dto.createDto(Policies.class).withRefererHostname("host"))}
        };
    }

    private Factory prepareFactoryWithGivenStorage(String type, String location) {
        return factory.withWorkspace(dto.createDto(WorkspaceConfigDto.class)
                                        .withProjects(Collections.singletonList(dto.createDto(
                                                ProjectConfigDto.class)
                                                                                   .withSource(dto.createDto(SourceStorageDto.class)
                                                                                                   .withType(type)
                                                                                                   .withLocation(location)))));
    }
}
