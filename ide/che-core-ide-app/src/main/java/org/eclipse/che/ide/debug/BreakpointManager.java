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

package org.eclipse.che.ide.debug;

import java.util.List;

/**
 * Breakpoint manager.
 *
 * @author Anatoliy Bazko
 */
public interface BreakpointManager {

    /**
     * Toggle / untoggle breakpoint.
     */
    void changeBreakpointState(int lineNumber);

    /**
     * Removes all breakpoints.
     */
    void removeAllBreakpoints();

    boolean isCurrentBreakpoint(int lineNumber);

    /**
     * @return all breakpoints
     */
    List<Breakpoint> getBreakpointList();

    /**
     * If debugger has stopped at specific line then this method is invoked.
     */
    void setCurrentBreakpoint(int lineNumber);
    
    /**
     * Removes current breakpoint mark.
     */
    void removeCurrentBreakpoint();
}
