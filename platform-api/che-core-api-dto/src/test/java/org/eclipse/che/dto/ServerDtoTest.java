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
package org.eclipse.che.dto;

import org.eclipse.che.dto.definitions.ComplicatedDto;
import org.eclipse.che.dto.definitions.DtoWithAny;
import org.eclipse.che.dto.definitions.DtoWithDelegate;
import org.eclipse.che.dto.definitions.DtoWithFieldNames;
import org.eclipse.che.dto.definitions.SimpleDto;
import org.eclipse.che.dto.server.DtoFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests that the interfaces specified in org.eclipse.che.dto.definitions have
 * corresponding generated server implementations.
 *
 * @author <a href="mailto:azatsarynnyy@codenvy.com">Artem Zatsarynnyy</a>
 */
public class ServerDtoTest {

    protected final static DtoFactory dtoFactory = DtoFactory.getInstance();

    @Test
    public void testCreateSimpleDto() throws Exception {
        final String fooString = "Something";
        final int fooId = 1;
        final String _default = "test_default_keyword";

        SimpleDto dto = dtoFactory.createDto(SimpleDto.class).withName(fooString).withId(fooId).withDefault(_default);

        // Check to make sure things are in a sane state.
        checkSimpleDto(dto, fooString, fooId, _default);
    }

    @Test
    public void testSimpleDtoSerializer() throws Exception {
        final String fooString = "Something";
        final int fooId = 1;
        final String _default = "test_default_keyword";

        SimpleDto dto = dtoFactory.createDto(SimpleDto.class).withName(fooString).withId(fooId).withDefault(_default);
        final String json = dtoFactory.toJson(dto);

        JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();
        Assert.assertEquals(jsonObject.get("name").getAsString(), fooString);
        Assert.assertEquals(jsonObject.get("id").getAsInt(), fooId);
        Assert.assertEquals(jsonObject.get("default").getAsString(), _default);
    }

    @Test
    public void testSimpleDtoDeserializer() throws Exception {
        final String fooString = "Something";
        final int fooId = 1;
        final String _default = "test_default_keyword";

        JsonObject json = new JsonObject();
        json.add("name", new JsonPrimitive(fooString));
        json.add("id", new JsonPrimitive(fooId));
        json.add("default", new JsonPrimitive(_default));

        SimpleDto dto = dtoFactory.createDtoFromJson(json.toString(), SimpleDto.class);

        // Check to make sure things are in a sane state.
        checkSimpleDto(dto, fooString, fooId, _default);
    }

    @Test
    public void testSerializerWithFieldNames() throws Exception {
        final String fooString = "Something";
        final String _default = "test_default_keyword";

        DtoWithFieldNames dto = dtoFactory.createDto(DtoWithFieldNames.class).withTheName(fooString).withTheDefault(_default);
        final String json = dtoFactory.toJson(dto);

        JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();
        Assert.assertEquals(jsonObject.get(DtoWithFieldNames.THENAME_FIELD).getAsString(), fooString);
        Assert.assertEquals(jsonObject.get(DtoWithFieldNames.THEDEFAULT_FIELD).getAsString(), _default);
    }

    @Test
    public void testDeerializerWithFieldNames() throws Exception {
        final String fooString = "Something";
        final String _default = "test_default_keyword";

        JsonObject json = new JsonObject();
        json.add(DtoWithFieldNames.THENAME_FIELD, new JsonPrimitive(fooString));
        json.add(DtoWithFieldNames.THEDEFAULT_FIELD, new JsonPrimitive(_default));

        DtoWithFieldNames dto = dtoFactory.createDtoFromJson(json.toString(), DtoWithFieldNames.class);

        Assert.assertEquals(dto.getTheName(), fooString);
        Assert.assertEquals(dto.getTheDefault(), _default);
    }

    @Test
    public void testSerializerWithAny() throws Exception {
        DtoWithAny dto = dtoFactory.createDto(DtoWithAny.class).withStuff(createTestValueForAny());
        final String json = dtoFactory.toJson(dto);

        JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();
        Assert.assertEquals(jsonObject.get("stuff"), createTestValueForAny());
    }

    @Test
    public void testDeerializerWithAny() throws Exception {
        final JsonElement stuff = createTestValueForAny();

        JsonObject json = new JsonObject();
        json.add("stuff", stuff);

        DtoWithAny dto = dtoFactory.createDtoFromJson(json.toString(), DtoWithAny.class);

        Assert.assertEquals(dto.getStuff(), createTestValueForAny());
    }

    /** Intentionally call several times to ensure non-reference equality */
    private static JsonElement createTestValueForAny() {
        return new JsonParser().parse("{a:100,b:{c:'blah',d:null}}");
    }

    @Test
    public void testListSimpleDtoDeserializer() throws Exception {
        final String fooString_1 = "Something 1";
        final int fooId_1 = 1;
        final String _default_1 = "test_default_keyword_1";
        final String fooString_2 = "Something 2";
        final int fooId_2 = 2;
        final String _default_2 = "test_default_keyword_2";

        JsonObject json1 = new JsonObject();
        json1.add("name", new JsonPrimitive(fooString_1));
        json1.add("id", new JsonPrimitive(fooId_1));
        json1.add("default", new JsonPrimitive(_default_1));

        JsonObject json2 = new JsonObject();
        json2.add("name", new JsonPrimitive(fooString_2));
        json2.add("id", new JsonPrimitive(fooId_2));
        json2.add("default", new JsonPrimitive(_default_2));

        JsonArray jsonArray = new JsonArray();
        jsonArray.add(json1);
        jsonArray.add(json2);

        org.eclipse.che.dto.shared.JsonArray<SimpleDto> listDtoFromJson =
                dtoFactory.createListDtoFromJson(jsonArray.toString(), SimpleDto.class);

        Assert.assertEquals(listDtoFromJson.get(0).getName(), fooString_1);
        Assert.assertEquals(listDtoFromJson.get(0).getId(), fooId_1);
        Assert.assertEquals(listDtoFromJson.get(0).getDefault(), _default_1);

        Assert.assertEquals(listDtoFromJson.get(1).getName(), fooString_2);
        Assert.assertEquals(listDtoFromJson.get(1).getId(), fooId_2);
        Assert.assertEquals(listDtoFromJson.get(1).getDefault(), _default_2);
    }

    @Test
    public void testComplicatedDtoSerializer() throws Exception {
        final String fooString = "Something";
        final int fooId = 1;
        final String _default = "test_default_keyword";

        List<String> listStrings = new ArrayList<>(2);
        listStrings.add("Something 1");
        listStrings.add("Something 2");

        ComplicatedDto.SimpleEnum simpleEnum = ComplicatedDto.SimpleEnum.ONE;

        // Assume that SimpleDto works. Use it to test nested objects
        SimpleDto simpleDto = dtoFactory.createDto(SimpleDto.class).withName(fooString).withId(fooId).withDefault(_default);

        Map<String, SimpleDto> mapDtos = new HashMap<>(1);
        mapDtos.put(fooString, simpleDto);

        List<SimpleDto> listDtos = new ArrayList<>(1);
        listDtos.add(simpleDto);

        List<List<ComplicatedDto.SimpleEnum>> listOfListOfEnum = new ArrayList<>(1);
        List<ComplicatedDto.SimpleEnum> listOfEnum = new ArrayList<>(3);
        listOfEnum.add(ComplicatedDto.SimpleEnum.ONE);
        listOfEnum.add(ComplicatedDto.SimpleEnum.TWO);
        listOfEnum.add(ComplicatedDto.SimpleEnum.THREE);
        listOfListOfEnum.add(listOfEnum);

        ComplicatedDto dto = dtoFactory.createDto(ComplicatedDto.class).withStrings(listStrings).
                withSimpleEnum(simpleEnum).withMap(mapDtos).withSimpleDtos(listDtos).
                                               withArrayOfArrayOfEnum(listOfListOfEnum);


        final String json = dtoFactory.toJson(dto);
        JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();

        Assert.assertTrue(jsonObject.has("strings"));
        JsonArray jsonArray = jsonObject.get("strings").getAsJsonArray();
        Assert.assertEquals(jsonArray.get(0).getAsString(), listStrings.get(0));
        Assert.assertEquals(jsonArray.get(1).getAsString(), listStrings.get(1));

        Assert.assertTrue(jsonObject.has("simpleEnum"));
        Assert.assertEquals(jsonObject.get("simpleEnum").getAsString(), simpleEnum.name());

        Assert.assertTrue(jsonObject.has("map"));
        JsonObject jsonMap = jsonObject.get("map").getAsJsonObject();
        JsonObject value = jsonMap.get(fooString).getAsJsonObject();
        Assert.assertEquals(value.get("name").getAsString(), fooString);
        Assert.assertEquals(value.get("id").getAsInt(), fooId);
        Assert.assertEquals(value.get("default").getAsString(), _default);

        Assert.assertTrue(jsonObject.has("simpleDtos"));
        JsonArray simpleDtos = jsonObject.get("simpleDtos").getAsJsonArray();
        JsonObject simpleDtoJsonObject = simpleDtos.get(0).getAsJsonObject();
        Assert.assertEquals(simpleDtoJsonObject.get("name").getAsString(), fooString);
        Assert.assertEquals(simpleDtoJsonObject.get("id").getAsInt(), fooId);
        Assert.assertEquals(simpleDtoJsonObject.get("default").getAsString(), _default);

        Assert.assertTrue(jsonObject.has("arrayOfArrayOfEnum"));
        JsonArray arrayOfArrayOfEnum = jsonObject.get("arrayOfArrayOfEnum").getAsJsonArray().get(0).getAsJsonArray();
        Assert.assertEquals(arrayOfArrayOfEnum.get(0).getAsString(), ComplicatedDto.SimpleEnum.ONE.name());
        Assert.assertEquals(arrayOfArrayOfEnum.get(1).getAsString(), ComplicatedDto.SimpleEnum.TWO.name());
        Assert.assertEquals(arrayOfArrayOfEnum.get(2).getAsString(), ComplicatedDto.SimpleEnum.THREE.name());
    }

    @Test
    public void testComplicatedDtoDeserializer() throws Exception {
        final String fooString = "Something";
        final int fooId = 1;
        final String _default = "test_default_keyword";

        JsonArray jsonArray = new JsonArray();
        jsonArray.add(new JsonPrimitive(fooString));

        JsonObject simpleDtoJsonObject = new JsonObject();
        simpleDtoJsonObject.add("name", new JsonPrimitive(fooString));
        simpleDtoJsonObject.add("id", new JsonPrimitive(fooId));
        simpleDtoJsonObject.add("default", new JsonPrimitive(_default));

        JsonObject jsonMap = new JsonObject();
        jsonMap.add(fooString, simpleDtoJsonObject);

        JsonArray simpleDtosArray = new JsonArray();
        simpleDtosArray.add(simpleDtoJsonObject);

        JsonArray arrayOfEnum = new JsonArray();
        arrayOfEnum.add(new JsonPrimitive(ComplicatedDto.SimpleEnum.ONE.name()));
        arrayOfEnum.add(new JsonPrimitive(ComplicatedDto.SimpleEnum.TWO.name()));
        arrayOfEnum.add(new JsonPrimitive(ComplicatedDto.SimpleEnum.THREE.name()));
        JsonArray arrayOfArrayEnum = new JsonArray();
        arrayOfArrayEnum.add(arrayOfEnum);

        JsonObject complicatedDtoJsonObject = new JsonObject();
        complicatedDtoJsonObject.add("strings", jsonArray);
        complicatedDtoJsonObject.add("simpleEnum", new JsonPrimitive(ComplicatedDto.SimpleEnum.ONE.name()));
        complicatedDtoJsonObject.add("map", jsonMap);
        complicatedDtoJsonObject.add("simpleDtos", simpleDtosArray);
        complicatedDtoJsonObject.add("arrayOfArrayOfEnum", arrayOfArrayEnum);

        ComplicatedDto complicatedDto =
                dtoFactory.createDtoFromJson(complicatedDtoJsonObject.toString(), ComplicatedDto.class);

        Assert.assertEquals(complicatedDto.getStrings().get(0), fooString);
        Assert.assertEquals(complicatedDto.getSimpleEnum(), ComplicatedDto.SimpleEnum.ONE);
        checkSimpleDto(complicatedDto.getMap().get(fooString), fooString, fooId, _default);
        checkSimpleDto(complicatedDto.getSimpleDtos().get(0), fooString, fooId, _default);
        Assert.assertEquals(complicatedDto.getArrayOfArrayOfEnum().get(0).get(0), ComplicatedDto.SimpleEnum.ONE);
        Assert.assertEquals(complicatedDto.getArrayOfArrayOfEnum().get(0).get(1), ComplicatedDto.SimpleEnum.TWO);
        Assert.assertEquals(complicatedDto.getArrayOfArrayOfEnum().get(0).get(2), ComplicatedDto.SimpleEnum.THREE);
    }

    private void checkSimpleDto(SimpleDto dto, String expectedName, int expectedId, String expectedDefault) {
        Assert.assertEquals(dto.getName(), expectedName);
        Assert.assertEquals(dto.getId(), expectedId);
        Assert.assertEquals(dto.getDefault(), expectedDefault);
    }

    @Test
    public void testDelegate() {
        Assert.assertEquals(DtoFactory.getInstance().createDto(DtoWithDelegate.class).withName("TEST").nameWithPrefix("### "), "### TEST");
    }
}
