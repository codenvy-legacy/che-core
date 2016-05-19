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
package org.eclipse.che.ide.workspace;

import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;

/**
 * Implementation of WorkBenchPartController, used with SplitLayoutPanel as container
 *
 * @author <a href="mailto:evidolob@codenvy.com">Evgen Vidolob</a>
 */
public class WorkBenchPartControllerImpl implements WorkBenchPartController {
    public static final int DURATION = 200;

    private final SplitLayoutPanel   splitLayoutPanel;
    private final SimplePanel        widget;
    private final HideWidgetCallback hideWidgetCallback;

    public WorkBenchPartControllerImpl(SplitLayoutPanel splitLayoutPanel, SimplePanel widget, HideWidgetCallback hideWidgetCallback) {
        this.splitLayoutPanel = splitLayoutPanel;
        this.widget = widget;
        this.hideWidgetCallback = hideWidgetCallback;

        splitLayoutPanel.setWidgetHidden(widget, true);
        splitLayoutPanel.forceLayout();
    }

    /** {@inheritDoc} */
    @Override
    public double getSize() {
        return splitLayoutPanel.getWidgetSize(widget);
    }

    /** {@inheritDoc} */
    @Override
    public void setSize(double size) {
        splitLayoutPanel.setWidgetSize(widget, size);
        splitLayoutPanel.animate(DURATION);
    }

    /** {@inheritDoc} */
    @Override
    public void setHidden(boolean hidden) {
        if (!hidden) {
            splitLayoutPanel.setWidgetHidden(widget, false);
        } else {
            hideWidgetCallback.addWidgetToHide(widget);
        }
        splitLayoutPanel.setWidgetSize(widget, hidden ? 0 : getSize());
        splitLayoutPanel.animate(DURATION, hideWidgetCallback);
    }

}
