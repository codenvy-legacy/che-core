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
package org.eclipse.che.api.project.server;

import org.eclipse.che.api.project.shared.Builders;
import org.eclipse.che.api.project.shared.dto.BuilderConfiguration;
import org.eclipse.che.api.project.shared.dto.BuildersDescriptor;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.vfs.shared.dto.AccessControlEntry;
import org.eclipse.che.api.vfs.shared.dto.Principal;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.commons.user.User;
import org.everrest.core.impl.uri.UriBuilderImpl;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * @author Vitaly Parfonov
 */
public class DtoConverterTest {


    /**
     * Check user permission is based on userID and not username.
     * @throws Exception if something is going wrong
     */
    @Test
    public void toProjectsDescriptorUserPermissionID() throws Exception {

        String userId = "florentID";
        String userName = "florent";

        List<String> permissions = Arrays.asList("read", "write");


        // setup project
        Project project = mock(Project.class);

        // project permissions
        AccessControlEntry accessControlEntry = mock(AccessControlEntry.class);
        List<AccessControlEntry> acl = Arrays.asList(accessControlEntry);
        doReturn(acl).when(project).getPermissions();
        Principal principal = mock(Principal.class);
        doReturn(principal).when(accessControlEntry).getPrincipal();
        doReturn(permissions).when(accessControlEntry).getPermissions();

        // VFS permissions are set on user id
        doReturn(Principal.Type.USER).when(principal).getType();
        doReturn(userId).when(principal).getName();


        // environment context
        EnvironmentContext customEnvironment = mock(EnvironmentContext.class);
        User user = mock(User.class);
        doReturn(user).when(customEnvironment).getUser();
        doReturn(userId).when(user).getId();
        doReturn(userName).when(user).getName();

        // launch convert and before set env context
        EnvironmentContext old = EnvironmentContext.getCurrent();
        ProjectDescriptor projectDescriptor;
        try {
            EnvironmentContext.setCurrent(customEnvironment);
             projectDescriptor = DtoConverter.toDescriptorDto2(project, null, new UriBuilderImpl(), null, "workspace");
        } finally {
            // reset
            EnvironmentContext.setCurrent(old);
        }

        Assert.assertNotNull(projectDescriptor);
        Assert.assertEquals(permissions, projectDescriptor.getPermissions());
    }


    @Test
    public void buildersFromDtoBuildersDescriptor() {
        //prepare
        String optionsKey = NameGenerator.generate("optionsKey",5);
        String optionsValue = NameGenerator.generate("optionsValue",5);
        String optionsKey1 = NameGenerator.generate("optionsKey",5);
        String optionsValue1 = NameGenerator.generate("optionsValue",5);
        String optionsKey2 = NameGenerator.generate("optionsKey",5);
        String optionsValue2 = NameGenerator.generate("optionsValue",5);
        Map<String, String> options = new HashMap<>(3);
        options.put(optionsKey,optionsValue);
        options.put(optionsKey1,optionsValue1);
        options.put(optionsKey2,optionsValue2);

        String target1 = NameGenerator.generate("target",5);
        String target2 = NameGenerator.generate("target",5);
        List<String> targets = new ArrayList<>(2);
        targets.add(target1);
        targets.add(target2);

        BuilderConfiguration builderConfiguration = mock(BuilderConfiguration.class);
        when(builderConfiguration.getOptions()).thenReturn(options);
        when(builderConfiguration.getTargets()).thenReturn(targets);

        Map<String, BuilderConfiguration> configurationMap =  new HashMap<>();
        String confName = NameGenerator.generate("conf",5);
        configurationMap.put(confName, builderConfiguration);

        String defaultBuilder  = NameGenerator.generate("builder",5);
        BuildersDescriptor buildersDescriptor = mock(BuildersDescriptor.class);
        when(buildersDescriptor.getConfigs()).thenReturn(configurationMap);
        when(buildersDescriptor.getDefault()).thenReturn(defaultBuilder);

        //check
        Builders builders = DtoConverter.fromDto(buildersDescriptor);
        Assert.assertNotNull(builders);
        Assert.assertEquals(defaultBuilder, builders.getDefault());

        Builders.Config config = builders.getConfig(confName);
        Assert.assertNotNull(config);
        Assert.assertNotNull(config.getTargets());
        Assert.assertEquals(2, config.getTargets().size());
        Assert.assertTrue(config.getTargets().contains(target1));
        Assert.assertTrue(config.getTargets().contains(target2));

        Assert.assertNotNull(config.getOptions());
        Assert.assertEquals(3, config.getOptions().size());
        Assert.assertTrue(config.getOptions().containsKey(optionsKey));
        Assert.assertTrue(config.getOptions().containsKey(optionsKey1));
        Assert.assertTrue(config.getOptions().containsKey(optionsKey2));

        Assert.assertTrue(config.getOptions().containsValue(optionsValue));
        Assert.assertTrue(config.getOptions().containsValue(optionsValue1));
        Assert.assertTrue(config.getOptions().containsValue(optionsValue2));
    }
}
