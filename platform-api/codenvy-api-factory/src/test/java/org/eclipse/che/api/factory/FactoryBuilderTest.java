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

import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.factory.FactoryParameter;
import org.eclipse.che.api.factory.dto.Action;
import org.eclipse.che.api.factory.dto.Actions;
import org.eclipse.che.api.factory.dto.Author;
import org.eclipse.che.api.factory.dto.Button;
import org.eclipse.che.api.factory.dto.ButtonAttributes;
import org.eclipse.che.api.factory.dto.Factory;
import org.eclipse.che.api.factory.dto.Ide;
import org.eclipse.che.api.factory.dto.OnAppClosed;
import org.eclipse.che.api.factory.dto.OnAppLoaded;
import org.eclipse.che.api.factory.dto.OnProjectOpened;
import org.eclipse.che.api.factory.dto.Policies;
import org.eclipse.che.api.factory.dto.WelcomeConfiguration;
import org.eclipse.che.api.factory.dto.WelcomePage;
import org.eclipse.che.api.factory.dto.Workspace;
import org.eclipse.che.api.project.shared.dto.BuildersDescriptor;
import org.eclipse.che.api.project.shared.dto.ImportSourceDescriptor;
import org.eclipse.che.api.project.shared.dto.NewProject;
import org.eclipse.che.api.project.shared.dto.RunnerConfiguration;
import org.eclipse.che.api.project.shared.dto.RunnerSource;
import org.eclipse.che.api.project.shared.dto.RunnersDescriptor;
import org.eclipse.che.api.project.shared.dto.Source;
import org.eclipse.che.api.vfs.shared.dto.ReplacementSet;
import org.eclipse.che.api.vfs.shared.dto.Variable;
import org.eclipse.che.dto.server.DtoFactory;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.eclipse.che.api.core.factory.FactoryParameter.FactoryFormat;
import static org.eclipse.che.api.core.factory.FactoryParameter.FactoryFormat.ENCODED;
import static org.eclipse.che.api.core.factory.FactoryParameter.FactoryFormat.NONENCODED;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;

/**
 * Tests for {@link org.eclipse.che.api.factory.dto.Factory}
 *
 * @author Alexander Garagatyi
 * @author Sergii Kabashniuk
 */
@Listeners(MockitoTestNGListener.class)
public class FactoryBuilderTest {

    private static DtoFactory dto = DtoFactory.getInstance();

    private FactoryBuilder factoryBuilder;

    private Factory actual;

    private Factory expected;

    @Mock
    private SourceProjectParametersValidator sourceProjectParametersValidator;

    @BeforeMethod
    public void setUp() throws Exception {
        factoryBuilder = new FactoryBuilder(sourceProjectParametersValidator);
        actual = dto.createDto(Factory.class);

        expected = dto.createDto(Factory.class);
    }

    @Test(dataProvider = "jsonprovider")
    public void shouldBeAbleToParserJsonV1_1(String json) {
        dto.createDtoFromJson(json, Factory.class);
    }


    @DataProvider(name = "jsonprovider")
    public static Object[][] createData() throws URISyntaxException, IOException {
        File file = new File(FactoryBuilderTest.class.getResource("/logback-test.xml").toURI());
        File resourcesDirectory = file.getParentFile();
        String[] list = resourcesDirectory.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".json");
            }
        });
        Object[][] result = new Object[list.length][1];
        for (int i = 0; i < list.length; i++) {
            result[i][0] = new String(Files.readAllBytes(new File(resourcesDirectory, list[i]).toPath()), "UTF-8");
        }

        return result;
    }

    @Test
    public void shouldBeAbleToValidateEncodedV2_0() throws Exception {
        actual.withV("2.0")
              .withSource(dto.createDto(Source.class)
                             .withProject(dto.createDto(ImportSourceDescriptor.class)
                                             .withType("git")
                                             .withLocation("location")
                                             .withParameters(singletonMap("key", "value")))
                             .withRunners(singletonMap("runEnv", dto.createDto(RunnerSource.class)
                                                                    .withLocation("location")
                                                                    .withParameters(singletonMap("key", "value")))))
              .withProject(dto.createDto(NewProject.class)
                              .withType("type")
                              .withAttributes(singletonMap("key", singletonList("value")))
                              .withBuilders(dto.createDto(BuildersDescriptor.class).withDefault("default"))
                              .withDescription("description")
                              .withName("name")
                              .withRunners(dto.createDto(RunnersDescriptor.class)
                                              .withDefault("default")
                                              .withConfigs(singletonMap("key", dto.createDto(RunnerConfiguration.class)
                                                                                  .withRam(768)
                                                                                  .withOptions(singletonMap("key", "value"))
                                                                                  .withVariables(singletonMap("key", "value")))))
                              .withVisibility("private"))
              .withCreator(dto.createDto(Author.class)
                              .withAccountId("accountId")
                              .withEmail("email")
                              .withName("name"))
              .withPolicies(dto.createDto(Policies.class)
                               .withRefererHostname("referrer")
                               .withValidSince(123l)
                               .withValidUntil(123l))
              .withActions(dto.createDto(Actions.class)
                              .withFindReplace(singletonList(dto.createDto(ReplacementSet.class)
                                                                .withFiles(singletonList("file"))
                                                                .withEntries(singletonList(dto.createDto(Variable.class)
                                                                                              .withFind("find")
                                                                                              .withReplace("replace")
                                                                                              .withReplacemode("mode")))))
                              .withOpenFile("openFile")
                              .withWarnOnClose(true)
                              .withWelcome(dto.createDto(WelcomePage.class)
                                              .withAuthenticated(dto.createDto(WelcomeConfiguration.class)
                                                                    .withContenturl("url")
                                                                    .withNotification("notification")
                                                                    .withTitle("title"))
                                              .withNonauthenticated(dto.createDto(WelcomeConfiguration.class)
                                                                       .withContenturl("url")
                                                                       .withNotification("notification")
                                                                       .withTitle("title"))))
              .withButton(dto.createDto(Button.class)
                             .withType(Button.ButtonType.logo)
                             .withAttributes(dto.createDto(ButtonAttributes.class)
                                                .withColor("color")
                                                .withCounter(true)
                                                .withLogo("logo")
                                                .withStyle("style")))
              .withWorkspace(dto.createDto(Workspace.class)
                                .withType("named"));
        factoryBuilder.checkValid(actual, ENCODED);

        verify(sourceProjectParametersValidator).validate(any(ImportSourceDescriptor.class), eq(FactoryParameter.Version.V2_0));
    }

    @Test
    public void shouldBeAbleToValidateNonEncodedV2_0() throws Exception {
        actual.withV("2.0")
              .withSource(dto.createDto(Source.class)
                             .withProject(dto.createDto(ImportSourceDescriptor.class)
                                             .withType("git")
                                             .withLocation("location")
                                             .withParameters(singletonMap("key", "value")))
                             .withRunners(singletonMap("runEnv", dto.createDto(RunnerSource.class)
                                                                    .withLocation("location")
                                                                    .withParameters(singletonMap("key", "value")))))
              .withProject(dto.createDto(NewProject.class)
                              .withType("type")
                              .withAttributes(singletonMap("key", singletonList("value")))
                              .withBuilders(dto.createDto(BuildersDescriptor.class).withDefault("default"))
                              .withDescription("description")
                              .withName("name")
                              .withVisibility("private")
                              .withRunners(dto.createDto(RunnersDescriptor.class)
                                              .withDefault("default")
                                              .withConfigs(singletonMap("key", dto.createDto(RunnerConfiguration.class)
                                                                                  .withRam(768)
                                                                                  .withOptions(singletonMap("key", "value"))
                                                                                  .withVariables(singletonMap("key", "value"))))))
              .withCreator(dto.createDto(Author.class)
                              .withAccountId("accountId")
                              .withEmail("email")
                              .withName("name"))
              .withPolicies(dto.createDto(Policies.class)
                               .withRefererHostname("referrer")
                               .withValidSince(123l)
                               .withValidUntil(123l))
              .withActions(dto.createDto(Actions.class)
                              .withOpenFile("openFile")
                              .withWarnOnClose(true)
                              .withFindReplace(singletonList(dto.createDto(ReplacementSet.class)
                                                                .withFiles(singletonList("file"))
                                                                .withEntries(singletonList(dto.createDto(Variable.class)
                                                                                              .withFind("find")
                                                                                              .withReplace("replace")
                                                                                              .withReplacemode("mode"))))))
              .withWorkspace(dto.createDto(Workspace.class)
                                .withType("named"));

        factoryBuilder.checkValid(actual, NONENCODED);

        verify(sourceProjectParametersValidator).validate(any(ImportSourceDescriptor.class), eq(FactoryParameter.Version.V2_0));
    }


    @Test
    public void shouldBeAbleToValidateEncodedV2_1() throws Exception {
        actual.withV("2.1")
              .withSource(dto.createDto(Source.class)
                             .withProject(dto.createDto(ImportSourceDescriptor.class)
                                             .withType("git")
                                             .withLocation("location")
                                             .withParameters(singletonMap("key", "value")))
                             .withRunners(singletonMap("runEnv", dto.createDto(RunnerSource.class)
                                                                    .withLocation("location")
                                                                    .withParameters(singletonMap("key", "value")))))
              .withProject(dto.createDto(NewProject.class)
                              .withType("type")
                              .withAttributes(singletonMap("key", singletonList("value")))
                              .withBuilders(dto.createDto(BuildersDescriptor.class).withDefault("default"))
                              .withDescription("description")
                              .withName("name")
                              .withRunners(dto.createDto(RunnersDescriptor.class)
                                              .withDefault("default")
                                              .withConfigs(singletonMap("key", dto.createDto(RunnerConfiguration.class)
                                                                                  .withRam(768)
                                                                                  .withOptions(singletonMap("key", "value"))
                                                                                  .withVariables(singletonMap("key", "value")))))
                              .withVisibility("private"))
              .withCreator(dto.createDto(Author.class)
                              .withAccountId("accountId")
                              .withEmail("email")
                              .withName("name"))
              .withPolicies(dto.createDto(Policies.class)
                               .withRefererHostname("referrer")
                               .withValidSince(123l)
                               .withValidUntil(123l))
              .withButton(dto.createDto(Button.class)
                             .withType(Button.ButtonType.logo)
                             .withAttributes(dto.createDto(ButtonAttributes.class)
                                                .withColor("color")
                                                .withCounter(true)
                                                .withLogo("logo")
                                                .withStyle("style")))
              .withWorkspace(dto.createDto(Workspace.class)
                                .withType("named"))
              .withIde(dto.createDto(Ide.class)
                          .withOnAppClosed(
                                  dto.createDto(OnAppClosed.class)
                                     .withActions(singletonList(dto.createDto(Action.class).withId("warnOnClose"))))
                          .withOnAppLoaded(
                                  dto.createDto(OnAppLoaded.class)
                                     .withActions(Arrays.asList(dto.createDto(Action.class).withId("newProject"),
                                                                dto.createDto(Action.class)
                                                                   .withId("openWelcomePage")
                                                                   .withProperties(ImmutableMap.of(
                                                                           "authenticatedTitle",
                                                                           "Greeting title for authenticated users",
                                                                           "authenticatedContentUrl",
                                                                           "http://example.com/content.url")))))
                          .withOnProjectOpened(dto.createDto(OnProjectOpened.class)
                                                  .withActions(Arrays.asList(
                                                          dto.createDto(Action.class)
                                                             .withId("openFile")
                                                             .withProperties(singletonMap("file", "pom.xml")),
                                                          dto.createDto(Action.class)
                                                             .withId("run"),
                                                          dto.createDto(Action.class)
                                                             .withId("findReplace")
                                                             .withProperties(ImmutableMap.of("in", "src/main/resources/consts2.properties",
                                                                                             "find", "OLD_VALUE_2",
                                                                                             "replace", "NEW_VALUE_2",
                                                                                             "replaceMode", "mode"))))));

        factoryBuilder.checkValid(actual, ENCODED);

        verify(sourceProjectParametersValidator).validate(any(ImportSourceDescriptor.class), eq(FactoryParameter.Version.V2_1));
    }

    @Test
    public void shouldBeAbleToValidateNonEncodedV2_1() throws Exception {
        actual.withV("2.1")
              .withSource(dto.createDto(Source.class)
                             .withProject(dto.createDto(ImportSourceDescriptor.class)
                                             .withType("git")
                                             .withLocation("location")
                                             .withParameters(singletonMap("key", "value")))
                             .withRunners(singletonMap("runEnv", dto.createDto(RunnerSource.class)
                                                                    .withLocation("location")
                                                                    .withParameters(singletonMap("key", "value")))))
              .withProject(dto.createDto(NewProject.class)
                              .withType("type")
                              .withAttributes(singletonMap("key", singletonList("value")))
                              .withBuilders(dto.createDto(BuildersDescriptor.class).withDefault("default"))
                              .withDescription("description")
                              .withName("name")
                              .withRunners(dto.createDto(RunnersDescriptor.class)
                                              .withDefault("default")
                                              .withConfigs(singletonMap("key", dto.createDto(RunnerConfiguration.class)
                                                                                  .withRam(768)
                                                                                  .withOptions(singletonMap("key", "value"))
                                                                                  .withVariables(singletonMap("key", "value")))))
                              .withVisibility("private"))
              .withCreator(dto.createDto(Author.class)
                              .withAccountId("accountId")
                              .withEmail("email")
                              .withName("name"))
              .withPolicies(dto.createDto(Policies.class)
                               .withRefererHostname("referrer")
                               .withValidSince(123l)
                               .withValidUntil(123l))
              .withWorkspace(dto.createDto(Workspace.class)
                                .withType("named"))
              .withIde(dto.createDto(Ide.class)
                          .withOnAppClosed(
                                  dto.createDto(OnAppClosed.class)
                                     .withActions(singletonList(dto.createDto(Action.class).withId("warnonclose"))))
                          .withOnAppLoaded(
                                  dto.createDto(OnAppLoaded.class)
                                     .withActions(singletonList(dto.createDto(Action.class).withId("newProject"))))
                          .withOnProjectOpened(dto.createDto(OnProjectOpened.class)
                                                  .withActions(Arrays.asList(
                                                          dto.createDto(Action.class)
                                                             .withId("openWelcomePage")
                                                             .withProperties(ImmutableMap.of(
                                                                     "authenticatedTitle",
                                                                     "Greeting title for authenticated users",
                                                                     "authenticatedContentUrl",
                                                                     "http://example.com/content.url")),
                                                          dto.createDto(Action.class)
                                                             .withId("openfile")
                                                             .withProperties(singletonMap("file", "pom.xml")),
                                                          dto.createDto(Action.class)
                                                             .withId("run"),
                                                          dto.createDto(Action.class)
                                                             .withId("findReplace")
                                                             .withProperties(ImmutableMap.of("in", "src/main/resources/consts2.properties",
                                                                                             "find", "OLD_VALUE_2",
                                                                                             "replace", "NEW_VALUE_2",
                                                                                             "replaceMode", "mode"
                                                                                            ))))));

        factoryBuilder.checkValid(actual, NONENCODED);

        verify(sourceProjectParametersValidator).validate(any(ImportSourceDescriptor.class), eq(FactoryParameter.Version.V2_1));
    }


    @DataProvider(name = "TFParamsProvider")
    public static Object[][] tFParamsProvider() throws URISyntaxException, IOException, NoSuchMethodException {
        Factory v2 = dto.createDto(Factory.class).withV("2.0")
                        .withSource(dto.createDto(Source.class)
                                       .withProject(dto.createDto(ImportSourceDescriptor.class)
                                                       .withType("git")
                                                       .withLocation("location")));

        return new Object[][]{
                {dto.clone(v2).withActions(dto.createDto(Actions.class).withWelcome(dto.createDto(WelcomePage.class)))},
                {dto.clone(v2).withPolicies(dto.createDto(Policies.class))}
        };
    }

    @Test(expectedExceptions = ApiException.class)
    public void shouldNotAllowInNonencodedVersionUsingParamsOnlyForEncodedVersion() throws ApiException, URISyntaxException {
        StringBuilder sb = new StringBuilder("?");
        sb.append("v=").append("1.0").append("&");
        sb.append("vcs=").append("git").append("&");
        sb.append("welcome=").append("welcome").append("&");

        factoryBuilder.buildEncoded(new URI(sb.toString()));
    }

    @Test(expectedExceptions = ApiException.class)
    public void shouldNotValidateUnparseableFactory() throws ApiException, URISyntaxException {
        factoryBuilder.checkValid(null, NONENCODED);
    }

    @Test(expectedExceptions = ApiException.class, dataProvider = "setByServerParamsProvider",
            expectedExceptionsMessageRegExp = "You have provided an invalid parameter .* for this version of Factory parameters.*")
    public void shouldNotAllowUsingParamsThatCanBeSetOnlyByServer(Factory factory)
            throws InvocationTargetException, IllegalAccessException, ApiException, NoSuchMethodException {
        factoryBuilder.checkValid(factory, ENCODED);
    }

    @DataProvider(name = "setByServerParamsProvider")
    public static Object[][] setByServerParamsProvider() throws URISyntaxException, IOException, NoSuchMethodException {
        Factory v2 = dto.createDto(Factory.class).withV("2.0")
                        .withSource(dto.createDto(Source.class).withProject(dto.createDto(ImportSourceDescriptor.class)
                                                                               .withType("git")
                                                                               .withLocation("location")));
        return new Object[][]{
                {dto.clone(v2).withId("id")},
                {dto.clone(v2).withCreator(dto.createDto(Author.class).withUserId("id"))},
                {dto.clone(v2).withCreator(dto.createDto(Author.class).withCreated(123l))}
        };
    }

    @Test
    public void shouldBeAbleToConvert2_0ActionsToNewFormat2_1() throws ApiException {
        Factory given = dto.createDto(Factory.class).withV("2.0")
                           .withSource(dto.createDto(Source.class)
                                          .withProject(dto.createDto(ImportSourceDescriptor.class)
                                                          .withType("git")
                                                          .withLocation("location")
                                                          .withParameters(singletonMap("key", "value")))
                                          .withRunners(singletonMap("runEnv", dto.createDto(RunnerSource.class)
                                                                                 .withLocation("location")
                                                                                 .withParameters(singletonMap("key", "value")))))
                           .withProject(dto.createDto(NewProject.class)
                                           .withType("type")
                                           .withAttributes(singletonMap("key", singletonList("value")))
                                           .withBuilders(dto.createDto(BuildersDescriptor.class).withDefault("default"))
                                           .withDescription("description")
                                           .withName("name")
                                           .withVisibility("private")
                                           .withRunners(dto.createDto(RunnersDescriptor.class)
                                                           .withDefault("default")
                                                           .withConfigs(
                                                                   singletonMap("key", dto.createDto(RunnerConfiguration.class)
                                                                                          .withRam(768)
                                                                                          .withOptions(singletonMap("key", "value"))
                                                                                          .withVariables(singletonMap("key", "value"))))))
                           .withCreator(dto.createDto(Author.class)
                                           .withAccountId("accountId")
                                           .withEmail("email")
                                           .withName("name"))
                           .withPolicies(dto.createDto(Policies.class)
                                            .withRefererHostname("referrer")
                                            .withValidSince(123l)
                                            .withValidUntil(123l))
                           .withActions(dto.createDto(Actions.class)
                                           .withFindReplace(singletonList(dto.createDto(ReplacementSet.class)
                                                                             .withFiles(singletonList("file"))
                                                                             .withEntries(singletonList(
                                                                                     dto.createDto(Variable.class)
                                                                                        .withFind("find")
                                                                                        .withReplace("replace")
                                                                                        .withReplacemode("mode")))))

                                           .withOpenFile("openFile")
                                           .withWarnOnClose(true)
                                           .withWelcome(dto.createDto(WelcomePage.class)
                                                           .withAuthenticated(dto.createDto(WelcomeConfiguration.class)
                                                                                 .withContenturl("url")
                                                                                 .withNotification("notification")
                                                                                 .withTitle("title"))
                                                           .withNonauthenticated(dto.createDto(WelcomeConfiguration.class)
                                                                                    .withContenturl("url")
                                                                                    .withNotification("notification")
                                                                                    .withTitle("title"))));

        expected.withV("2.1")
                .withSource(dto.createDto(Source.class)
                               .withProject(dto.createDto(ImportSourceDescriptor.class)
                                               .withType("git")
                                               .withLocation("location")
                                               .withParameters(singletonMap("key", "value")))
                               .withRunners(singletonMap("runEnv", dto.createDto(RunnerSource.class)
                                                                      .withLocation("location")
                                                                      .withParameters(
                                                                              singletonMap("key", "value")))))
                .withProject(dto.createDto(NewProject.class)
                                .withType("type")
                                .withAttributes(singletonMap("key", singletonList("value")))
                                .withBuilders(dto.createDto(BuildersDescriptor.class).withDefault("default"))
                                .withDescription("description")
                                .withName("name")
                                .withRunners(dto.createDto(RunnersDescriptor.class)
                                                .withDefault("default")
                                                .withConfigs(singletonMap("key", dto.createDto(RunnerConfiguration.class)
                                                                                    .withRam(768)
                                                                                    .withOptions(singletonMap("key", "value"))
                                                                                    .withVariables(singletonMap("key", "value")))))
                                .withVisibility("private"))
                .withCreator(dto.createDto(Author.class)
                                .withAccountId("accountId")
                                .withEmail("email")
                                .withName("name"))
                .withPolicies(dto.createDto(Policies.class)
                                 .withRefererHostname("referrer")
                                 .withValidSince(123l)
                                 .withValidUntil(123l))
                .withIde(dto.createDto(Ide.class)
                            .withOnAppLoaded(dto.createDto(OnAppLoaded.class)
                                                .withActions(singletonList(dto.createDto(Action.class)
                                                                              .withId("openWelcomePage")
                                                                              .withProperties(ImmutableMap.<String, String>builder()
                                                                                                          .put("authenticatedTitle",
                                                                                                               "title")
                                                                                                          .put("authenticatedContentUrl",
                                                                                                               "url")
                                                                                                          .put("authenticatedNotification",
                                                                                                               "notification")
                                                                                                          .put("nonAuthenticatedTitle",
                                                                                                               "title")
                                                                                                          .put("nonAuthenticatedContentUrl",
                                                                                                               "url")
                                                                                                          .put("nonAuthenticatedNotification",
                                                                                                               "notification")
                                                                                                          .build()))))
                            .withOnAppClosed(
                                    dto.createDto(OnAppClosed.class)
                                       .withActions(
                                               singletonList(dto.createDto(Action.class).withId("warnOnClose"))))
                            .withOnProjectOpened(dto.createDto(OnProjectOpened.class)
                                                    .withActions(Arrays.asList(
                                                            dto.createDto(Action.class)
                                                               .withId("openFile")
                                                               .withProperties(singletonMap("file", "openFile")),
                                                            dto.createDto(Action.class)
                                                               .withId("findReplace")
                                                               .withProperties(ImmutableMap.of("in", "file",
                                                                                               "find", "find",
                                                                                               "replace", "replace",
                                                                                               "replaceMode", "mode"
                                                                                              ))))));

        assertEquals(factoryBuilder.convertToLatest(given), expected);
    }

    @Test(expectedExceptions = ApiException.class, dataProvider = "notValidParamsProvider")
    public void shouldNotAllowUsingNotValidParams(Factory factory, FactoryFormat encoded)
            throws InvocationTargetException, IllegalAccessException, ApiException, NoSuchMethodException {
        factoryBuilder.checkValid(factory, encoded);
    }

    @DataProvider(name = "notValidParamsProvider")
    public static Object[][] notValidParamsProvider() throws URISyntaxException, IOException, NoSuchMethodException {
        Factory factory = dto.createDto(Factory.class)
                             .withV("2.0")
                             .withSource(dto.createDto(Source.class)
                                            .withProject(dto.createDto(ImportSourceDescriptor.class)
                                                            .withType("git")
                                                            .withLocation(
                                                                    "http://github.com/codenvy/platform-api.git")));

        return new Object[][]{};
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }


    @Test
    public void shouldBeAbleToParseAndValidateNonEncodedV2_0() throws Exception {
        expected.withV("2.0")
                .withSource(dto.createDto(Source.class)
                               .withProject(dto.createDto(ImportSourceDescriptor.class)
                                               .withType("git")
                                               .withLocation("location")
                                               .withParameters(new HashMap<String, String>() {
                                                   {
                                                       put("keepVcs", "true");
                                                       put("branch", "master");
                                                       put("commitId", "123");
                                                   }
                                               }))
                               .withRunners(singletonMap("runEnv", dto.createDto(RunnerSource.class)
                                                                      .withLocation("location")
                                                                      .withParameters(singletonMap("key", "value")))))
                .withProject(dto.createDto(NewProject.class)
                                .withType("type")
                                .withAttributes(singletonMap("key", singletonList("value")))
                                .withBuilders(dto.createDto(BuildersDescriptor.class).withDefault("default"))
                                .withDescription("description")
                                .withName("name")
                                .withRunners(dto.createDto(RunnersDescriptor.class)
                                                .withDefault("default")
                                                .withConfigs(singletonMap("key", dto.createDto(RunnerConfiguration.class)
                                                                                    .withRam(768)
                                                                                    .withOptions(new HashMap<String, String>() {
                                                                                        {
                                                                                            put("key1", "value1");
                                                                                            put("key2", "value2");
                                                                                        }
                                                                                    })
                                                                                    .withVariables(new HashMap<String, String>() {
                                                                                        {
                                                                                            put("key1", "value1");
                                                                                            put("key2", "value2");
                                                                                        }
                                                                                    }))))
                                .withVisibility("private"))
                .withCreator(dto.createDto(Author.class)
                                .withAccountId("accountId")
                                .withEmail("email")
                                .withName("name"))
                .withPolicies(dto.createDto(Policies.class)
                                 .withRefererHostname("referrer")
                                 .withValidSince(123l)
                                 .withValidUntil(123l))
                .withActions(dto.createDto(Actions.class)
                                .withOpenFile("openFile")
                                .withWarnOnClose(true)
                                .withFindReplace(singletonList(dto.createDto(ReplacementSet.class)
                                                                  .withFiles(singletonList("file"))
                                                                  .withEntries(singletonList(dto.createDto(Variable.class)
                                                                                                .withFind("find")
                                                                                                .withReplace("replace")
                                                                                                .withReplacemode("mode"))))))
                .withWorkspace(dto.createDto(Workspace.class)
                                  .withType("named"));


        StringBuilder sb = new StringBuilder("?");
        sb.append("v=2.0").append("&");
        sb.append("actions.openFile=openFile").append("&");
        sb.append("actions.warnOnClose=true").append("&");
        sb.append("actions.findReplace=")
          .append(URLEncoder.encode(DtoFactory.getInstance().toJson(expected.getActions().getFindReplace()), "UTF-8")).append("&");
        sb.append("policies.refererHostname=referrer").append("&");
        sb.append("policies.validSince=123").append("&");
        sb.append("policies.validUntil=123").append("&");
        sb.append("creator.accountId=accountId").append("&");
        sb.append("creator.email=email").append("&");
        sb.append("creator.name=name").append("&");
        sb.append("workspace.type=named").append("&");
        sb.append("source.project.type=git").append("&");
        sb.append("source.project.location=location").append("&");
        sb.append("source.project.parameters.keepVcs=true").append("&");
        sb.append("source.project.parameters.commitId=123").append("&");
        sb.append("source.project.parameters.branch=master").append("&");
        sb.append("source.runners.runEnv.location=location").append("&");
        sb.append("source.runners.runEnv.parameters.key=value").append("&");
        sb.append("project.type=type").append("&");
        sb.append("project.name=name").append("&");
        sb.append("project.description=description").append("&");
        sb.append("project.attributes.key=value").append("&");
        sb.append("project.visibility=private").append("&");
        sb.append("project.builders.default=default").append("&");
        sb.append("project.runners.default=default").append("&");
        sb.append("project.runners.configs.key.ram=768").append("&");
        sb.append("project.runners.configs.key.options.key1=value1").append("&");
        sb.append("project.runners.configs.key.options.key2=value2").append("&");
        sb.append("project.runners.configs.key.variables.key1=value1").append("&");
        sb.append("project.runners.configs.key.variables.key2=value2");


        Factory newFactory = factoryBuilder.buildEncoded(new URI(sb.toString()));
        assertEquals(newFactory, expected);
    }

    @Test
    public void shouldBeAbleToParseAndValidateNonEncodedV2_1() throws Exception {
        expected.withV("2.1")
                .withSource(dto.createDto(Source.class)
                               .withProject(dto.createDto(ImportSourceDescriptor.class)
                                               .withType("git")
                                               .withLocation("location")
                                               .withParameters(new HashMap<String, String>() {
                                                   {
                                                       put("keepVcs", "true");
                                                       put("branch", "master");
                                                       put("commitId", "123");
                                                   }
                                               }))
                               .withRunners(singletonMap("runEnv", dto.createDto(RunnerSource.class)
                                                                      .withLocation("location")
                                                                      .withParameters(singletonMap("key", "value")))))
                .withProject(dto.createDto(NewProject.class)
                                .withType("type")
                                .withAttributes(singletonMap("key", singletonList("value")))
                                .withBuilders(dto.createDto(BuildersDescriptor.class).withDefault("default"))
                                .withDescription("description")
                                .withName("name")
                                .withRunners(dto.createDto(RunnersDescriptor.class)
                                                .withDefault("default")
                                                .withConfigs(singletonMap("key", dto.createDto(RunnerConfiguration.class)
                                                                                    .withRam(768)
                                                                                    .withOptions(new HashMap<String, String>() {
                                                                                        {
                                                                                            put("key1", "value1");
                                                                                            put("key2", "value2");
                                                                                        }
                                                                                    })
                                                                                    .withVariables(new HashMap<String, String>() {
                                                                                        {
                                                                                            put("key1", "value1");
                                                                                            put("key2", "value2");
                                                                                        }
                                                                                    }))))
                                .withVisibility("private"))
                .withCreator(dto.createDto(Author.class)
                                .withAccountId("accountId")
                                .withEmail("email")
                                .withName("name"))
                .withPolicies(dto.createDto(Policies.class)
                                 .withRefererHostname("referrer")
                                 .withValidSince(123l)
                                 .withValidUntil(123l))
                .withWorkspace(dto.createDto(Workspace.class)
                                  .withType("named"))
                .withIde(dto.createDto(Ide.class)
                            .withOnAppClosed(
                                    dto.createDto(OnAppClosed.class)
                                       .withActions(singletonList(dto.createDto(Action.class).withId("warnonclose"))))
                            .withOnAppLoaded(
                                    dto.createDto(OnAppLoaded.class)
                                       .withActions(singletonList(dto.createDto(Action.class).withId("newProject"))))
                            .withOnProjectOpened(dto.createDto(OnProjectOpened.class)
                                                    .withActions(Arrays.asList(
                                                            dto.createDto(Action.class)
                                                               .withId("openfile")
                                                               .withProperties(singletonMap("file", "pom.xml")),
                                                            dto.createDto(Action.class)
                                                               .withId("run"),
                                                            dto.createDto(Action.class)
                                                               .withId("findReplace")
                                                               .withProperties(ImmutableMap.of(
                                                                       "in", "src/main/resources/consts2.properties",
                                                                       "find", "OLD_VALUE_2",
                                                                       "replace", "NEW_VALUE_2")),
                                                            dto.createDto(Action.class)
                                                               .withId("openWelcomePage")
                                                               .withProperties(ImmutableMap.of(
                                                                       "authenticatedTitle",
                                                                       "Greeting title for authenticated users",
                                                                       "authenticatedContentUrl",
                                                                       "http://example.com/content.url"))))));


        StringBuilder sb = new StringBuilder("?");
        sb.append("v=2.1").append("&");
        sb.append("policies.refererHostname=referrer").append("&");
        sb.append("policies.validSince=123").append("&");
        sb.append("policies.validUntil=123").append("&");
        sb.append("creator.accountId=accountId").append("&");
        sb.append("creator.email=email").append("&");
        sb.append("creator.name=name").append("&");
        sb.append("workspace.type=named").append("&");
        sb.append("source.project.type=git").append("&");
        sb.append("source.project.location=location").append("&");
        sb.append("source.project.parameters.keepVcs=true").append("&");
        sb.append("source.project.parameters.commitId=123").append("&");
        sb.append("source.project.parameters.branch=master").append("&");
        sb.append("source.runners.runEnv.location=location").append("&");
        sb.append("source.runners.runEnv.parameters.key=value").append("&");
        sb.append("project.type=type").append("&");
        sb.append("project.name=name").append("&");
        sb.append("project.description=description").append("&");
        sb.append("project.attributes.key=value").append("&");
        sb.append("project.visibility=private").append("&");
        sb.append("project.builders.default=default").append("&");
        sb.append("project.runners.default=default").append("&");
        sb.append("project.runners.configs.key.ram=768").append("&");
        sb.append("project.runners.configs.key.options.key1=value1").append("&");
        sb.append("project.runners.configs.key.options.key2=value2").append("&");
        sb.append("project.runners.configs.key.variables.key1=value1").append("&");
        sb.append("project.runners.configs.key.variables.key2=value2").append("&");
        sb.append("ide.onProjectOpened.actions.%5B0%5D.id=openfile").append("&");
        sb.append("ide.onProjectOpened.actions.%5B0%5D.properties.file=pom.xml").append("&");
        sb.append("ide.onProjectOpened.actions.%5B1%5D.id=run").append("&");
        sb.append("ide.onProjectOpened.actions.%5B2%5D.id=findReplace").append("&");
        sb.append("ide.onProjectOpened.actions.%5B2%5D.properties.in=src%2Fmain%2Fresources%2Fconsts2.properties").append("&");
        sb.append("ide.onProjectOpened.actions.%5B2%5D.properties.find=OLD_VALUE_2").append("&");
        sb.append("ide.onProjectOpened.actions.%5B2%5D.properties.replace=NEW_VALUE_2").append("&");
        sb.append("ide.onProjectOpened.actions.%5B3%5D.id=openWelcomePage").append("&");
        sb.append("ide.onProjectOpened.actions.%5B3%5D.properties.authenticatedTitle=Greeting+title+for+authenticated+users").append("&");
        sb.append("ide.onProjectOpened.actions.%5B3%5D.properties.authenticatedContentUrl=http%3A%2F%2Fexample.com%2Fcontent.url")
          .append("&");
        sb.append("ide.onAppClosed.actions.%5B0%5D.id=warnonclose").append("&");
        sb.append("ide.onAppLoaded.actions.%5B0%5D.id=newProject");


        Factory newFactory = factoryBuilder.buildEncoded(new URI(sb.toString()));
        assertEquals(newFactory, expected);
    }


    @Test
    public void shouldBeAbleToValidateV2_0WithTrackedParamsWithoutAccountIdIfOnPremisesIsEnabled() throws Exception {
        factoryBuilder = new FactoryBuilder(sourceProjectParametersValidator);

        Factory factory = dto.createDto(Factory.class);
        factory.withV("2.0")
               .withSource(dto.createDto(Source.class)
                              .withProject(dto.createDto(ImportSourceDescriptor.class)
                                              .withType("git")
                                              .withLocation("location")))
               .withPolicies(dto.createDto(Policies.class)
                                .withRefererHostname("referrer")
                                .withValidSince(123l)
                                .withValidUntil(123l))
               .withActions(dto.createDto(Actions.class).withWelcome(dto.createDto(WelcomePage.class)));

        factoryBuilder.checkValid(factory, FactoryFormat.ENCODED);
    }
}
