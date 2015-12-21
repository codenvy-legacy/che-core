/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Codenvy, S.A. - initial API and implementation
 *******************************************************************************/

package org.eclipse.che.dto.generator;

import org.eclipse.che.dto.server.JsonArrayImpl;
import org.eclipse.che.dto.server.JsonSerializable;
import org.eclipse.che.dto.server.JsonStringMapImpl;
import org.eclipse.che.dto.shared.DelegateRule;
import org.eclipse.che.dto.shared.DTOImpl;
import org.eclipse.che.dto.shared.DelegateTo;
import org.eclipse.che.dto.shared.JsonArray;
import org.eclipse.che.dto.shared.JsonStringMap;
import org.eclipse.che.dto.shared.SerializationIndex;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Primitives;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/** Generates the source code for a generated Server DTO impl. */
public class DtoImplServerTemplate extends DtoImpl {
    private static final String JSON_ARRAY_IMPL   = JsonArrayImpl.class.getCanonicalName();
    private static final String JSON_MAP_IMPL     = JsonStringMapImpl.class.getCanonicalName();
    private static final String SERVER_DTO_MARKER = "  @" + DTOImpl.class.getCanonicalName() + "(\"server\")\n";

    DtoImplServerTemplate(DtoTemplate template, Class<?> superInterface) {
        super(template, superInterface);
    }

    @Override
    String serialize() {
        StringBuilder builder = new StringBuilder();
        final Class<?> dtoInterface = getDtoInterface();
        final String dtoInterfaceName = dtoInterface.getCanonicalName();
        emitPreamble(dtoInterface, builder);
        List<Method> getters = getDtoGetters(dtoInterface);
        // Enumerate the getters and emit field names and getters + setters.
        emitFields(getters, builder);
        emitGettersAndSetters(getters, builder);
        List<Method> inheritedGetters = getInheritedDtoGetters(dtoInterface);
        List<Method> methods = new ArrayList<>();
        methods.addAll(getters);
        Set<String> getterNames = new HashSet<>();
        for (Method getter : getters) {
            getterNames.add(getter.getName());
        }
        for (Method getter : inheritedGetters) {
            if (getterNames.add(getter.getName())) {
                methods.add(getter);
            }
        }
        // equals, hashCode, serialization and copy constructor
        emitEqualsAndHashCode(methods, builder);
        emitSerializer(methods, builder);
        emitDeserializer(methods, builder);
        emitDeserializerShortcut(builder);
        emitCopyConstructor(methods, builder);
        // Delegation DTO methods.
        emitDelegateMethods(builder);
        // "builder" method, it is method that set field and return "this" instance
        emitWithMethods(getters, dtoInterfaceName, builder);
        // Implement withXXX methods that are declared directly in this DTO even if there are no any getter for the fields.
        // Need that to override methods from super DTO and return correct type for with method.
        // @DTO
        // public interface A {
        //     String getProperty();
        //     void setProperty(String property);
        //     A withProperty();
        // }
        //
        // @DTO
        // public interface B extends A {
        //     // New methods
        //     ...
        //     // Override method to return type B instead of A.
        //     B withProperty();
        // }
        getterNames.clear();
        for (Method getter : getters) {
            getterNames.add(getter.getName());
        }
        for (Method method : dtoInterface.getDeclaredMethods()) {
            if (!method.isDefault() && method.getName().startsWith("with")) {
                String noPrefixName = method.getName().substring(4);
                // Check do we already generate withXXX method or not.
                // If there is getter in DTO interface we already have withXXX method
                if (!getterNames.contains("get" + noPrefixName) && !getterNames.contains("is" + noPrefixName)) {
                    String fieldName = Character.toLowerCase(noPrefixName.charAt(0)) + noPrefixName.substring(1);
                    String parameterFqn = getFqParameterizedName(method.getGenericParameterTypes()[0]);
                    emitWithMethod(method.getName(), fieldName, parameterFqn, dtoInterfaceName, builder);
                }
            }
        }
        emitPostamble(builder);
        return builder.toString();
    }

    private void emitEqualsAndHashCode(List<Method> getters, StringBuilder builder) {
        builder.append("    @Override\n");
        builder.append("    public boolean equals(Object o) {\n");
        builder.append("      if (!(o instanceof ").append(getImplClassName()).append(")) {\n");
        builder.append("        return false;\n");
        builder.append("      }\n");
        builder.append("      ").append(getImplClassName()).append(" other = (").append(getImplClassName()).append(") o;\n");
        List<Method> comparableGetters =  getComparableMethods(getters);
        if (comparableGetters.isEmpty()) {
            comparableGetters = getters;
        }
        for (Method getter : comparableGetters) {
            String fieldName = getJavaFieldName(getter.getName());
            Class<?> returnType = getter.getReturnType();
            if (returnType.isPrimitive()) {
                builder.append("      if (this.").append(fieldName).append(" != other.").append(fieldName).append(") {\n");
                builder.append("        return false;\n");
                builder.append("      }\n");
            } else {
                if (isList(returnType) || isMap(returnType)) {
                    builder.append("      ").append("this.").append(getEnsureName(fieldName)).append("();\n");
                    builder.append("      ").append("other.").append(getEnsureName(fieldName)).append("();\n");
                    builder.append("      \n");
                }
                builder.append("      if (this.").append(fieldName).append(" != null) {\n");
                builder.append("        if (!this.").append(fieldName).append(".equals(other.").append(fieldName).append(")) {\n");
                builder.append("          return false;\n");
                builder.append("        }\n");
                builder.append("      } else {\n");
                builder.append("        if (other.").append(fieldName).append(" != null) {\n");
                builder.append("          return false;\n");
                builder.append("        }\n");
                builder.append("      }\n");
            }
        }
        builder.append("      return true;\n");
        builder.append("    }\n\n");

        // this isn't the greatest hash function in the world, but it meets the requirement that for any
        // two objects A and B, A.equals(B) only if A.hashCode() == B.hashCode()
        builder.append("    @Override\n");
        builder.append("    public int hashCode() {\n");
        builder.append("      int hash = 7;\n");
        for (Method method : comparableGetters) {
            Class<?> type = method.getReturnType();
            String fieldName = getJavaFieldName(method.getName());
            if (type.isPrimitive()) {
                Class<?> wrappedType = Primitives.wrap(type);
                builder.append("      hash = hash * 31 + ").append(wrappedType.getName()).append(".valueOf(").append(fieldName)
                       .append(").hashCode();\n");
            } else {
                if (isList(type) || isMap(type)) {
                    builder.append("      ").append(getEnsureName(fieldName)).append("();\n");
                }
                builder.append("      hash = hash * 31 + (").append(fieldName).append(" != null ? ").append(fieldName).append(
                        ".hashCode() : 0);\n");
            }
        }
        builder.append("      return hash;\n");
        builder.append("    }\n\n");
    }

    private void emitFactoryMethod(StringBuilder builder) {
        builder.append("    public static ");
        builder.append(getImplClassName());
        builder.append(" make() {");
        builder.append("\n        return new ");
        builder.append(getImplClassName());
        builder.append("();\n    }\n\n");
    }

    private void emitFields(List<Method> getters, StringBuilder builder) {
        for (Method getter : getters) {
            String fieldName = getJavaFieldName(getter.getName());
            builder.append("    ");
            builder.append(getFieldTypeAndAssignment(getter, fieldName));
        }
        builder.append("\n");
    }

    /** Emits a method to get a field. Getting a collection ensures that the collection is created. */
    private void emitGetter(Method getter, String fieldName, String returnType, StringBuilder builder) {
        if (getter.isAnnotationPresent(javax.validation.constraints.NotNull.class)) {
            builder.append("    @javax.validation.constraints.NotNull\n");
        } else if (getter.isAnnotationPresent(org.eclipse.che.commons.annotation.Nullable.class)) {
            builder.append("    @org.eclipse.che.commons.annotation.Nullable\n");
        }
        builder.append("    @Override\n    public ");
        builder.append(returnType);
        builder.append(" ");
        builder.append(getter.getName());
        builder.append("() {\n");
        // Initialize the collection.
        Class<?> returnTypeClass = getter.getReturnType();
        if (isList(returnTypeClass) || isMap(returnTypeClass)) {
            builder.append("      ");
            builder.append(getEnsureName(fieldName));
            builder.append("();\n");
        }
        builder.append("      return ");
        emitReturn(getter, fieldName, builder);
        builder.append(";\n    }\n\n");
    }

    private void emitGettersAndSetters(List<Method> getters, StringBuilder builder) {
        for (Method getter : getters) {
            String fieldName = getJavaFieldName(getter.getName());
            if (fieldName == null) {
                continue;
            }
            Class<?> returnTypeClass = getter.getReturnType();
            String returnType = getFqParameterizedName(getter.getGenericReturnType());
            // Getter.
            emitGetter(getter, fieldName, returnType, builder);
            // Setter.
            emitSetter(fieldName, returnType, builder);
            // List/Map-specific methods.
            if (isList(returnTypeClass)) {
                emitListAdd(getter, fieldName, builder);
                emitClear(fieldName, builder);
                emitEnsureCollection(getter, fieldName, builder);
            } else if (isMap(returnTypeClass)) {
                emitMapPut(getter, fieldName, builder);
                emitClear(fieldName, builder);
                emitEnsureCollection(getter, fieldName, builder);
            }
        }
    }

    private void emitDelegateMethod(String returnType, Method method, Class<?> delegateToType, String delegateToMethod,
                                    StringBuilder builder) {
        builder.append("    public ").append(returnType).append(" ").append(method.getName()).append("(");
        Type[] parameterTypes = method.getGenericParameterTypes();
        String[] parameters = new String[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(getFqParameterizedName(parameterTypes[i]));
            String parameter = "$p" + i;
            builder.append(" ").append(parameter);
            parameters[i] = parameter;
        }
        builder.append(") {\n");
        builder.append("      ");
        if (!"void".equals(returnType)) {
            builder.append("return ");
        }
        builder.append(delegateToType.getCanonicalName()).append(".").append(delegateToMethod).append("(");
        builder.append("this");
        for (String parameter : parameters) {
            builder.append(", ");
            builder.append(parameter);
        }
        builder.append(");\n");
        builder.append("    }\n\n");
    }

    private void emitDelegateMethods(StringBuilder builder) {
        for (Method method : getDtoInterface().getDeclaredMethods()) {
            DelegateTo delegateTo = method.getAnnotation(DelegateTo.class);
            if (delegateTo != null) {
                DelegateRule serverRule = delegateTo.server();
                String returnType = getFqParameterizedName(method.getGenericReturnType());
                emitDelegateMethod(returnType, method, serverRule.type(), serverRule.method(), builder);
            }
        }
    }

    private void emitSerializer(List<Method> getters, StringBuilder builder) {
        builder.append("    @Override\n");
        builder.append("    public JsonElement toJsonElement() {\n");
        // The default toJsonElement() returns JSONs for unsafe use thus 'any' properties should be copied
        builder.append("      return toJsonElementInt(true);\n");
        builder.append("    }\n");
        builder.append("    public JsonElement toJsonElementInt(boolean ").append(COPY_JSONS_PARAM).append(") {\n");
        if (isCompactJson()) {
            builder.append("      JsonArray result = new JsonArray();\n");
            for (Method method : getters) {
                emitSerializeFieldForMethodCompact(method, builder);
            }
        } else {
            builder.append("      JsonObject result = new JsonObject();\n");
            for (Method getter : getters) {
                emitSerializeFieldForMethod(getter, builder);
            }
        }
        builder.append("      return result;\n");
        builder.append("    }\n");
        builder.append("\n");
        builder.append("    @Override\n");
        builder.append("    public String toJson() {\n");
        // The default toJson() creates its own JSON for internal printing, thus keeping JSONs values is safe
        builder.append("      return gson.toJson(toJsonElementInt(false));\n");
        builder.append("    }\n");
        builder.append("\n");
        builder.append("    @Override\n");
        builder.append("    public String toString() {\n");
        builder.append("      return toJson();\n");
        builder.append("    }\n\n");
    }

    private void emitSerializeFieldForMethod(Method getter, final StringBuilder builder) {
        final String fieldName = getFieldNameFromGetterName(getter.getName());
        final String jsonFieldName = getJsonFieldName(getter);
        final String fieldNameOut = fieldName + "Out";
        final String baseIndentation = "      ";
        builder.append("\n");
        List<Type> expandedTypes = expandType(getter.getGenericReturnType());
        emitSerializerImpl(expandedTypes, 0, builder, getJavaFieldName(getter.getName()), fieldNameOut, baseIndentation);
        builder.append("      result.add(");
        builder.append(quoteStringLiteral(jsonFieldName));
        builder.append(", ");
        builder.append(fieldNameOut);
        builder.append(");\n");
    }

    private void emitSerializeFieldForMethodCompact(Method getter, StringBuilder builder) {
        if (getter == null) {
            builder.append("      result.add(JsonNull.INSTANCE);\n");
            return;
        }
        final String jsonFieldName = getJsonFieldName(getter);
        final String fieldNameOut = jsonFieldName + "Out";
        final String baseIndentation = "      ";
        builder.append("\n");
        List<Type> expandedTypes = expandType(getter.getGenericReturnType());
        emitSerializerImpl(expandedTypes, 0, builder, getJavaFieldName(getter.getName()), fieldNameOut, baseIndentation);
        if (isLastMethod(getter)) {
            if (isList(getRawClass(expandedTypes.get(0)))) {
                builder.append("      if (").append(fieldNameOut).append(".size() != 0) {\n");
                builder.append("        result.add(").append(fieldNameOut).append(");\n");
                builder.append("      }\n");
                return;
            }
        }
        builder.append("      result.add(").append(fieldNameOut).append(");\n");
    }

    /**
     * Produces code to serialize the type with the given variable names.
     *
     * @param expandedTypes
     *         the type and its generic (and its generic (..))
     *         expanded into a list, @see {@link #expandType(java.lang.reflect.Type)}
     * @param depth
     *         the depth (in the generics) for this recursive call. This can
     *         be used to index into {@code expandedTypes}
     * @param inVar
     *         the java type that will be the input for serialization
     * @param outVar
     *         the JsonElement subtype that will be the output for
     *         serialization
     * @param i
     *         indentation string
     */
    private void emitSerializerImpl(List<Type> expandedTypes, int depth, StringBuilder builder, String inVar, String outVar, String i) {
        Type type = expandedTypes.get(depth);
        String childInVar = inVar + "_";
        String childOutVar = outVar + "_";
        String entryVar = "entry" + depth;
        Class<?> rawClass = getRawClass(type);
        if (isList(rawClass)) {
            String childInTypeName = getImplName(expandedTypes.get(depth + 1), false);
            builder.append(i).append("JsonArray ").append(outVar).append(" = new JsonArray();\n");
            if (depth == 0) {
                builder.append(i).append("this.").append(getEnsureName(inVar)).append("();\n");
            }
            builder.append(i).append("for (").append(childInTypeName).append(" ").append(childInVar).append(" : ").append(
                    depth == 0 ? "this." + inVar : inVar).append(") {\n");

        } else if (isMap(rawClass)) {
            String childInTypeName = getImplName(expandedTypes.get(depth + 1), false);
            builder.append(i).append("JsonObject ").append(outVar).append(" = new JsonObject();\n");
            if (depth == 0) {
                builder.append(i).append("this.").append(getEnsureName(inVar)).append("();\n");
            }
            builder.append(i).append("for (java.util.Map.Entry<String, ").append(childInTypeName).append("> ").append(
                    entryVar).append(" : ").append(depth == 0 ? "this." + inVar : inVar).append(".entrySet()) {\n");
            builder.append(i).append("  ").append(childInTypeName).append(" ").append(childInVar).append(" = ").append(
                    entryVar).append(".getValue();\n");
        } else if (rawClass.isEnum()) {
            builder.append(i).append("JsonElement ").append(outVar).append(" = (").append(depth == 0 ? "this." + inVar : inVar).append(
                    " == null) ? JsonNull.INSTANCE : new JsonPrimitive(").append(depth == 0 ? "this." + inVar : inVar)
                   .append(".name());\n");
        } else if (getEnclosingTemplate().isDtoInterface(rawClass)) {
            builder.append(i).append("JsonElement ").append(outVar).append(" = ").append(depth == 0 ? "this." + inVar : inVar).append(
                    " == null ? JsonNull.INSTANCE : ((").append(getImplNameForDto((Class<?>)expandedTypes.get(depth))).append(")")
                   .append(depth == 0 ? "this." + inVar : inVar).append(").toJsonElementInt(").append(COPY_JSONS_PARAM).append(");\n");
        } else if (rawClass.equals(String.class)) {
            builder.append(i).append("JsonElement ").append(outVar).append(" = (").append(depth == 0 ? "this." + inVar : inVar).append(
                    " == null) ? JsonNull.INSTANCE : new JsonPrimitive(").append(depth == 0 ? "this." + inVar : inVar).append(");\n");
        } else if (rawClass == boolean.class
                   || rawClass == int.class
                   || rawClass == long.class
                   || rawClass == double.class
                   || rawClass == float.class
                   || rawClass == short.class
                   || rawClass == byte.class) {
            builder.append(i).append("JsonPrimitive ").append(outVar).append(" = new JsonPrimitive(")
                   .append(depth == 0 ? "this." + inVar : inVar).append(");\n");
        } else if (rawClass == Boolean.class
                   || rawClass == Integer.class
                   || rawClass == Long.class
                   || rawClass == Double.class
                   || rawClass == Float.class
                   || rawClass == Short.class
                   || rawClass == Byte.class) {
            builder.append(i).append("JsonElement ").append(outVar).append(" = ").append(depth == 0 ? " this." + inVar : inVar).append(
                    " == null ? JsonNull.INSTANCE : new JsonPrimitive(").append(depth == 0 ? "this." + inVar : inVar).append(");\n");
        } else if (isAny(rawClass)) {
            // TODO JsonElement.deepCopy() is package-protected, JSONs are serialized to strings then parsed for copying them
            // outVar = inVar == null ? JsonNull.INSTNACE : (copyJsons ? new JsonParser().parse(inVar) : inVar);
            builder.append(i).append("JsonElement ").append(outVar).append(" = ").append(depth == 0 ? " this." + inVar : inVar)
                    .append(" == null || !(").append(inVar).append(" instanceof JsonElement) ? JsonNull.INSTANCE : (");
            appendCopyJsonExpression(inVar, builder).append(");\n");
        } else {
            final Class<?> dtoImplementation = getEnclosingTemplate().getDtoImplementation(rawClass);
            if (dtoImplementation != null) {
                builder.append(i).append("JsonElement ").append(outVar).append(" = ").append(depth == 0 ? "this." + inVar : inVar).append(
                        " == null ? JsonNull.INSTANCE : ((").append(dtoImplementation.getCanonicalName()).append(")")
                       .append(depth == 0 ? "this." + inVar : inVar).append(").toJsonElementInt(").append(COPY_JSONS_PARAM).append(");\n");
            } else {
                throw new IllegalArgumentException("Unable to generate server implementation for DTO interface " +
                                                   getDtoInterface().getCanonicalName() + ". Type " + rawClass +
                                                   " is not allowed to use in DTO interface.");
            }
        }

        if (depth + 1 < expandedTypes.size()) {
            emitSerializerImpl(expandedTypes, depth + 1, builder, childInVar, childOutVar, i + "  ");
        }
        if (isList(rawClass)) {
            builder.append(i).append("  ").append(outVar).append(".add(").append(childOutVar).append(");\n");
            builder.append(i).append("}\n");
        } else if (isMap(rawClass)) {
            builder.append(i).append("  ").append(outVar).append(".add(").append(entryVar).append(".getKey(), ").append(
                    childOutVar).append(");\n");
            builder.append(i).append("}\n");
        }
    }

    /** Generates a static factory method that creates a new instance based on a JsonElement. */
    private void emitDeserializer(List<Method> getters, StringBuilder builder) {
        // The default fromJsonElement(json) works in unsafe mode and clones the JSON's for 'any' properties
        builder.append("    public static ").append(getImplClassName()).append(" fromJsonElement(JsonElement jsonElem) {\n");
        builder.append("      return fromJsonElement(jsonElem, true);\n");
        builder.append("    }\n");
        builder.append("    public static ").append(getImplClassName()).append(" fromJsonElement(JsonElement jsonElem, boolean ").append(COPY_JSONS_PARAM).append(") {\n");
        builder.append("      if (jsonElem == null || jsonElem.isJsonNull()) {\n");
        builder.append("        return null;\n");
        builder.append("      }\n\n");
        builder.append("      ").append(getImplClassName()).append(" dto = new ").append(getImplClassName()).append("();\n");
        if (isCompactJson()) {
            builder.append("      JsonArray json = jsonElem.getAsJsonArray();\n");
            for (Method method : getters) {
                emitDeserializeFieldForMethodCompact(method, builder);
            }
        } else {
            builder.append("      JsonObject json = jsonElem.getAsJsonObject();\n");
            for (Method getter : getters) {
                emitDeserializeFieldForMethod(getter, builder);
            }
        }
        builder.append("\n      return dto;\n");
        builder.append("    }\n\n");
    }

    private void emitDeserializerShortcut(StringBuilder builder) {
        builder.append("    public static ");
        builder.append(getImplClassName());
        builder.append(" fromJsonString(String jsonString) {\n");
        builder.append("      if (jsonString == null) {\n");
        builder.append("        return null;\n");
        builder.append("      }\n\n");
        // The default fromJsonElement(json) creates its own JSON thus keeping parts of its as value is OK
        builder.append("      return fromJsonElement(new JsonParser().parse(jsonString), false);\n");
        builder.append("    }\n\n");
    }

    private void emitDeserializeFieldForMethod(Method method, StringBuilder builder) {
        final String fieldName = getFieldNameFromGetterName(method.getName());
        final String fieldNameIn = fieldName + "In";
        final String fieldNameOut = fieldName + "Out";
        final String baseIndentation = "        ";
        
        final String jsonFieldName = getJsonFieldName(method);

        builder.append("\n");
        builder.append("      if (json.has(").append(quoteStringLiteral(jsonFieldName)).append(")) {\n");
        List<Type> expandedTypes = expandType(method.getGenericReturnType());
        builder.append("        JsonElement ").append(fieldNameIn).append(" = json.get(").append(quoteStringLiteral(jsonFieldName)).append(");\n");
        emitDeserializerImpl(expandedTypes, 0, builder, fieldNameIn, fieldNameOut, baseIndentation);
        builder.append("        dto.").append(getSetterName(fieldName)).append("(").append(fieldNameOut).append(");\n");
        builder.append("      }\n");
    }

    private void emitDeserializeFieldForMethodCompact(Method method, final StringBuilder builder) {
        final String fieldName = getJsonFieldName(method);
        final String fieldNameIn = fieldName + "In";
        final String fieldNameOut = fieldName + "Out";
        final String baseIndentation = "        ";
        SerializationIndex serializationIndex = Preconditions.checkNotNull(method.getAnnotation(SerializationIndex.class));
        int index = serializationIndex.value() - 1;
        builder.append("\n");
        builder.append("      if (").append(index).append(" < json.size()) {\n");
        List<Type> expandedTypes = expandType(method.getGenericReturnType());
        builder.append("        JsonElement ").append(fieldNameIn).append(" = json.get(").append(index).append(");\n");
        emitDeserializerImpl(expandedTypes, 0, builder, fieldNameIn, fieldNameOut, baseIndentation);
        builder.append("        dto.").append(getSetterName(fieldName)).append("(").append(fieldNameOut).append(");\n");
        builder.append("      }\n");
    }

    /**
     * Produces code to deserialize the type with the given variable names.
     *
     * @param expandedTypes
     *         the type and its generic (and its generic (..)) expanded into a list, @see {@link #expandType(java.lang.reflect.Type)}
     * @param depth
     *         the depth (in the generics) for this recursive call. This can be used to index into {@code expandedTypes}
     * @param inVar
     *         the java type that will be the input for serialization
     * @param outVar
     *         the JsonElement subtype that will be the output for serialization
     * @param i
     *         indentation string
     */
    private void emitDeserializerImpl(List<Type> expandedTypes, int depth, StringBuilder builder, String inVar,
                                      String outVar, String i) {
        Type type = expandedTypes.get(depth);
        String childInVar = inVar + "_";
        String childOutVar = outVar + "_";
        Class<?> rawClass = getRawClass(type);

        if (isList(rawClass)) {
            String inVarIterator = inVar + "Iterator";
            builder.append(i).append(getImplName(type, false)).append(" ").append(outVar).append(" = null;\n");
            builder.append(i).append("if (").append(inVar).append(" != null && !").append(inVar).append(".isJsonNull()) {\n");
            builder.append(i).append("  ").append(outVar).append(" = new ").append(getImplName(type, true)).append("();\n");
            builder.append(i).append("  ").append(getImplName(Iterator.class, false)).append("<JsonElement> ")
                   .append(inVarIterator).append(" = ").append(inVar).append(".getAsJsonArray().iterator();\n");
            builder.append(i).append("  while (").append(inVarIterator).append(".hasNext()) {\n");
            builder.append(i).append("    JsonElement ").append(childInVar).append(" = ").append(inVarIterator).append(".next();\n");

            emitDeserializerImpl(expandedTypes, depth + 1, builder, childInVar, childOutVar, i + "    ");

            builder.append(i).append("    ").append(outVar).append(".add(").append(childOutVar).append(");\n");
            builder.append(i).append("  }\n");
            builder.append(i).append("}\n");
        } else if (isMap(rawClass)) {
            // TODO: Handle type
            String entryVar = "entry" + depth;
            String entriesVar = "entries" + depth;
            builder.append(i).append(getImplName(type, false)).append(" ").append(outVar).append(" = null;\n");
            builder.append(i).append("if (").append(inVar).append(" != null && !").append(inVar).append(".isJsonNull()) {\n");
            builder.append(i).append("  ").append(outVar).append(" = new ").append(getImplName(type, true)).append("();\n");
            builder.append(i).append("  java.util.Set<java.util.Map.Entry<String, JsonElement>> ").append(entriesVar).append(
                    " = ").append(inVar).append(".getAsJsonObject().entrySet();\n");
            builder.append(i).append("  for (java.util.Map.Entry<String, JsonElement> ").append(entryVar).append(" : ").append(entriesVar)
                   .append(") {\n");
            builder.append(i).append("    JsonElement ").append(childInVar).append(" = ").append(entryVar).append(".getValue();\n");
            emitDeserializerImpl(expandedTypes, depth + 1, builder, childInVar, childOutVar, i + "    ");

            builder.append(i).append("    ").append(outVar).append(".put(").append(entryVar).append(".getKey(), ").append(
                    childOutVar).append(");\n");
            builder.append(i).append("  }\n");
            builder.append(i).append("}\n");
        } else if (getEnclosingTemplate().isDtoInterface(rawClass)) {
            String className = getImplName(rawClass, false);
            builder.append(i).append(className).append(" ").append(outVar).append(" = ").append(getImplNameForDto(rawClass))
                   .append(".fromJsonElement(").append(inVar).append(", ").append(COPY_JSONS_PARAM).append(");\n");
        } else if (rawClass.isPrimitive()) {
            String primitiveName = rawClass.getSimpleName();
            String primitiveNameCap = primitiveName.substring(0, 1).toUpperCase() + primitiveName.substring(1);
            builder.append(i).append(primitiveName).append(" ").append(outVar).append(" = ").append(inVar).append(
                    ".getAs").append(primitiveNameCap).append("();\n");
        } else if (isAny(rawClass)) {
            // TODO JsonElement.deepCopy() is package-protected, JSONs are serialized to strings then parsed for copying them
            // outVar = copyJsons ? new JsonParser().parse(inVar) : inVar;
            builder.append(i).append("JsonElement ").append(outVar).append(" = ");
            appendCopyJsonExpression(inVar, builder).append(";\n");
        } else {
            final Class<?> dtoImplementation = getEnclosingTemplate().getDtoImplementation(rawClass);
            if (dtoImplementation != null) {
                String className = getImplName(rawClass, false);
                builder.append(i).append(className).append(" ").append(outVar).append(" = ")
                       .append(dtoImplementation.getCanonicalName()).append(".fromJsonElement(").append(inVar).append(", ").append(COPY_JSONS_PARAM).append(");\n");
            } else {
                // Use gson to handle all other types.
                String rawClassName = rawClass.getName().replace('$', '.');
                builder.append(i).append(rawClassName).append(" ").append(outVar).append(" = gson.fromJson(").append(
                        inVar).append(", ").append(rawClassName).append(".class);\n");
            }
        }
    }
    
    /**
     * Append the expression that clones the given JsonElement variable into a new value. If the copyJons run-time
     * parameter is set to false, then the expression won't perform a clone but instead will reuse the variable by
     * reference.
     */
    private static StringBuilder appendCopyJsonExpression(String inVar, StringBuilder builder) {
        builder.append(COPY_JSONS_PARAM).append(" ? ");
        appendNaiveCopyJsonExpression(inVar, builder).append(" : (JsonElement)(").append(inVar).append(")");
        return builder;
    }
    private static StringBuilder appendNaiveCopyJsonExpression(String inValue, StringBuilder builder) {
        builder.append("((");
        builder.append(inValue);
        builder.append(") != null ? new JsonParser().parse((");
        builder.append(inValue);
        builder.append(").toString()) : null)");
        return builder;
    }

    private void emitPreamble(Class<?> dtoInterface, StringBuilder builder) {
        builder.append(SERVER_DTO_MARKER);
        builder.append("  public static class ");
        builder.append(getImplClassName());

        Class<?> superType = getSuperDtoInterface(getDtoInterface());
        if (superType != null && superType != JsonSerializable.class) {
            // We need to extend something.
            builder.append(" extends ");

            final Class<?> superTypeImpl = getEnclosingTemplate().getDtoImplementation(superType);
            if (superTypeImpl == null) {
                builder.append(superType.getSimpleName()).append("Impl");
            } else {
                builder.append(superTypeImpl.getCanonicalName());
            }
        }
        builder.append(" implements ");
        builder.append(dtoInterface.getCanonicalName());
        builder.append(", JsonSerializable ");
        builder.append(" {\n\n");
        emitFactoryMethod(builder);
        emitDefaultConstructor(builder);
    }

    private void emitPostamble(StringBuilder builder) {
        builder.append("  }\n\n");
    }

    private void emitDefaultConstructor(StringBuilder builder) {
        builder.append("    public ");
        builder.append(getImplClassName());
        builder.append("() {\n");
        builder.append("    }\n\n");
    }

    private void emitReturn(Method method, String fieldName, StringBuilder builder) {
        if (isList(method.getReturnType())) {
            // Wrap the returned List in the server adapter.
            builder.append("new ");
            builder.append(JSON_ARRAY_IMPL);
            builder.append("(");
            builder.append(fieldName);
            builder.append(")");
        } else if (isMap(method.getReturnType())) {
            // Wrap the Map.
            builder.append("new ");
            builder.append(JSON_MAP_IMPL);
            builder.append("(");
            builder.append(fieldName);
            builder.append(")");
        } else {
            builder.append(fieldName);
        }
    }

    private void emitSetter(String fieldName, String paramType, StringBuilder builder) {
        builder.append("    public ");
        builder.append("void");
        builder.append(" ");
        builder.append(getSetterName(fieldName));
        builder.append("(");
        builder.append(paramType);
        builder.append(" v) {\n");
        builder.append("      this.");
        builder.append(fieldName);
        builder.append(" = ");
        builder.append("v;\n    }\n\n");
    }

    private void emitWithMethods(List<Method> getters, String dtoInterfaceName, StringBuilder builder) {
        for (Method getter : getters) {
            String fieldName = getJavaFieldName(getter.getName());
            emitWithMethod(getWithName(fieldName), fieldName, getFqParameterizedName(getter.getGenericReturnType()), dtoInterfaceName,
                           builder);
        }
    }

    private void emitWithMethod(String methodName, String fieldName, String paramType, String dtoInterfaceName, StringBuilder builder) {
        builder.append("    public ");
        builder.append(dtoInterfaceName);
        builder.append(" ");
        builder.append(methodName);
        builder.append("(");
        builder.append(paramType);
        builder.append(" v) {\n");
        builder.append("      this.");
        builder.append(fieldName);
        builder.append(" = ");
        builder.append("v;\n      return this;\n    }\n\n");
    }

    /**
     * Emits an add method to add to a list. If the list is null, it is created.
     *
     * @param method
     *         a method with a list return type
     */
    private void emitListAdd(Method method, String fieldName, StringBuilder builder) {
        builder.append("    public void ");
        builder.append(getListAdderName(fieldName));
        builder.append("(");
        builder.append(getTypeArgumentImplName((ParameterizedType)method.getGenericReturnType(), 0));
        builder.append(" v) {\n      ");
        builder.append(getEnsureName(fieldName));
        builder.append("();\n      ");
        builder.append(fieldName);
        builder.append(".add(v);\n");
        builder.append("    }\n\n");
    }

    /**
     * Emits a put method to put a value into a map. If the map is null, it is created.
     *
     * @param method
     *         a method with a map return value
     */
    private void emitMapPut(Method method, String fieldName, StringBuilder builder) {
        builder.append("    public void ");
        builder.append(getMapPutterName(fieldName));
        builder.append("(String k, ");
        builder.append(getTypeArgumentImplName((ParameterizedType)method.getGenericReturnType(), 1));
        builder.append(" v) {\n      ");
        builder.append(getEnsureName(fieldName));
        builder.append("();\n      ");
        builder.append(fieldName);
        builder.append(".put(k, v);\n");
        builder.append("    }\n\n");
    }

    /** Emits a method to clear a list or map. Clearing the collections ensures that the collection is created. */
    private void emitClear(String fieldName, StringBuilder builder) {
        builder.append("    public void ");
        builder.append(getClearName(fieldName));
        builder.append("() {\n      ");
        builder.append(getEnsureName(fieldName));
        builder.append("();\n      ");
        builder.append(fieldName);
        builder.append(".clear();\n");
        builder.append("    }\n\n");
    }

    private void emitCopyConstructor(List<Method> getters, StringBuilder builder) {
        String dtoInterface = getDtoInterface().getCanonicalName();
        String implClassName = getImplClassName();
        builder.append("    public ").append(implClassName).append("(").append(dtoInterface).append(" origin) {\n");
        for (Method method : getters) {
            emitDeepCopyForGetters(expandType(method.getGenericReturnType()), 0, builder, "origin", method, "      ");
        }
        builder.append("    }\n\n");
    }

    private void emitDeepCopyForGetters(List<Type> expandedTypes, int depth, StringBuilder builder, String origin, Method getter,
                                        String i) {
        String getterName = getter.getName();
        String fieldName = getJavaFieldName(getterName);
        String fieldNameIn = fieldName + "In";
        String fieldNameOut = fieldName + "Out";
        Type type = expandedTypes.get(depth);
        Class<?> rawClass = getRawClass(type);
        String rawTypeName = getImplName(type, false);

        if (isList(rawClass) || isMap(rawClass)) {
            builder.append(i).append(rawTypeName).append(" ").append(fieldNameIn).append(" = ").append(origin).append(".")
                   .append(getterName).append("();\n");
            builder.append(i).append("if (").append(fieldNameIn).append(" != null) {\n");
            builder.append(i).append("  ").append(rawTypeName).append(" ").append(fieldNameOut)
                   .append(" = new ").append(getImplName(type, true)).append("();\n");
            emitDeepCopyCollections(expandedTypes, depth, builder, fieldNameIn, fieldNameOut, i);
            builder.append(i).append("  ").append("this.").append(fieldName).append(" = ").append(fieldNameOut).append(";\n");
            builder.append(i).append("}\n");
        } else if (isAny(rawClass)) {
            builder.append(i).append("this.").append(fieldName).append(" = ");
            appendNaiveCopyJsonExpression(origin + "." + getterName + "()", builder).append(";\n");
        } else if (getEnclosingTemplate().isDtoInterface(rawClass)) {
            builder.append(i).append(rawTypeName).append(" ").append(fieldNameIn).append(" = ").append(origin).append(".")
                   .append(getterName).append("();\n");
            builder.append(i).append("this.").append(fieldName).append(" = ");
            emitCheckNullAndCopyDto(rawClass, fieldNameIn, builder);
            builder.append(";\n");
        } else {
            builder.append(i).append("this.").append(fieldName).append(" = ")
                   .append(origin).append(".").append(getterName).append("();\n");
        }
    }

    private void emitDeepCopyCollections(List<Type> expandedTypes, int depth, StringBuilder builder, String varIn, String varOut,
                                         String i) {
        Type type = expandedTypes.get(depth);
        String childVarIn = varIn + "_";
        String childVarOut = varOut + "_";
        String entryVar = "entry" + depth;
        Class<?> rawClass = getRawClass(type);
        Class<?> childRawType = getRawClass(expandedTypes.get(depth + 1));
        final String childTypeName = getImplName(expandedTypes.get(depth + 1), false);
        if (isList(rawClass)) {
            builder.append(i).append("  for (").append(childTypeName).append(" ").append(childVarIn)
                   .append(" : ").append(varIn).append(") {\n");
        } else if (isMap(rawClass)) {
            builder.append(i).append("  for (java.util.Map.Entry<String, ").append(childTypeName).append("> ").append(entryVar)
                   .append(" : ").append(varIn).append(".entrySet()) {\n");
            builder.append(i).append("    ").append(childTypeName).append(" ").append(childVarIn).append(" = ").append(
                    entryVar).append(".getValue();\n");
        }
        if (isList(childRawType) || isMap(childRawType)) {
            builder.append(i).append("    if (").append(childVarIn).append(" != null) {\n");
            builder.append(i).append("      ").append(childTypeName).append(" ").append(childVarOut)
                   .append(" = new ").append(getImplName(expandedTypes.get(depth + 1), true)).append("();\n");
            emitDeepCopyCollections(expandedTypes, depth + 1, builder, childVarIn, childVarOut, i + "    ");
            builder.append(i).append("      ").append(varOut);
            if (isList(rawClass)) {
                builder.append(".add(");
            } else {
                builder.append(".put(").append(entryVar).append(".getKey(), ");
            }
            builder.append(childVarOut);
            builder.append(");\n");
            builder.append(i).append("    ").append("}\n");
        } else {
            builder.append(i).append("      ").append(varOut);
            if (isList(rawClass)) {
                builder.append(".add(");
            } else {
                builder.append(".put(").append(entryVar).append(".getKey(), ");
            }
            if (getEnclosingTemplate().isDtoInterface(childRawType)) {
                emitCheckNullAndCopyDto(childRawType, childVarIn, builder);
            } else {
                builder.append(childVarIn);
            }
            builder.append(");\n");
        }
        builder.append(i).append("  }\n");
    }

    private void emitCheckNullAndCopyDto(Class<?> dto, String fieldName, StringBuilder builder) {
        String implName = dto.getSimpleName() + "Impl";
        builder.append(fieldName).append(" == null ? null : ").append("new ").append(implName).append("(").append(fieldName).append(")");
    }

    /** Emit a method that ensures a collection is initialized. */
    private void emitEnsureCollection(Method method, String fieldName, StringBuilder builder) {
        builder.append("    protected void ");
        builder.append(getEnsureName(fieldName));
        builder.append("() {\n");
        builder.append("      if (");
        builder.append(fieldName);
        builder.append(" == null) {\n        ");
        builder.append(fieldName);
        builder.append(" = new ");
        builder.append(getImplName(method.getGenericReturnType(), true));
        builder.append("();\n");
        builder.append("      }\n");
        builder.append("    }\n");
    }

    /**
     * Appends a suitable type for the given type. For example, at minimum, this will replace DTO interfaces with their implementation
     * classes and JSON collections with corresponding Java types. If a suitable type cannot be determined, this will throw an exception.
     *
     * @param genericType
     *         the type as returned by e.g. method.getGenericReturnType()
     */
    private void appendType(Type genericType, final StringBuilder builder) {
        builder.append(getImplName(genericType, false));
    }

    /**
     * In most cases we simply echo the return type and field name, except for JsonArray<T>, which is special in the server impl case,
     * since it must be represented by a List<T> for Gson to correctly serialize/deserialize it.
     *
     * @param method
     *         The getter method.
     * @return String representation of what the field type should be, as well as the assignment (initial value) to said field type, if any.
     */
    private String getFieldTypeAndAssignment(Method method, String fieldName) {
        StringBuilder builder = new StringBuilder();
        builder.append("protected ");
        appendType(method.getGenericReturnType(), builder);
        builder.append(" ");
        builder.append(fieldName);
        builder.append(";\n");
        return builder.toString();
    }

    /**
     * Returns the fully-qualified type name using Java concrete implementation classes.
     * <p/>
     * For example, for JsonArray&lt;JsonStringMap&lt;Dto&gt;&gt;, this would return "ArrayList&lt;Map&lt;String, DtoImpl&gt;&gt;".
     */
    private String getImplName(Type type, boolean allowJreCollectionInterface) {
        Class<?> rawClass = getRawClass(type);
        String fqName = getFqParameterizedName(type);
        fqName = fqName.replaceAll(JsonArray.class.getCanonicalName(), ArrayList.class.getCanonicalName());
        fqName = fqName.replaceAll(JsonStringMap.class.getCanonicalName() + "<",
                                   HashMap.class.getCanonicalName() + "<String, ");

        if (allowJreCollectionInterface) {
            if (isList(rawClass)) {
                fqName = fqName.replaceFirst(List.class.getCanonicalName(), ArrayList.class.getCanonicalName());
            } else if (isMap(rawClass)) {
                fqName = fqName.replaceFirst(Map.class.getCanonicalName(), HashMap.class.getCanonicalName());
            }
        }

        return fqName;
    }

    /** Returns the fully-qualified type name including parameters. */
    private String getFqParameterizedName(Type type) {
        if (type instanceof Class<?>) {
            return ((Class<?>)type).getCanonicalName();
            //return getImplNameForDto((Class<?>)type);

        } else if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType)type;

            StringBuilder sb = new StringBuilder(getRawClass(pType).getCanonicalName());
            sb.append('<');
            for (int i = 0; i < pType.getActualTypeArguments().length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(getFqParameterizedName(pType.getActualTypeArguments()[i]));
            }
            sb.append('>');

            return sb.toString();

        } else {
            throw new IllegalArgumentException("Can't build implementation of " + getDtoInterface().getSimpleName() +
                                               ". DtoGenerator does not handle this type " + type.toString());
        }
    }

    /**
     * Returns the fully-qualified type name using Java concrete implementation classes of the first type argument for a parameterized
     * type. If one is not specified, returns "Object".
     *
     * @param type
     *         the parameterized type
     * @return the first type argument
     */
    private String getTypeArgumentImplName(ParameterizedType type, int index) {
        Type[] typeArgs = type.getActualTypeArguments();
        if (typeArgs.length == 0) {
            return "Object";
        }
        return getImplName(typeArgs[index], false);
    }

    private String getImplNameForDto(Class<?> dtoInterface) {
        if (getEnclosingTemplate().isDtoInterface(dtoInterface)) {
            // This will eventually get a generated impl type.
            return dtoInterface.getSimpleName() + "Impl";
        }
        return dtoInterface.getCanonicalName();
    }
}