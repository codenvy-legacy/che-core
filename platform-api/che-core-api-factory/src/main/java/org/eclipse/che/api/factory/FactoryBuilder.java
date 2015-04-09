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

import com.google.common.base.CaseFormat;

import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.factory.FactoryParameter;
import org.eclipse.che.api.factory.converter.ActionsConverter;
import org.eclipse.che.api.factory.converter.LegacyConverter;
import org.eclipse.che.api.factory.dto.Factory;
import org.eclipse.che.api.factory.dto.FactoryV2_0;
import org.eclipse.che.api.factory.dto.FactoryV2_1;
import org.eclipse.che.api.project.shared.dto.ImportSourceDescriptor;
import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.dto.shared.DTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.eclipse.che.api.core.factory.FactoryParameter.Obligation;
import static org.eclipse.che.api.core.factory.FactoryParameter.Version;

/**
 * Tool to easy convert Factory object to json and vise versa.
 * Also it provides factory parameters compatibility.
 *
 * @author Sergii Kabashniuk
 * @author Alexander Garagatyi
 */
@Singleton
public class FactoryBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(FactoryService.class);

    /** List contains all possible implementation of factory legacy converters. */
    static final List<LegacyConverter> LEGACY_CONVERTERS;

    static {
        List<LegacyConverter> l = new ArrayList<>(1);
        l.add(new ActionsConverter());
        LEGACY_CONVERTERS = Collections.unmodifiableList(l);
    }

    private final SourceProjectParametersValidator sourceProjectParametersValidator;

    @Inject
    public FactoryBuilder(SourceProjectParametersValidator sourceProjectParametersValidator) {
        this.sourceProjectParametersValidator = sourceProjectParametersValidator;
    }

    /**
     * Build factory from json of encoded factory and validate compatibility.
     *
     * @param json
     *         - json Reader from encoded factory.
     * @return - Factory object represented by given factory json.
     */
    public Factory buildEncoded(Reader json) throws IOException, ApiException {
        Factory factory = DtoFactory.getInstance().createDtoFromJson(json, Factory.class);
        checkValid(factory);
        return factory;
    }

    /**
     * Build factory from json of encoded factory and validate compatibility.
     *
     * @param json
     *         - json string from encoded factory.
     * @return - Factory object represented by given factory json.
     */
    public Factory buildEncoded(String json) throws ApiException {
        Factory factory = DtoFactory.getInstance().createDtoFromJson(json, Factory.class);
        checkValid(factory);
        return factory;
    }

    /**
     * Build factory from json of encoded factory and validate compatibility.
     *
     * @param json
     *         - json  InputStream from encoded factory.
     * @return - Factory object represented by given factory json.
     */
    public Factory buildEncoded(InputStream json) throws IOException, ApiException {
        Factory factory = DtoFactory.getInstance().createDtoFromJson(json, Factory.class);
        checkValid(factory);
        return factory;
    }

    /**
     * Validate factory compatibility.
     *
     * @param factory
     *         - factory object to validate
     * @throws ApiException
     */
    public void checkValid(Factory factory) throws ApiException {
        if (null == factory) {
            throw new ConflictException(FactoryConstants.UNPARSABLE_FACTORY_MESSAGE);
        }
        if (factory.getV() == null) {
            throw new ConflictException(FactoryConstants.INVALID_VERSION_MESSAGE);
        }

        Version v;
        try {
            v = Version.fromString(factory.getV());
        } catch (IllegalArgumentException e) {
            throw new ConflictException(FactoryConstants.INVALID_VERSION_MESSAGE);
        }

        Class usedFactoryVersionMethodProvider;
        switch (v) {
            case V2_0:
                usedFactoryVersionMethodProvider = FactoryV2_0.class;
                break;
            case V2_1:
                usedFactoryVersionMethodProvider = FactoryV2_1.class;
                break;
            default:
                throw new ConflictException(FactoryConstants.INVALID_VERSION_MESSAGE);
        }
        validateCompatibility(factory, Factory.class, usedFactoryVersionMethodProvider, v, "");
    }

    /**
     * Convert factory of given version to the latest factory format.
     *
     * @param factory
     *         - given factory.
     * @return - factory in latest format.
     * @throws org.eclipse.che.api.core.ApiException
     */
    public Factory convertToLatest(Factory factory) throws ApiException {
        Factory resultFactory = DtoFactory.getInstance().clone(factory);
        resultFactory.setV("2.1");
        for (LegacyConverter converter : LEGACY_CONVERTERS) {
            converter.convert(resultFactory);
        }

        return resultFactory;
    }


    /**
     * Validate compatibility of factory parameters.
     *
     * @param object
     *         - object to validate factory parameters
     * @param methodsProvider
     *         - class that provides methods with {@link org.eclipse.che.api.core.factory.FactoryParameter}
     *         annotations
     * @param allowedMethodsProvider
     *         - class that provides allowed methods
     * @param version
     *         - version of factory
     * @param parentName
     *         - parent parameter queryParameterName
     * @throws org.eclipse.che.api.core.ApiException
     */
    void validateCompatibility(Object object,
                               Class methodsProvider,
                               Class allowedMethodsProvider,
                               Version version,
                               String parentName) throws ApiException {
        // get all methods recursively
        for (Method method : methodsProvider.getMethods()) {
            FactoryParameter factoryParameter = method.getAnnotation(FactoryParameter.class);
            // is it factory parameter
            if (factoryParameter != null) {
                String fullName = (parentName.isEmpty() ? "" : (parentName + ".")) + CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL,
                                                                                                              method.getName().substring(3)
                                                                                                                     .toLowerCase());
                // check that field is set
                Object parameterValue;
                try {
                    parameterValue = method.invoke(object);
                } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
                    // should never happen
                    LOG.error(e.getLocalizedMessage(), e);
                    throw new ConflictException(FactoryConstants.INVALID_PARAMETER_MESSAGE);
                }

                // if value is null or empty collection or default value for primitives
                if (ValueHelper.isEmpty(parameterValue)) {
                    // field must not be a mandatory, unless it's ignored or deprecated or doesn't suit to the version
                    if (Obligation.MANDATORY.equals(factoryParameter.obligation()) &&
                        factoryParameter.deprecatedSince().compareTo(version) > 0 &&
                        factoryParameter.ignoredSince().compareTo(version) > 0 &&
                        method.getDeclaringClass().isAssignableFrom(allowedMethodsProvider)) {
                        throw new ConflictException(FactoryConstants.MISSING_MANDATORY_MESSAGE);
                    }
                } else if (!method.getDeclaringClass().isAssignableFrom(allowedMethodsProvider)) {
                    throw new ConflictException(String.format(FactoryConstants.PARAMETRIZED_INVALID_PARAMETER_MESSAGE, fullName, version));
                } else {
                    // is parameter deprecated
                    if (factoryParameter.deprecatedSince().compareTo(version) <= 0) {
                        throw new ConflictException(
                                String.format(FactoryConstants.PARAMETRIZED_INVALID_PARAMETER_MESSAGE, fullName, version));
                    }

                    if (factoryParameter.setByServer()) {
                        throw new ConflictException(
                                String.format(FactoryConstants.PARAMETRIZED_INVALID_PARAMETER_MESSAGE, fullName, version));
                    }

                    // use recursion if parameter is DTO object
                    if (method.getReturnType().isAnnotationPresent(DTO.class)) {
                        // validate inner objects such Git ot ProjectAttributes
                        validateCompatibility(parameterValue, method.getReturnType(), method.getReturnType(), version, fullName);
                    } else if (Map.class.isAssignableFrom(method.getReturnType())) {
                        Type tp = ((ParameterizedType)method.getGenericReturnType()).getActualTypeArguments()[1];

                        Class secMapParamClass;
                        if (tp instanceof ParameterizedType) {
                            secMapParamClass = (Class)((ParameterizedType)tp).getRawType();
                        } else {
                            secMapParamClass = (Class)tp;
                        }
                        if (String.class.equals(secMapParamClass)) {
                            if (ImportSourceDescriptor.class.equals(methodsProvider)) {
                                sourceProjectParametersValidator.validate((ImportSourceDescriptor)object, version);
                            }
                        } else if (List.class.equals(secMapParamClass)) {
                            // do nothing
                        } else {
                            if (secMapParamClass.isAnnotationPresent(DTO.class)) {
                                Map<Object, Object> map = (Map)parameterValue;
                                for (Map.Entry<Object, Object> entry : map.entrySet()) {
                                    validateCompatibility(entry.getValue(), secMapParamClass, secMapParamClass, version,
                                                          fullName + "." + entry.getKey());
                                }
                            } else {
                                throw new RuntimeException("This type of fields is not supported by factory.");
                            }
                        }
                    }
                }
            }
        }
    }
}
