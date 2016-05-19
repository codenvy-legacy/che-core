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

import elemental.events.KeyboardEvent.KeyCode;
import elemental.events.MouseEvent;

import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.ide.api.text.TypedRegion;
import org.eclipse.che.ide.jseditor.client.annotation.AnnotationModel;
import org.eclipse.che.ide.jseditor.client.annotation.AnnotationModelEvent;
import org.eclipse.che.ide.jseditor.client.annotation.ClearAnnotationModelEvent;
import org.eclipse.che.ide.jseditor.client.annotation.GutterAnnotationRenderer;
import org.eclipse.che.ide.jseditor.client.annotation.InlineAnnotationRenderer;
import org.eclipse.che.ide.jseditor.client.annotation.MinimapAnnotationRenderer;
import org.eclipse.che.ide.jseditor.client.annotation.QueryAnnotationsEvent;
import org.eclipse.che.ide.jseditor.client.changeintercept.ChangeInterceptorProvider;
import org.eclipse.che.ide.jseditor.client.changeintercept.TextChange;
import org.eclipse.che.ide.jseditor.client.changeintercept.TextChangeInterceptor;
import org.eclipse.che.ide.jseditor.client.codeassist.CodeAssistCallback;
import org.eclipse.che.ide.jseditor.client.codeassist.CodeAssistProcessor;
import org.eclipse.che.ide.jseditor.client.codeassist.CodeAssistant;
import org.eclipse.che.ide.jseditor.client.codeassist.CodeAssistantFactory;
import org.eclipse.che.ide.jseditor.client.codeassist.CompletionProposal;
import org.eclipse.che.ide.jseditor.client.codeassist.CompletionReadyCallback;
import org.eclipse.che.ide.jseditor.client.codeassist.CompletionsSource;
import org.eclipse.che.ide.jseditor.client.document.DocumentHandle;
import org.eclipse.che.ide.jseditor.client.editorconfig.TextEditorConfiguration;
import org.eclipse.che.ide.jseditor.client.events.CompletionRequestEvent;
import org.eclipse.che.ide.jseditor.client.events.CompletionRequestHandler;
import org.eclipse.che.ide.jseditor.client.events.DocumentChangeEvent;
import org.eclipse.che.ide.jseditor.client.events.GutterClickEvent;
import org.eclipse.che.ide.jseditor.client.events.GutterClickHandler;
import org.eclipse.che.ide.jseditor.client.events.TextChangeEvent;
import org.eclipse.che.ide.jseditor.client.events.TextChangeHandler;
import org.eclipse.che.ide.jseditor.client.events.doc.DocReadyWrapper;
import org.eclipse.che.ide.jseditor.client.events.doc.DocReadyWrapper.DocReadyInit;
import org.eclipse.che.ide.jseditor.client.gutter.Gutters;
import org.eclipse.che.ide.jseditor.client.gutter.HasGutter;
import org.eclipse.che.ide.jseditor.client.keymap.KeyBindingAction;
import org.eclipse.che.ide.jseditor.client.keymap.Keybinding;
import org.eclipse.che.ide.jseditor.client.minimap.HasMinimap;
import org.eclipse.che.ide.jseditor.client.partition.DocumentPartitioner;
import org.eclipse.che.ide.jseditor.client.position.PositionConverter;
import org.eclipse.che.ide.jseditor.client.quickfix.QuickAssistAssistant;
import org.eclipse.che.ide.jseditor.client.quickfix.QuickAssistProcessor;
import org.eclipse.che.ide.jseditor.client.quickfix.QuickAssistantFactory;
import org.eclipse.che.ide.jseditor.client.reconciler.Reconciler;
import org.eclipse.che.ide.jseditor.client.text.TextPosition;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Initialization controller for the text editor.
 * Sets-up (when available) the different components that depend on the document being ready.
 */
public class TextEditorInit<T extends EditorWidget> {

    /** The logger. */
    private static final Logger LOG = Logger.getLogger(TextEditorInit.class.getName());

    private final TextEditorConfiguration        configuration;
    private final EventBus                       generalEventBus;
    private final CodeAssistantFactory           codeAssistantFactory;
    private final QuickAssistantFactory          quickAssistantFactory;
    private final EmbeddedTextEditorPresenter<T> textEditor;


    /**
     * The quick assist assistant.
     */
    private QuickAssistAssistant quickAssist;

    public TextEditorInit(final TextEditorConfiguration configuration,
                          final EventBus generalEventBus,
                          final CodeAssistantFactory codeAssistantFactory,
                          final QuickAssistantFactory quickAssistantFactory,
                          final EmbeddedTextEditorPresenter<T> textEditor) {

        this.configuration = configuration;
        this.generalEventBus = generalEventBus;
        this.codeAssistantFactory = codeAssistantFactory;
        this.quickAssistantFactory = quickAssistantFactory;
        this.textEditor = textEditor;
    }

    /**
     * Initialize the text editor.
     * Sets itself as {@link org.eclipse.che.ide.jseditor.client.events.DocumentReadyEvent} handler.
     */
    public void init() {

        final DocReadyInit<TextEditorInit<T>> init = new DocReadyInit<TextEditorInit<T>>() {

            @Override
            public void initialize(final DocumentHandle documentHandle, final TextEditorInit<T> wrapped) {
                configurePartitioner(documentHandle);
                configureReconciler(documentHandle);
                configureAnnotationModel(documentHandle);
                configureCodeAssist(documentHandle);
                configureQuickAssist(documentHandle);
                configureChangeInterceptors(documentHandle);
            }
        };
        new DocReadyWrapper<TextEditorInit<T>>(generalEventBus, this.textEditor.getEditorHandle(), init, this);
    }

    /**
     * Configures the editor's DocumentPartitioner.
     * @param documentHandle the handle to the document
     */
    private void configurePartitioner(final DocumentHandle documentHandle) {
        final DocumentPartitioner partitioner = configuration.getPartitioner();
        if (partitioner != null) {
            partitioner.setDocumentHandle(documentHandle);
            documentHandle.getDocEventBus().addHandler(DocumentChangeEvent.TYPE, partitioner);
            partitioner.initialize();
        }
    }

    /**
     * Configures the editor's Reconciler.
     * @param documentHandle the handle to the document
     */
    private void configureReconciler(final DocumentHandle documentHandle) {
        final Reconciler reconciler = configuration.getReconciler();
        if (reconciler != null) {
            reconciler.setDocumentHandle(documentHandle);
            documentHandle.getDocEventBus().addHandler(DocumentChangeEvent.TYPE, reconciler);
            reconciler.install(textEditor);
        }
    }

    /**
     * Configures the editor's annotation model.
     * @param documentHandle the handle on the editor
     */
    private void configureAnnotationModel(final DocumentHandle documentHandle) {
        final AnnotationModel annotationModel = configuration.getAnnotationModel();
        if (annotationModel == null) {
            return;
        }
        // add the renderers (event handler) before the model (event source)

        // gutter renderer
        if (textEditor instanceof HasGutter && ((HasGutter)this.textEditor).getGutter() != null) {
            final GutterAnnotationRenderer annotationRenderer = new GutterAnnotationRenderer();
            annotationRenderer.setDocument(documentHandle.getDocument());
            annotationRenderer.setHasGutter(((HasGutter)this.textEditor).getGutter());
            documentHandle.getDocEventBus().addHandler(AnnotationModelEvent.TYPE, annotationRenderer);
            documentHandle.getDocEventBus().addHandler(ClearAnnotationModelEvent.TYPE, annotationRenderer);
        }

        // inline renderer
        final InlineAnnotationRenderer inlineAnnotationRenderer = new InlineAnnotationRenderer();
        inlineAnnotationRenderer.setDocument(documentHandle.getDocument());
        inlineAnnotationRenderer.setHasTextMarkers(this.textEditor.getHasTextMarkers());
        documentHandle.getDocEventBus().addHandler(AnnotationModelEvent.TYPE, inlineAnnotationRenderer);
        documentHandle.getDocEventBus().addHandler(ClearAnnotationModelEvent.TYPE, inlineAnnotationRenderer);

        // minimap renderer
        if (this.textEditor instanceof HasMinimap && ((HasMinimap)this.textEditor).getMinimap() != null) {
            final MinimapAnnotationRenderer minimapAnnotationRenderer = new MinimapAnnotationRenderer();
            minimapAnnotationRenderer.setDocument(documentHandle.getDocument());
            minimapAnnotationRenderer.setMinimap(((HasMinimap)this.textEditor).getMinimap());
            documentHandle.getDocEventBus().addHandler(AnnotationModelEvent.TYPE, minimapAnnotationRenderer);
            documentHandle.getDocEventBus().addHandler(ClearAnnotationModelEvent.TYPE, minimapAnnotationRenderer);
        }

        annotationModel.setDocumentHandle(documentHandle);
        documentHandle.getDocEventBus().addHandler(DocumentChangeEvent.TYPE, annotationModel);

        // the model listens to QueryAnnotation events
        documentHandle.getDocEventBus().addHandler(QueryAnnotationsEvent.TYPE, annotationModel);
    }

    /**
     * Configure the editor's code assistant.
     * @param documentHandle the handle on the document
     */
    private void configureCodeAssist(final DocumentHandle documentHandle) {
        if (this.codeAssistantFactory == null) {
            return;
        }
        final Map<String, CodeAssistProcessor> processors = configuration.getContentAssistantProcessors();

        if (processors != null && !processors.isEmpty()) {
            LOG.info("Creating code assistant.");

            final CodeAssistant codeAssistant = this.codeAssistantFactory.create(this.textEditor,
                                                                                 this.configuration.getPartitioner());
            for (String key: processors.keySet()) {
                codeAssistant.setCodeAssistantProcessor(key, processors.get(key));
            }

            final KeyBindingAction action = new KeyBindingAction() {
                @Override
                public void action() {
                    showCompletion(codeAssistant);
                }
            };
            final HasKeybindings hasKeybindings = this.textEditor.getHasKeybindings();
            hasKeybindings.addKeybinding(new Keybinding(true, false, false, false, KeyCode.SPACE, action));

            // handle CompletionRequest events that come from text operations instead of simple key binding
            documentHandle.getDocEventBus().addHandler(CompletionRequestEvent.TYPE, new CompletionRequestHandler() {
                @Override
                public void onCompletionRequest(final CompletionRequestEvent event) {
                    showCompletion(codeAssistant);
                }
            });
        } else {
            final KeyBindingAction action = new KeyBindingAction() {
                @Override
                public void action() {
                    showCompletion();
                }
            };
            final HasKeybindings hasKeybindings = this.textEditor.getHasKeybindings();
            hasKeybindings.addKeybinding(new Keybinding(true, false, false, false, KeyCode.SPACE, action));

            // handle CompletionRequest events that come from text operations instead of simple key binding
            documentHandle.getDocEventBus().addHandler(CompletionRequestEvent.TYPE, new CompletionRequestHandler() {
                @Override
                public void onCompletionRequest(final CompletionRequestEvent event) {
                    showCompletion();
                }
            });
        }
    }

    /**
     * Show the available completions.
     *
     * @param codeAssistant the code assistant
     */
    private void showCompletion(final CodeAssistant codeAssistant) {
        final int cursor = textEditor.getCursorOffset();
        if (cursor < 0) {
            return;
        }
        final CodeAssistProcessor processor = codeAssistant.getProcessor(cursor);
        if (processor != null) {
            this.textEditor.showCompletionProposals(new CompletionsSource() {
                @Override
                public void computeCompletions(final CompletionReadyCallback callback) {
                    // cursor must be computed here again so it's original value is not baked in
                    // the SMI instance closure - important for completion update when typing
                    final int cursor = textEditor.getCursorOffset();
                    codeAssistant.computeCompletionProposals(cursor, new CodeAssistCallback() {
                        @Override
                        public void proposalComputed(final List<CompletionProposal> proposals) {
                            callback.onCompletionReady(proposals);
                        }
                    });
                }
            });
        } else {
            showCompletion();
        }
    }

    /** Show the available completions. */
    private void showCompletion() {
        this.textEditor.showCompletionProposals();
    }

    /**
     * Sets up the quick assist assistant.
     * @param documentHandle the handle to the document
     */
    private void configureQuickAssist(final DocumentHandle documentHandle) {
        final QuickAssistProcessor processor = configuration.getQuickAssistProcessor();
        if (this.quickAssistantFactory != null && processor != null) {
            this.quickAssist = quickAssistantFactory.createQuickAssistant(this.textEditor);
            this.quickAssist.setQuickAssistProcessor(processor);
            documentHandle.getDocEventBus().addHandler(GutterClickEvent.TYPE, new GutterClickHandler() {
                @Override
                public void onGutterClick(final GutterClickEvent event) {
                    if (Gutters.ANNOTATION_GUTTER.equals(event.getGutterId())) {
                        final MouseEvent originalEvent = event.getEvent();
                        showQuickAssistant(event.getLineNumber(),
                                           originalEvent.getClientX(),
                                           originalEvent.getClientY());
                    }
                }
            });

            //add a key binding
            final KeyBindingAction action = new KeyBindingAction() {
                @Override
                public void action() {
                    final PositionConverter positionConverter = textEditor.getPositionConverter();
                    if (positionConverter != null) {
                        final TextPosition cursor = textEditor.getCursorPosition();
                        final int lineNumber = cursor.getLine();
                        final PositionConverter.PixelCoordinates pixelPos = positionConverter.textToPixel(cursor);
                        showQuickAssistant(lineNumber, pixelPos.getX(), pixelPos.getY());
                    }
                }
            };
            final HasKeybindings hasKeybindings = this.textEditor.getHasKeybindings();
            hasKeybindings.addKeybinding(new Keybinding(true, false, false, false, KeyCode.ONE, action));
            hasKeybindings.addKeybinding(new Keybinding(true, false, false, false, KeyCode.NUM_ONE, action));
        }
    }

    private void showQuickAssistant(final int lineNumber, int clientX, int clientY) {
        quickAssist.showPossibleQuickAssists(lineNumber, clientX, clientY);
    }

    private void configureChangeInterceptors(final DocumentHandle documentHandle) {
        final ChangeInterceptorProvider interceptors = configuration.getChangeInterceptorProvider();
        if (interceptors != null) {
            documentHandle.getDocEventBus().addHandler(TextChangeEvent.TYPE, new TextChangeHandler() {
                @Override
                public void onTextChange(final TextChangeEvent event) {
                    final TextChange change = event.getChange();
                    if (change == null) {
                        return;
                    }
                    final TextPosition from = change.getFrom();
                    if (from == null) {
                        return;
                    }
                    final int startOffset = documentHandle.getDocument().getIndexFromPosition(from);
                    final TypedRegion region = configuration.getPartitioner().getPartition(startOffset);
                    if (region == null) {
                        return;
                    }
                    final List<TextChangeInterceptor> filteredInterceptors = interceptors.getInterceptors(region.getType());
                    if (filteredInterceptors == null || filteredInterceptors.isEmpty()) {
                        return;
                    }
                    // don't apply the interceptors if the range end doesn't belong to the same partition
                    final TextPosition to = change.getTo();
                    if (to != null && ! from.equals(to)) {
                        final int endOffset = documentHandle.getDocument().getIndexFromPosition(to);
                        if (endOffset < region.getOffset() || endOffset > region.getOffset() + region.getLength()) {
                            return;
                        }
                    }
                    // stop as soon as one interceptors has modified the content
                    for (final TextChangeInterceptor interceptor: filteredInterceptors) {
                        final TextChange result = interceptor.processChange(change,
                                                                            documentHandle.getDocument().getReadOnlyDocument());
                        if (result != null) {
                            event.update(result);
                            break;
                        }
                    }
                }
            });
        }
    }
}
