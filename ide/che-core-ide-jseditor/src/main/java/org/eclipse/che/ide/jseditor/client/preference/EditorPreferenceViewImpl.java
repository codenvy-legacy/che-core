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
package org.eclipse.che.ide.jseditor.client.preference;


import javax.inject.Inject;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.SimplePanel;

/** Implementation of the {@link EditorPreferenceView}. */
public class EditorPreferenceViewImpl extends Composite implements EditorPreferenceView {

    /** The UI binder instance. */
    private static final EditorTypePreferenceViewImplUiBinder UIBINDER    = GWT.create(EditorTypePreferenceViewImplUiBinder.class);

    @UiField
    SimplePanel editorSelectionSection;

    @UiField
    SimplePanel keymapsSection;

    @Inject
    public EditorPreferenceViewImpl() {
        initWidget(UIBINDER.createAndBindUi(this));
    }

    /** UI binder interface for the {@link EditorPreferenceViewImpl} component. */
    interface EditorTypePreferenceViewImplUiBinder extends UiBinder<HTMLPanel, EditorPreferenceViewImpl> {
    }

    @Override
    public AcceptsOneWidget getEditorTypeContainer() {
        return this.editorSelectionSection;
    }

    @Override
    public AcceptsOneWidget getKeymapsContainer() {
        return this.keymapsSection;
    }
}
