package org.eclipse.che.ide.jseditor.client.codeassist;

import org.eclipse.che.ide.jseditor.client.codeassist.CompletionProposal.CompletionCallback;

/**
 * Extends {@link CompletionProposal} with the following
 * function:
 * <ul>
 * <li>Allow computation of the Completion with replacing or inserting text .</li>
 * </ul>
 *
 * @author Evgen Vidolob
 */
public interface CompletionProposalExtension {

    void getCompletion(boolean insert, CompletionCallback callback);
}
