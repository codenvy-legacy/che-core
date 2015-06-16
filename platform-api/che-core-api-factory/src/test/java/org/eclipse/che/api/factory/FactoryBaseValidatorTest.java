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
package org.eclipse.che.api.factory;

import com.google.common.collect.ImmutableMap;

import org.eclipse.che.api.account.server.dao.AccountDao;
import org.eclipse.che.api.account.server.dao.Member;
import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.factory.dto.Action;
import org.eclipse.che.api.factory.dto.Actions;
import org.eclipse.che.api.factory.dto.Author;
import org.eclipse.che.api.factory.dto.Factory;
import org.eclipse.che.api.factory.dto.Ide;
import org.eclipse.che.api.factory.dto.OnAppClosed;
import org.eclipse.che.api.factory.dto.OnAppLoaded;
import org.eclipse.che.api.factory.dto.OnProjectOpened;
import org.eclipse.che.api.factory.dto.Policies;
import org.eclipse.che.api.factory.dto.WelcomePage;
import org.eclipse.che.api.factory.dto.Workspace;
import org.eclipse.che.api.project.shared.dto.ImportSourceDescriptor;
import org.eclipse.che.api.project.shared.dto.NewProject;
import org.eclipse.che.api.project.shared.dto.Source;
import org.eclipse.che.api.user.server.dao.PreferenceDao;
import org.eclipse.che.api.user.server.dao.User;
import org.eclipse.che.api.user.server.dao.UserDao;
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

    private TestFactoryBaseValidator validator;

    private Member member;

    private Factory factory;

    private DtoFactory dto = DtoFactory.getInstance();


    @BeforeMethod
    public void setUp() throws ParseException, NotFoundException, ServerException {
        factory = dto.createDto(Factory.class)
                     .withV("2.1")
                     .withSource(dto.createDto(Source.class)
                                    .withProject(dto.createDto(ImportSourceDescriptor.class)
                                                    .withType("git")
                                                    .withLocation(VALID_REPOSITORY_URL)))
                     .withCreator(dto.createDto(Author.class)
                                     .withAccountId(ID)
                                     .withUserId("userid"));

        User user = new User().withId("userid");

        member = new Member().withUserId("userid").withRoles(Arrays.asList("account/owner"));
        when(accountDao.getMembers(anyString())).thenReturn(Arrays.asList(member));
        when(userDao.getById("userid")).thenReturn(user);


        validator = new TestFactoryBaseValidator(accountDao, userDao, preferenceDao);
    }

    @Test
    public void shouldBeAbleToValidateFactoryUrlObject() throws ApiException {
        validator.validateSource(factory);
        validator.validateProjectName(factory);
        validator.validateAccountId(factory);
    }

    @Test
    public void shouldBeAbleToValidateFactoryUrlObjectIfVcsIsESBWSO2() throws ApiException {
        factory = factory.withSource(dto.createDto(Source.class)
                                        .withProject(dto.createDto(ImportSourceDescriptor.class)
                                                        .withType("esbwso2")
                                                        .withLocation(VALID_REPOSITORY_URL)));


        validator.validateSource(factory);
        validator.validateProjectName(factory);
        validator.validateAccountId(factory);
    }

    @Test(expectedExceptions = ApiException.class,
            expectedExceptionsMessageRegExp =
                    "The parameter source.project.location has a value submitted http://codenvy.com/git/04%2 with a value that is " +
                    "unexpected. " +
                    "For more information, please visit http://docs.codenvy.com/user/project-lifecycle/#configuration-reference")
    public void shouldNotValidateIfVcsurlContainIncorrectEncodedSymbol() throws ApiException {
        // given
        factory = factory.withSource(dto.createDto(Source.class)
                                        .withProject(dto.createDto(ImportSourceDescriptor.class)
                                                        .withType("git")
                                                        .withLocation("http://codenvy.com/git/04%2")));

        // when, then
        validator.validateSource(factory);
    }

    @Test(expectedExceptions = ApiException.class,
            expectedExceptionsMessageRegExp = "Attribute workspace.type have only two possible values - named or temp.")
    public void shouldNotValidateIfWorkspaceTypeIsInvalid() throws ApiException {
        factory = factory.withWorkspace(dto.createDto(Workspace.class)
                                           .withType("wrongg"));

        validator.validateWorkspace(factory);
    }

    @Test(expectedExceptions = ApiException.class,
            expectedExceptionsMessageRegExp = "Attribute workspace.location have only two possible values - owner or acceptor.")
    public void shouldNotValidateIfWorkspaceLocationIsInvalid() throws ApiException {
        factory = factory.withWorkspace(dto.createDto(Workspace.class)
                                           .withLocation("wrongg"));

        validator.validateWorkspace(factory);
    }

    @Test(expectedExceptions = ApiException.class,
            expectedExceptionsMessageRegExp = "Current workspace location requires factory creator accountId to be set.")
    public void shouldNotValidateIfWorkspaceLocationOwnerButNotAuthor() throws ApiException {
        factory = factory.withWorkspace(dto.createDto(Workspace.class).withLocation("owner")).withCreator(null);
        validator.validateCreator(factory);
    }

    @Test
    public void shouldValidateIfVcsurlIsCorrectSsh() throws ApiException {
        // given
        factory = factory.withSource(dto.createDto(Source.class)
                                        .withProject(dto.createDto(ImportSourceDescriptor.class)
                                                        .withType("git")
                                                        .withLocation("ssh://codenvy@review.gerrithub.io:29418/codenvy/exampleProject")));

        // when, then
        validator.validateSource(factory);
    }

    @Test
    public void shouldValidateIfVcsurlIsCorrectHttps() throws ApiException {
        // given
        factory = factory.withSource(dto.createDto(Source.class)
                                        .withProject(dto.createDto(ImportSourceDescriptor.class)
                                                        .withType("git")
                                                        .withLocation("https://github.com/codenvy/example.git")));

        // when, then
        validator.validateSource(factory);
    }

    @Test(dataProvider = "badAdvancedFactoryUrlProvider", expectedExceptions = ApiException.class)
    public void shouldNotValidateIfVcsOrVcsUrlIsInvalid(Factory factory) throws ApiException {
        validator.validateSource(factory);
    }

    @DataProvider(name = "badAdvancedFactoryUrlProvider")
    public Object[][] invalidParametersFactoryUrlProvider() throws UnsupportedEncodingException {
        Factory adv1 = DtoFactory.getInstance().createDto(Factory.class)
                                 .withV("2.1")
                                 .withSource(dto.createDto(Source.class)
                                                .withProject(dto.createDto(ImportSourceDescriptor.class)
                                                                .withType("notagit")
                                                                .withLocation(VALID_REPOSITORY_URL)));


        Factory adv2 = DtoFactory.getInstance().createDto(Factory.class).withV("2.1")
                                 .withSource(dto.createDto(Source.class)
                                                .withProject(dto.createDto(ImportSourceDescriptor.class)
                                                                .withType("git")));


        Factory adv3 = DtoFactory.getInstance().createDto(Factory.class).withV("2.1")
                                 .withSource(dto.createDto(Source.class)
                                                .withProject(dto.createDto(ImportSourceDescriptor.class)
                                                                .withType("git").withLocation("")));


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
        factory.withProject(dto.createDto(NewProject.class)
                               .withType("type")
                               .withName(projectName));


        // when, then
        validator.validateProjectName(factory);
    }

    @Test(dataProvider = "validProjectNamesProvider")
    public void shouldBeAbleToValidateValidProjectName(String projectName) throws Exception {
        // given
        factory.withProject(dto.createDto(NewProject.class)
                               .withType("type")
                               .withName(projectName));


        // when, then
        validator.validateProjectName(factory);
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
                                .withValidSince(currentTime + 10000l)
                                .withValidUntil(currentTime + 20000l)
                            );
        validator.validateCurrentTimeBeforeSinceUntil(factory);
    }

    @Test(expectedExceptions = ApiException.class,
            expectedExceptionsMessageRegExp = FactoryConstants.INVALID_VALIDSINCE_MESSAGE)
    public void shouldNotValidateIfValidSinceBeforeCurrent() throws ApiException {
        factory.withPolicies(dto.createDto(Policies.class)
                                .withValidSince(1l)
                            );
        validator.validateCurrentTimeBeforeSinceUntil(factory);
    }

    @Test(expectedExceptions = ApiException.class,
            expectedExceptionsMessageRegExp = FactoryConstants.INVALID_VALIDUNTIL_MESSAGE)
    public void shouldNotValidateIfValidUntilBeforeCurrent() throws ApiException {
        factory.withPolicies(dto.createDto(Policies.class)
                                .withValidUntil(1l)
                            );
        validator.validateCurrentTimeBeforeSinceUntil(factory);
    }

    @Test(expectedExceptions = ApiException.class,
            expectedExceptionsMessageRegExp = FactoryConstants.INVALID_VALIDSINCEUNTIL_MESSAGE)
    public void shouldNotValidateIfValidUntilBeforeValidSince() throws ApiException {
        factory.withPolicies(dto.createDto(Policies.class)
                                .withValidSince(2l)
                                .withValidUntil(1l)
                            );

        validator.validateCurrentTimeBeforeSinceUntil(factory);
    }

    @Test(expectedExceptions = ApiException.class,
            expectedExceptionsMessageRegExp = FactoryConstants.ILLEGAL_FACTORY_BY_VALIDUNTIL_MESSAGE)
    public void shouldNotValidateIfValidUntilBeforeCurrentTime() throws ApiException {
        Long currentTime = new Date().getTime();
        factory.withPolicies(dto.createDto(Policies.class)
                                .withValidUntil(currentTime - 10000l)
                            );


        validator.validateCurrentTimeBetweenSinceUntil(factory);
    }

    @Test
    public void shouldValidateIfCurrentTimeBetweenValidUntilSince() throws ApiException {
        Long currentTime = new Date().getTime();

        factory.withPolicies(dto.createDto(Policies.class)
                                .withValidSince(currentTime - 10000l)
                                .withValidUntil(currentTime + 10000l)
                            );

        validator.validateCurrentTimeBetweenSinceUntil(factory);
    }

    @Test(expectedExceptions = ApiException.class,
            expectedExceptionsMessageRegExp = FactoryConstants.ILLEGAL_FACTORY_BY_VALIDSINCE_MESSAGE)
    public void shouldNotValidateIfValidUntilSinceAfterCurrentTime() throws ApiException {
        Long currentTime = new Date().getTime();
        factory.withPolicies(dto.createDto(Policies.class)
                                .withValidSince(currentTime + 10000l)
                            );

        validator.validateCurrentTimeBetweenSinceUntil(factory);
    }


    @Test
    public void shouldValidateTrackedParamsIfOrgIdIsMissingButOnPremisesTrue() throws Exception {
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        Factory factory = dtoFactory.createDto(Factory.class);
        factory.withV("2.1")
               .withPolicies(dtoFactory.createDto(Policies.class)
                                       .withValidSince(System.currentTimeMillis() + 1_000_000)
                                       .withValidUntil(System.currentTimeMillis() + 10_000_000)
                                       .withRefererHostname("codenvy.com"))
               .withActions(dtoFactory.createDto(Actions.class)
                                      .withWelcome(dtoFactory.createDto(WelcomePage.class)));
        validator = new TestFactoryBaseValidator(accountDao, userDao, preferenceDao);

        validator.validateAccountId(factory);
    }


    @Test(expectedExceptions = ConflictException.class)
    public void shouldNotValidateOpenfileActionIfInWrongSectionOnAppClosed() throws Exception {
        //given
        validator = new TestFactoryBaseValidator(accountDao, userDao, preferenceDao);
        List<Action> actions = Arrays.asList(dto.createDto(Action.class).withId("openFile"));
        Ide ide = dto.createDto(Ide.class).withOnAppClosed(dto.createDto(OnAppClosed.class).withActions(actions));
        Factory factoryWithAccountId = dto.clone(factory).withIde(ide);
        //when
        validator.validateProjectActions(factoryWithAccountId);
    }

    @Test(expectedExceptions = ConflictException.class)
    public void shouldNotValidateFindReplaceActionIfInWrongSectionOnAppLoaded() throws Exception {
        //given
        validator = new TestFactoryBaseValidator(accountDao, userDao, preferenceDao);
        List<Action> actions = Arrays.asList(dto.createDto(Action.class).withId("findReplace"));
        Ide ide = dto.createDto(Ide.class).withOnAppLoaded(dto.createDto(OnAppLoaded.class).withActions(actions));
        Factory factoryWithAccountId = dto.clone(factory).withIde(ide);
        //when
        validator.validateProjectActions(factoryWithAccountId);
    }

    @Test(expectedExceptions = ConflictException.class)
    public void shouldNotValidateIfOpenfileActionInsufficientParams() throws Exception {
        //given
        validator = new TestFactoryBaseValidator(accountDao, userDao, preferenceDao);
        List<Action> actions = Arrays.asList(dto.createDto(Action.class).withId("openFile"));
        Ide ide = dto.createDto(Ide.class).withOnProjectOpened(dto.createDto(OnProjectOpened.class).withActions(actions));
        Factory factoryWithAccountId = dto.clone(factory).withIde(ide);
        //when
        validator.validateProjectActions(factoryWithAccountId);
    }

    @Test(expectedExceptions = ConflictException.class)
    public void shouldNotValidateIfOpenWelcomePageActionInsufficientParams() throws Exception {
        //given
        validator = new TestFactoryBaseValidator(accountDao, userDao, preferenceDao);
        List<Action> actions = Arrays.asList(dto.createDto(Action.class).withId("openWelcomePage"));
        Ide ide = dto.createDto(Ide.class).withOnAppLoaded((dto.createDto(OnAppLoaded.class).withActions(actions)));
        Factory factoryWithAccountId = dto.clone(factory).withIde(ide);
        //when
        validator.validateProjectActions(factoryWithAccountId);
    }

    @Test(expectedExceptions = ConflictException.class)
    public void shouldNotValidateIfFindReplaceActionInsufficientParams() throws Exception {
        //given
        validator = new TestFactoryBaseValidator(accountDao, userDao, preferenceDao);
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
        validator = new TestFactoryBaseValidator(accountDao, userDao, preferenceDao);
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
        validator = new TestFactoryBaseValidator(accountDao, userDao, preferenceDao);
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
                           .withV("2.1")
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

                {dto.createDto(Factory.class).withV("2.1").withPolicies(dto.createDto(Policies.class).withValidSince(10000l))},
                {dto.createDto(Factory.class).withV("2.1").withPolicies(dto.createDto(Policies.class).withValidUntil(10000l))},
                {dto.createDto(Factory.class).withV("2.0")
                    .withActions(dto.createDto(Actions.class).withWelcome(dto.createDto(WelcomePage.class)))},
                {dto.createDto(Factory.class).withV("2.1").withPolicies(dto.createDto(Policies.class).withRefererHostname("host"))}
        };
    }

}
