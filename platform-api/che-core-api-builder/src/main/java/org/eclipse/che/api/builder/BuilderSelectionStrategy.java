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
package org.eclipse.che.api.builder;

import java.util.List;
import org.eclipse.che.api.builder.dto.BaseBuilderRequest;

/**
 * Selects the 'best' SlaveBuilder from the List according to implementation. BuildQueue uses implementation of this interface to
 * find the 'best' slave-builder for processing incoming build request. If slave-builder available then BuildQueue collects
 * them (their front-ends which are represented by SlaveBuilder) and passes to implementation of this interface. This implementation
 * should select the 'best' one.
 *
 * @author andrew00x
 */
public interface BuilderSelectionStrategy {

    /**
     * Selects best matching {@code RemoteBuilder} that satisfies the implemented strategy
     * @param slaveBuilders
     * @return {@code RemoteBuilder} or {@code null} if there is no adequate selection
     */
    RemoteBuilder select(List<RemoteBuilder> slaveBuilders);

    /**
     * Selects best matching {@code RemoteBuilder} that satisfies implemented strategy.
     * Implemented strategy may consider both {@code BaseBuilderRequest} and {@code RemoteBuilder} properties
     * This method is implementation optional, as selection by builder properties might be enough, and backward compatible.
     * @param slaveBuilders
     * @param request
     * @return {@code RemoteBuilder} or {@code null} if there is no adequate selection
     */
    default RemoteBuilder select(List<RemoteBuilder> slaveBuilders, BaseBuilderRequest request){
        return select(slaveBuilders);
    }
}
