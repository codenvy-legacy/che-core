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
package org.eclipse.che.ide.jseditor.client.texteditor;

import java.util.List;

import org.eclipse.che.ide.api.texteditor.HandlesUndoRedo;
import org.eclipse.che.ide.jseditor.client.codeassist.AdditionalInfoCallback;
import org.eclipse.che.ide.jseditor.client.codeassist.CompletionProposal;
import org.eclipse.che.ide.jseditor.client.codeassist.CompletionsSource;
import org.eclipse.che.ide.jseditor.client.position.PositionConverter;

import com.google.gwt.user.client.ui.Composite;

public abstract class CompositeEditorWidget extends Composite implements EditorWidget {

    @Override
    public LineStyler getLineStyler() {
        return null;
    }

    @Override
    public void onResize() {
        // Does nothing by default
    }

    @Override
    public HandlesUndoRedo getUndoRedo() {
        return null;
    }


    @Override
    public PositionConverter getPositionConverter() {
        return null;
    }

    @Override
    public void showCompletionsProposals(final List<CompletionProposal> proposals) {
        // does nothing by default
    }

    @Override
    public void showCompletionProposals(final CompletionsSource completionsSource) {
        // does nothing by default
    }

    @Override
    public void showCompletionProposals() {
        // does nothing by default
    }

    @Override
    public void showCompletionProposals(final CompletionsSource completionsSource,
                                        final AdditionalInfoCallback additionalInfoCallback) {
        showCompletionProposals(completionsSource);
    }

    @Override
    public void refresh() {
        
    }
}
