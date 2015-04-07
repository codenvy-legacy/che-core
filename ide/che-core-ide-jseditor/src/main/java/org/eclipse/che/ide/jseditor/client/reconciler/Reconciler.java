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
package org.eclipse.che.ide.jseditor.client.reconciler;

import org.eclipse.che.ide.jseditor.client.document.UseDocumentHandle;
import org.eclipse.che.ide.jseditor.client.events.DocumentChangeHandler;
import org.eclipse.che.ide.jseditor.client.texteditor.TextEditor;

/**
 * An <code>Reconciler</code> defines and maintains a model of the content of the text view document in the presence of changes applied to
 * this document. Reconciler have a list of {@link ReconcilingStrategy} objects each of which is registered for a particular document
 * content type. The reconciler uses the strategy objects to react on the changes applied to the text view document.
 * 
 * @author <a href="mailto:evidolob@exoplatform.com">Evgen Vidolob</a>
 * @version $Id:
 */
public interface Reconciler extends UseDocumentHandle, DocumentChangeHandler {

    /**
     * Installs the reconciler on the given text view. After this method has been finished, the reconciler is operational, i.e., it works
     * without requesting further client actions until <code>uninstall</code> is called.
     */
    void install(TextEditor editor);

    /**
     * Removes the reconciler from the text view it has previously been installed on.
     */
    void uninstall();

    /**
     * Returns the reconciling strategy registered with the reconciler for the specified content type.
     * 
     * @param contentType the content type for which to determine the reconciling strategy
     * @return the reconciling strategy registered for the given content type, or <code>null</code> if there is no such strategy
     */
    ReconcilingStrategy getReconcilingStrategy(String contentType);

    /**
     * Returns the partitioning this reconciler is using.
     * 
     * @return the partitioning this reconciler is using
     */
    String getDocumentPartitioning();

    void addReconcilingStrategy(String contentType, ReconcilingStrategy strategy);
}
