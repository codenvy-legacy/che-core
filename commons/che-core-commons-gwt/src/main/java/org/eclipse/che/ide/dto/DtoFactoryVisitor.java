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
package org.eclipse.che.ide.dto;

/**
 * Visitor pattern. Generally needed to register {@link DtoProvider}s (by generated code) in {@link DtoFactory}.
 * Class, which contains generated code for client side, implements this interface. When all implementations of this
 * interface is instantiated - its {@link #accept(DtoFactory)} method will be called.
 *
 * @author <a href="mailto:azatsarynnyy@codenvy.com">Artem Zatsarynnyy</a>
 */
public interface DtoFactoryVisitor {
    void accept(DtoFactory dtoFactory);
}
