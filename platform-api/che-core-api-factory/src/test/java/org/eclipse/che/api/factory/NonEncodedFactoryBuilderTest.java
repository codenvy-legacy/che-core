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

import org.eclipse.che.api.factory.dto.Action;
import org.eclipse.che.api.factory.dto.Actions;
import org.eclipse.che.api.factory.dto.Author;
import org.eclipse.che.api.factory.dto.Button;
import org.eclipse.che.api.factory.dto.ButtonAttributes;
import org.eclipse.che.api.factory.dto.Factory;
import org.eclipse.che.api.factory.dto.Ide;
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
import org.eclipse.che.commons.lang.URLEncodedUtils;
import org.eclipse.che.dto.server.DtoFactory;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Tests for {@link NonEncodedFactoryBuilder}
 *
 * @author Alexander Garagatyi
 */
public class NonEncodedFactoryBuilderTest {
    private static final String EXPECTED_2_0 = "v=2.0&" +
                                               "source.project.type=git&" +
                                               "source.project.location=http%3A%2F%2Fdev.box.com%2Fgit%2Fb4%2F13%2F9a%2Fworkspacednmsfgxl6rkwauu3%2Fspring&" +
                                               "source.project.parameters.commitId=123456&" +
                                               "source.project.parameters.branch=master&" +
                                               "source.project.parameters.keepDirectory=src&" +
                                               "source.project.parameters.keepVcs=true&" +
                                               "source.project.parameters.remoteOriginFetch=123456789&" +
                                               "source.runners.%2Fdocker%2Fenv_name1.location=http%3A%2F%2Fgoogle.com%2F123&" +
                                               "source.runners.%2Fdocker%2Fenv_name1.parameters.key2=value2&" +
                                               "source.runners.%2Fdocker%2Fenv_name1.parameters.key1=value1&" +
                                               "source.runners.%2Fdocker%2Fenv_name2.location=http%3A%2F%2Fgoogle.com%2F1234&" +
                                               "source.runners.%2Fdocker%2Fenv_name2.parameters.key4=value4&" +
                                               "source.runners.%2Fdocker%2Fenv_name2.parameters.key3=value3&" +
                                               "creator.name=Alex+Garagatyi&" +
                                               "creator.email=garagatyi%40gmail.com&" +
                                               "creator.accountId=account51xd02utpfvtp8zo&" +
                                               "workspace.type=named&" +
                                               "project.name=spring-sad&" +
                                               "project.description=A+basic+example+using+Spring+servlets.+The+app+returns+values+entered+into+a+submit+form.&" +
                                               "project.type=maven&" +
                                               "project.visibility=private&" +
                                               "project.builders.default=maven&" +
                                               "project.runners.default=tomcat7&" +
                                               "project.runners.configs.myEnv2.ram=768&" +
                                               "project.runners.configs.myEnv2.variables.var1=value1&" +
                                               "project.runners.configs.myEnv2.variables.var2=value2&" +
                                               "project.runners.configs.myEnv2.options.opt2=value2&" +
                                               "project.runners.configs.myEnv2.options.opt1=value1&" +
                                               "project.runners.configs.myEnv1.ram=256&" +
                                               "project.runners.configs.myEnv1.variables.var1=value1&" +
                                               "project.runners.configs.myEnv1.variables.var2=value2&" +
                                               "project.runners.configs.myEnv1.options.opt2=value2&" +
                                               "project.runners.configs.myEnv1.options.opt1=value1&" +
                                               "project.attributes.language=java&" +
                                               "policies.validSince=1413198747007&" +
                                               "policies.validUntil=2413192647001&" +
                                               "policies.refererHostname=dev.box.com&" +
                                               "actions.openFile=%2Fpom.xml&" +
                                               "actions.warnOnClose=true";

    private static final String EXPECTED_2_1 = "v=2.1&" +
                                               "source.project.type=git&" +
                                               "source.project.location=http%3A%2F%2Fdev.box.com%2Fgit%2Fb4%2F13%2F9a%2Fworkspacednmsfgxl6rkwauu3%2Fspring&" +
                                               "source.project.parameters.commitId=123456&" +
                                               "source.project.parameters.branch=master&" +
                                               "source.project.parameters.keepDirectory=src&" +
                                               "source.project.parameters.keepVcs=true&" +
                                               "project.name=spring-sad&" +
                                               "project.description=A+basic+example+using+Spring+servlets.+The+app+returns+values+entered+into+a+submit+form.&" +
                                               "project.type=maven&" +
                                               "project.visibility=private&" +
                                               "ide.onProjectOpened.actions.%5B0%5D.id=openFile&" +
                                               "ide.onProjectOpened.actions.%5B0%5D.properties.file=src%2Fmain%2Fjava%2Fcom%2Fcodenvy%2Fexample%2Fspring%2FHelloWorld.java";

    private NonEncodedFactoryBuilder factoryBuilder = new NonEncodedFactoryBuilder() {
        @Override
        protected String encode(String value) {
            try {
                return URLEncoder.encode(value, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                return value;
            }
        }

        @Override
        protected String toJson(List<ReplacementSet> dto) {
            return DtoFactory.getInstance().toJson(dto);
        }
    };

    @BeforeMethod
    public void setUp() throws Exception {

    }

    @Test
    public void shouldBeAbleToConvert2_0EncodedToNonEncoded() throws Exception {
        String nonEncFactory = factoryBuilder.buildNonEncoded(getFactory2_0());

        Set<String> expectedSet = new HashSet<>(Arrays.asList(EXPECTED_2_0.split("&")));
        Set<String> nonEncFactorySet = new HashSet<>(Arrays.asList(nonEncFactory.split("&")));

        assertEquals(expectedSet, nonEncFactorySet);
    }

    @Test
    public void shouldBeAbleToConvert2_1EncodedToNonEncoded() throws Exception {
        String nonEncFactory = factoryBuilder.buildNonEncoded(getFactory2_1());

        Set<String> expectedSet = new HashSet<>(Arrays.asList(EXPECTED_2_1.split("&")));
        Set<String> nonEncFactorySet = new HashSet<>(Arrays.asList(nonEncFactory.split("&")));

        assertEquals(expectedSet, nonEncFactorySet);
    }

    @Test
    public void shouldSkipWelcomePage() throws Exception {
        System.out.println(EXPECTED_2_1);

        final DtoFactory dto = DtoFactory.getInstance();
        final Factory factory2_0 = getFactory2_0();
        factory2_0.getActions().withWelcome(dto.createDto(WelcomePage.class)
                                               .withAuthenticated(dto.createDto(WelcomeConfiguration.class)
                                                                     .withTitle("nonauthenticated")
                                                                     .withNotification("nonauthnotification")
                                                                     .withIconurl("http://google.com/nonauth/icon")
                                                                     .withContenturl("http:google.com/nonauth/content"))
                                               .withNonauthenticated(dto.createDto(WelcomeConfiguration.class)
                                                                        .withTitle("authenticated")
                                                                        .withNotification("authnotification")
                                                                        .withIconurl("http://google.com/auth/icon")
                                                                        .withContenturl("http:google.com/auth/content")));
        String nonEncFactory = factoryBuilder.buildNonEncoded(factory2_0);

        Set<String> expectedSet = new HashSet<>(Arrays.asList(EXPECTED_2_0.split("&")));
        Set<String> nonEncFactorySet = new HashSet<>(Arrays.asList(nonEncFactory.split("&")));

        assertEquals(expectedSet, nonEncFactorySet);
    }

    @Test
    public void shouldSkipButton() throws Exception {

        final Factory factory = getFactory2_0().withButton(DtoFactory.getInstance().createDto(Button.class)
                                                                     .withType(Button.ButtonType.logo)
                                                                     .withAttributes(
                                                                             DtoFactory.getInstance().createDto(ButtonAttributes.class)
                                                                                       .withStyle("style")
                                                                                       .withLogo("logo")
                                                                                       .withCounter(true)
                                                                                       .withColor("logo")));
        String nonEncFactory = factoryBuilder.buildNonEncoded(factory);

        Set<String> expectedSet = new HashSet<>(Arrays.asList(EXPECTED_2_0.split("&")));
        Set<String> nonEncFactorySet = new HashSet<>(Arrays.asList(nonEncFactory.split("&")));

        assertEquals(expectedSet, nonEncFactorySet);
    }

    @Test
    public void shouldBeAbleToConvertFindReplaceToNonEncoded() throws Exception {
        final DtoFactory dto = DtoFactory.getInstance();
        final Factory factory2_0 = getFactory2_0();
        factory2_0.getActions().withFindReplace(new ArrayList<ReplacementSet>() {
            {
                add(dto.createDto(ReplacementSet.class)
                       .withFiles(singletonList("src/main/resources/*"))
                       .withEntries(singletonList(dto.createDto(Variable.class)
                                                     .withReplacemode("first")
                                                     .withReplace("NEW_VALUE")
                                                     .withFind("OLD_VALUE"))));
                add(dto.createDto(ReplacementSet.class)
                       .withFiles(Arrays.asList("src/main/resources/consts.properties",
                                                "src/main/resources/consts2.properties"))
                       .withEntries(Arrays.asList(dto.createDto(Variable.class)
                                                     .withReplacemode("first")
                                                     .withReplace("NEW_VALUE2")
                                                     .withFind("OLD_VALUE2"),
                                                  dto.createDto(Variable.class)
                                                     .withReplacemode("first")
                                                     .withReplace("NEW_VALUE3")
                                                     .withFind("OLD_VALUE3")
                                                 )));
            }
        });

        String nonEncFactory = factoryBuilder.buildNonEncoded(factory2_0);

        Map<String, Set<String>> queryParams = URLEncodedUtils.parse(new URI("?" + nonEncFactory), "UTF-8");
        final Set<String> findReplace = queryParams.get("actions.findReplace");
        assertNotNull(findReplace);
        assertEquals(findReplace.size(), 1);
        final List<ReplacementSet> variables = dto.createListDtoFromJson(findReplace.iterator().next(), ReplacementSet.class);
        assertEquals(variables, factory2_0.getActions().getFindReplace());
    }

    private Factory getFactory2_0() {
        final DtoFactory dto = DtoFactory.getInstance();
        return dto.createDto(Factory.class).withV("2.0")
                  .withSource(dto.createDto(Source.class)
                                 .withProject(dto.createDto(ImportSourceDescriptor.class)
                                                 .withType("git")
                                                 .withLocation("http://dev.box.com/git/b4/13/9a/workspacednmsfgxl6rkwauu3/spring")
                                                 .withParameters(new HashMap<String, String>() {
                                                     {
                                                         put("keepVcs", "true");
                                                         put("branch", "master");
                                                         put("commitId", "123456");
                                                         put("keepDirectory", "src");
                                                         put("remoteOriginFetch", "123456789");
                                                     }
                                                 }))
                                 .withRunners(new HashMap<String, RunnerSource>() {
                                     {
                                         put("/docker/env_name1", dto.createDto(RunnerSource.class)
                                                                     .withLocation("http://google.com/123")
                                                                     .withParameters(new HashMap<String, String>() {
                                                                         {
                                                                             put("key1", "value1");
                                                                             put("key2", "value2");
                                                                         }
                                                                     }));
                                         put("/docker/env_name2", dto.createDto(RunnerSource.class)
                                                                     .withLocation("http://google.com/1234")
                                                                     .withParameters(new HashMap<String, String>() {
                                                                         {
                                                                             put("key3", "value3");
                                                                             put("key4", "value4");
                                                                         }
                                                                     }));
                                     }
                                 }))
                  .withCreator(dto.createDto(Author.class)
                                  .withAccountId("account51xd02utpfvtp8zo")
                                  .withEmail("garagatyi@gmail.com")
                                  .withName("Alex Garagatyi"))
                  .withWorkspace(dto.createDto(Workspace.class)
                                    .withType("named"))
                  .withProject(dto.createDto(NewProject.class)
                                  .withName("spring-sad")
                                  .withDescription(
                                          "A basic example using Spring servlets. The app returns values entered into a submit form.")
                                  .withType("maven")
                                  .withAttributes(singletonMap("language", singletonList("java")))
                                  .withVisibility("private")
                                  .withBuilders(dto.createDto(BuildersDescriptor.class).withDefault("maven"))
                                  .withRunners(dto.createDto(RunnersDescriptor.class)
                                                  .withDefault("tomcat7")
                                                  .withConfigs(new HashMap<String, RunnerConfiguration>() {
                                                                   {
                                                                       put("myEnv1", dto.createDto(RunnerConfiguration.class)
                                                                                        .withRam(256)
                                                                                        .withOptions(new HashMap<String, String>() {
                                                                                            {
                                                                                                put("opt1", "value1");
                                                                                                put("opt2", "value2");
                                                                                            }
                                                                                        })
                                                                                        .withVariables(new HashMap<String, String>() {
                                                                                            {
                                                                                                put("var1", "value1");
                                                                                                put("var2", "value2");
                                                                                            }
                                                                                        }));
                                                                       put("myEnv2", dto.createDto(RunnerConfiguration.class)
                                                                                        .withRam(768)
                                                                                        .withOptions(new HashMap<String, String>() {
                                                                                            {
                                                                                                put("opt1", "value1");
                                                                                                put("opt2", "value2");
                                                                                            }
                                                                                        })
                                                                                        .withVariables(new HashMap<String, String>() {
                                                                                            {
                                                                                                put("var1", "value1");
                                                                                                put("var2", "value2");
                                                                                            }
                                                                                        }));
                                                                   }
                                                               }
                                                              )))
                  .withPolicies(dto.createDto(Policies.class)
                                   .withRefererHostname("dev.box.com")
                                   .withValidSince(1413198747007l)
                                   .withValidUntil(2413192647001l))
                  .withActions(dto.createDto(Actions.class)
                                  .withOpenFile("/pom.xml")
                                  .withWarnOnClose(true));
    }

    private Factory getFactory2_1() {
        final DtoFactory dto = DtoFactory.getInstance();
        return dto.createDto(Factory.class).withV("2.1")
                  .withSource(dto.createDto(Source.class)
                                 .withProject(dto.createDto(ImportSourceDescriptor.class)
                                                 .withType("git")
                                                 .withLocation("http://dev.box.com/git/b4/13/9a/workspacednmsfgxl6rkwauu3/spring")
                                                 .withParameters(new HashMap<String, String>() {
                                                     {
                                                         put("keepVcs", "true");
                                                         put("branch", "master");
                                                         put("commitId", "123456");
                                                         put("keepDirectory", "src");
                                                     }
                                                 })))
                  .withProject(dto.createDto(NewProject.class)
                                  .withName("spring-sad")
                                  .withDescription(
                                          "A basic example using Spring servlets. The app returns values entered into a submit form.")
                                  .withType("maven")
                                  .withVisibility("private"))
                  .withIde(dto.createDto(Ide.class)
                              .withOnProjectOpened(
                                      dto.createDto(OnProjectOpened.class)
                                         .withActions(Arrays.asList(
                                                 dto.createDto(Action.class)
                                                    .withId("openFile")
                                                    .withProperties(new HashMap<String, String>() {
                                                        {
                                                            put("file", "src/main/java/com/codenvy/example/spring/HelloWorld.java");
                                                        }
                                                    })))));

    }
}
