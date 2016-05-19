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
package org.eclipse.che.ide.ui.status;

import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import javax.validation.constraints.NotNull;

/**
 * @author Vlad Zhukovskiy
 */
public abstract class StatusText {

    public static final String DEFAULT_EMPTY_TEXT = "Nothing to show";
    private             String myText             = "";
    private Widget widget;

    protected StatusText(Widget widget) {
        this();
        bind(widget);
    }

    public StatusText() {
        setText(DEFAULT_EMPTY_TEXT);
    }

    public void bind(Widget widget) {
        this.widget = widget;
    }

    @NotNull
    public String getText() {
        return myText;
    }

    public StatusText setText(String text) {
        return clear().appendText(text);
    }

    public StatusText appendText(String text) {
        myText += text;
        return this;
    }

    public StatusText clear() {
        myText = "";
        return this;
    }

    protected abstract boolean isStatusVisible();

    public void paint() {
        if (!isStatusVisible()) {
            return;
        }

        VerticalPanel verticalPanel = new VerticalPanel();

        verticalPanel.setHeight("50px");
        verticalPanel.setWidth("100%");

        verticalPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        verticalPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);

        verticalPanel.add(new Label(getText()));

        widget.getElement().appendChild(verticalPanel.getElement());
    }
}
