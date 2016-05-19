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
package org.eclipse.che.ide.jseditor.client.codeassist;

import org.eclipse.che.ide.jseditor.client.texteditor.TextEditor;

/**
 * A code assist processor proposes completions for a particular content type.
 * <p>
 * This interface must be implemented by clients. Implementers should be
 * registered with a code assistant in order to get involved in the
 * assisting process.
 * </p>
 *
 * @author <a href="mailto:evidolob@exoplatform.com">Evgen Vidolob</a>
 * @version $Id:
 */
public interface CodeAssistProcessor {

    /**
     * Returns a list of completion proposals based on the
     * specified location within the document that corresponds
     * to the current cursor position within the text view.
     *
     * @param view
     *         the editor whose document is used to compute the proposals
     * @param offset
     *         an offset within the document for which completions should be computed
     * @return an array of completion proposals or <code>null</code> if no proposals are possible
     */
    void computeCompletionProposals(TextEditor editor, int offset, CodeAssistCallback callback);

    /**
     * Returns the reason why this content assist processor
     * was unable to produce any completion proposals or context information.
     *
     * @return an error message or <code>null</code> if no error occurred
     */
    String getErrorMessage();
}
