/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.factory;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.factory.FactoryParameter;
import org.eclipse.che.api.project.shared.dto.ImportSourceDescriptor;

import java.util.Map;

import static org.eclipse.che.api.factory.FactoryConstants.PARAMETRIZED_ILLEGAL_PARAMETER_VALUE_MESSAGE;
import static org.eclipse.che.api.factory.FactoryConstants.PARAMETRIZED_INVALID_PARAMETER_MESSAGE;
import static java.lang.String.format;

/**
 * @author Alexander Garagatyi
 * @author Valeriy Svydenko
 */
public class SourceProjectParametersValidator implements FactoryParameterValidator<ImportSourceDescriptor> {
    @Override
    public void validate(ImportSourceDescriptor source, FactoryParameter.Version version) throws ConflictException {
        if ("git".equals(source.getType()) || "esbwso2".equals(source.getType())) {
            for (Map.Entry<String, String> entry : source.getParameters().entrySet()) {
                switch (entry.getKey()) {
                    case "keepVcs":
                        final String keepVcs = entry.getValue();
                        if (!"true".equals(keepVcs) && !"false".equals(keepVcs)) {
                            throw new ConflictException(
                                    format(PARAMETRIZED_ILLEGAL_PARAMETER_VALUE_MESSAGE, "source.project.parameters.keepVcs", entry.getValue()));
                        }
                        break;
                    case "branch":
                    case "commitId":
                    case "keepDirectory":
                    case "remoteOriginFetch":
                    case "branchMerge":
                        break;
                    default:
                        throw new ConflictException(format(PARAMETRIZED_INVALID_PARAMETER_MESSAGE, "source.project.parameters." + entry.getKey(), version));
                }
            }
        } else {
            throw new ConflictException(format(PARAMETRIZED_ILLEGAL_PARAMETER_VALUE_MESSAGE, "source.project.type", source.getType()));
        }
    }
}
