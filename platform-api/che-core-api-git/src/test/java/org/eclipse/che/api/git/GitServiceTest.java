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
package org.eclipse.che.api.git;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import org.eclipse.che.api.git.shared.ConfigRequest;
import org.eclipse.che.api.vfs.server.MountPoint;
import org.eclipse.che.api.vfs.server.VirtualFileSystem;
import org.eclipse.che.api.vfs.server.VirtualFileSystemProvider;
import org.eclipse.che.api.vfs.server.VirtualFileSystemRegistry;
import org.eclipse.che.api.vfs.shared.PropertyFilter;
import org.eclipse.che.api.vfs.shared.dto.Item;
import org.eclipse.che.commons.json.JsonHelper;
import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.vfs.impl.fs.LocalPathResolver;
import org.eclipse.che.vfs.impl.fs.VirtualFileImpl;
import org.everrest.assured.EverrestJetty;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * @author <a href="mailto:offershostak@gmail.com">Offer Shostak</a>
 */
@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class GitServiceTest {

    private final String SERVICE_PATH = "/git/ws1";
    private final String PROJECT_PATH_PARAM_NAME = "projectPath";

    @Mock
    private GitConnectionFactory gitConnectionFactory;
    @Mock
    private GitConnection gitConnection;
    @Mock
    private Config config;
    @Mock
    private VirtualFileSystemRegistry vfsRegistry;
    @Mock
    private VirtualFileSystem vfs;
    @Mock
    private VirtualFileSystemProvider vfsp;
    @Mock
    private LocalPathResolver localPathResolver;
    @InjectMocks
    private GitService gitService;
    private String projectPath = "/watt_il_test_new";
    private String vfsId = "ws1";
    private DtoFactory dto;

    @BeforeMethod
    public void setUp() throws Exception {
        dto = DtoFactory.getInstance();
        when(vfsRegistry.getProvider(vfsId)).thenReturn(vfsp);
        when(vfsRegistry.getProvider(vfsId).newInstance(null)).thenReturn(vfs);
        Item mockedItem = Mockito.mock(Item.class);
        when(vfs.getItemByPath(projectPath, null, false, PropertyFilter.ALL_FILTER)).thenReturn(mockedItem);
        MountPoint mockedMountPoint = Mockito.mock(MountPoint.class);
        when(vfs.getMountPoint()).thenReturn(mockedMountPoint);
        VirtualFileImpl mockedVirtualFileImpl = Mockito.mock(VirtualFileImpl.class);
        when(mockedMountPoint.getVirtualFile(mockedItem.getPath())).thenReturn(mockedVirtualFileImpl);
        when(localPathResolver.resolve(mockedVirtualFileImpl)).thenReturn(projectPath);
        when(gitConnectionFactory.getConnection(projectPath)).thenReturn(gitConnection);
        when(gitConnection.getConfig()).thenReturn(config);
    }

    @Test
    public void shouldReturnStatus200OnGetConfigWithNoRequestedConfig() throws Exception {
        // given
        List<String> configList = new ArrayList<>();
        configList.add("xxx.aaa=1");
        configList.add("xxx.bbb=2");
        when(config.getList()).thenReturn(configList);

        // when
        Response response = given().//
                when().//
                get(SERVICE_PATH + "/config?" + PROJECT_PATH_PARAM_NAME + "=/watt_il_test_new");

        // then
        assertEquals(response.getStatusCode(), 200);
        String expectedString1 = "\"xxx.aaa\":\"1\"";
        String expectedString2 = "\"xxx.bbb\":\"2\"";
        assertThat(response.getBody().asString(), containsString(expectedString1));
        assertThat(response.getBody().asString(), containsString(expectedString2));
    }

    @Test
    public void shouldReturnStatus200OnGetConfigWithRequestedConfig() throws Exception {
        // given
        when(config.get("myKey")).thenReturn("myValue");

        // when
        Response response = given().//
                when().//
                get(
                SERVICE_PATH + "/config?" + PROJECT_PATH_PARAM_NAME + "=/watt_il_test_new" + "&requestedConfig=myKey");

        // then
        assertEquals(response.getStatusCode(), 200);
        String expectedString = "\"myKey\":\"myValue\"";
        assertThat(response.getBody().asString(), containsString(expectedString));
    }

    @Test
    public void shouldReturnStatus200OnSetConfigWithRequestedConfigNotExist() throws Exception {
        // given
        when(config.get("myKey2")).thenThrow(new GitException("not exist"));

        // when
        Response response = given().//
                when().//
                get(
                SERVICE_PATH + "/config?" + PROJECT_PATH_PARAM_NAME + "=/watt_il_test_new" + "&requestedConfig=myKey2");

        // then
        assertEquals(response.getStatusCode(), 200);
        String expectedString = "";
        assertThat(response.getBody().asString(), containsString(expectedString));
    }

    @Test
    public void shouldReturnStatus204OnSetConfigWithBody() throws Exception {
        // given
        Map<String, String> configEntries = new HashMap<>();
        configEntries.put("gerrit" + "." + "createchangeid", "true");
        ConfigRequest configRequest = dto.createDto(ConfigRequest.class).withConfigEntries(configEntries);

        // when
        Response response = given().//
                when().//
                contentType(ContentType.JSON).
                body(JsonHelper.toJson(configRequest)).
                put(SERVICE_PATH + "/config?" + PROJECT_PATH_PARAM_NAME + "=/watt_il_test_new");

        // then
        assertEquals(response.getStatusCode(), 204);
    }

    @Test
    public void shouldReturnStatus500OnSetConfigWithNoBody() throws Exception {
        // when
        Response response = given().//
                when().//
                contentType(ContentType.JSON).
                body(JsonHelper.toJson("")).
                put(SERVICE_PATH + "/config?" + PROJECT_PATH_PARAM_NAME + "=/watt_il_test_new");

        // then
        assertEquals(response.getStatusCode(), 500);
    }

    @Test
    public void shouldReturnStatus204OnUnsetConfigWithRequestedConfig() throws Exception {
        // when
        Response response = given().//
                when().//
                delete(SERVICE_PATH + "/config?" + PROJECT_PATH_PARAM_NAME + "=/watt_il_test_new"
                + "&requestedConfig=xxx.aaa");

        // then
        assertEquals(response.getStatusCode(), 204);
    }

    @Test
    public void shouldReturnStatus204OnUnsetConfigWithNoRequestedConfig() throws Exception {
        // when
        Response response = given().//
                when().//
                delete(SERVICE_PATH + "/config?" + PROJECT_PATH_PARAM_NAME + "=/watt_il_test_new");

        // then
        assertEquals(response.getStatusCode(), 204);
    }
}
