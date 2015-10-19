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
package org.eclipse.che.api.git.shared;

import org.eclipse.che.dto.shared.DTO;

/**
 * Request to add rebase branch
 *
 * @author Dror Cohen
 */
@DTO
public interface RebaseRequest {
	/** set rebase operation */
	void setOperation(String operation);
	/** @return operation used */
	String getOperation();
	/** @ return rebase branch */
	String getBranch();
	/** set rebase branch */
	void setBranch(String branch);
}
