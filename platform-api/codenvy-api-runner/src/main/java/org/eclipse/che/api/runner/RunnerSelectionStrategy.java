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
package org.eclipse.che.api.runner;

import java.util.List;

/**
 * Selects the 'best' RemoteRunner from the List according to implementation. RunQueue uses implementation of this interface fo
 * find the 'best' slave-runner for processing incoming request for running application. If more then one slave-runner available then
 * RunQueue collects them (their front-ends which are represented by RemoteRunner) and passes to implementation of this interface.
 * This implementation should select the 'best' one.
 *
 * @author andrew00x
 */
public interface RunnerSelectionStrategy {
    RemoteRunner select(List<RemoteRunner> remoteRunners);
}
