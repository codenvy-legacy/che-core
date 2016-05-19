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

/**
 * Selects the 'best' SlaveBuilder from the List according to implementation. BuildQueue uses implementation of this interface fo
 * find the 'best' slave-builder for processing incoming build request. If more then one slave-builder available then BuildQueue collects
 * them (their front-ends which are represented by SlaveBuilder) and passes to implementation of this interface. This implementation
 * should select the 'best' one.
 *
 * @author andrew00x
 */
public interface BuilderSelectionStrategy {
    RemoteBuilder select(List<RemoteBuilder> slaveBuilders);
}
