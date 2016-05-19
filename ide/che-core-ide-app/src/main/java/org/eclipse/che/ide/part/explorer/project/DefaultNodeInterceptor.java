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
package org.eclipse.che.ide.part.explorer.project;

import com.google.inject.Singleton;

import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.api.project.node.interceptor.NodeInterceptor;

import java.util.List;

/**
 * //
 *
 * @author Vitalii Parfonov
 */
@Singleton
public class DefaultNodeInterceptor implements NodeInterceptor {
    @Override
    public Promise<List<Node>> intercept(Node parent, List<Node> children) {
        return Promises.resolve(children);
    }

    @Override
    public Integer weightOrder() {
        return 0;
    }
}
