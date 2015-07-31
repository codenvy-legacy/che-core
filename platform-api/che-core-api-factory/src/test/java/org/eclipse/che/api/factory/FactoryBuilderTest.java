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
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Arrays;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
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
@SuppressWarnings("deprecation")
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
                               .withValidSince(123L)
                               .withValidUntil(123L))
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
        factoryBuilder.checkValid(actual);

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
                               .withValidSince(123L)
                               .withValidUntil(123L))
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

        factoryBuilder.checkValid(actual);

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
    public void shouldNotValidateUnparseableFactory() throws ApiException, URISyntaxException {
        factoryBuilder.checkValid(null);
    }

    @Test(expectedExceptions = ApiException.class, dataProvider = "setByServerParamsProvider",
            expectedExceptionsMessageRegExp = "You have provided an invalid parameter .* for this version of Factory parameters.*")
    public void shouldNotAllowUsingParamsThatCanBeSetOnlyByServer(Factory factory)
            throws InvocationTargetException, IllegalAccessException, ApiException, NoSuchMethodException {
        factoryBuilder.checkValid(factory);
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
                {dto.clone(v2).withCreator(dto.createDto(Author.class).withCreated(123L))}
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
                                            .withValidSince(123L)
                                            .withValidUntil(123L))
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
                                 .withValidSince(123L)
                                 .withValidUntil(123L))
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
    public void shouldNotAllowUsingNotValidParams(Factory factory)
            throws InvocationTargetException, IllegalAccessException, ApiException, NoSuchMethodException {
        factoryBuilder.checkValid(factory);
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
                                .withValidSince(123L)
                                .withValidUntil(123L))
               .withActions(dto.createDto(Actions.class).withWelcome(dto.createDto(WelcomePage.class)));

        factoryBuilder.checkValid(factory);
    }
}
