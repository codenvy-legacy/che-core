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
package org.eclipse.che.ide.ui.loaders.requestLoader;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.inject.Inject;

import org.eclipse.che.ide.rest.AsyncRequestLoader;

import java.util.HashMap;
import java.util.Map;


/**
 * The loader for rest request.
 *
 * @author Andrey Plotnikov
 * @author Sergii Leschenko
 * @author Oleksii Orel
 */
public class IdeLoader implements AsyncRequestLoader {
    private static final String DEFAULT_MESSAGE = "Loading ...";

    private final PopupPanel             loader;
    private final MessageHeap            messageHeap;
    private final Label                  loaderTextField;
    private final RequestLoaderResources resources;

    private final Timer showInclusionTimer;


    /**
     * Create loader.
     */
    @Inject
    public IdeLoader(final RequestLoaderResources resources) {
        this.resources = resources;
        resources.Css().ensureInjected();

        messageHeap = new MessageHeap();

        loaderTextField = new Label();
        loaderTextField.setStyleName(resources.Css().textField());

        loader = new PopupPanel();
        loader.ensureDebugId("loader");
        loader.setGlassEnabled(true);
        loader.setGlassStyleName(resources.Css().glassStyle());
        loader.addStyleName(resources.Css().loader());
        loader.setVisible(false);

        final Grid grid = new Grid(1, 2);
        final FlowPanel pinionPanel = new FlowPanel();
        pinionPanel.add(new FlowPanel());
        pinionPanel.add(new FlowPanel());
        pinionPanel.setStyleName(resources.Css().pinionPanel());
        grid.setWidget(0, 0, pinionPanel);
        grid.setWidget(0, 1, loaderTextField);
        loader.add(grid);

        showInclusionTimer = new Timer() {
            @Override
            public void run() {
                loader.setGlassStyleName(resources.Css().glassStyle());
                loader.removeStyleName(resources.Css().hide());
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public void show() {
        //show with default message
        show(DEFAULT_MESSAGE);
    }

    /** {@inheritDoc} */
    @Override
    public void show(String message) {
        messageHeap.push(message);
        setMessage(message);
        loader.center();
        //add a delay before the loader showing because most of the process lasts less than 1s
        if (!showInclusionTimer.isRunning()) {
            loader.setGlassStyleName(resources.Css().loader());
            loader.addStyleName(resources.Css().hide());
            showInclusionTimer.schedule(1000);
        }
        loader.show();
    }

    /** {@inheritDoc} */
    @Override
    public void hide() {
        hide(DEFAULT_MESSAGE);
    }

    /** {@inheritDoc} */
    @Override
    public void hide(String message) {
        String newMessage = messageHeap.drop(message);
        if (newMessage != null) {
            setMessage(newMessage);
        }
        loader.hide();
        if (showInclusionTimer.isRunning()) {
            showInclusionTimer.cancel();
        }
    }

    public void setMessage(String message) {
        loaderTextField.setText(message);
    }

    public String getMessage() {
        return loaderTextField.getText();
    }

    private class MessageHeap {
        private final Map<String, Integer> messages = new HashMap<>();

        /**
         * Pushes message to heap
         *
         * @param message
         *         message for push
         */
        public void push(String message) {
            if (messages.containsKey(message)) {
                messages.put(message, messages.get(message) + 1);
            } else {
                messages.put(message, 1);
            }
        }

        /**
         * Drop message from heap
         *
         * @param message
         *         message for drop
         * @return any message from heap or <code>null</code> if heap does have message
         */
        public String drop(String message) {
            if (messages.isEmpty() || !messages.containsKey(message)) {
                return null;
            }

            int count = messages.get(message) - 1;
            if (count == 0) {
                messages.remove(message);

                // If dropped message that isn't displayed then do not update text
                if (!getMessage().equals(message)) {
                    return getMessage();
                }

                if (!messages.isEmpty()) {
                    return messages.keySet().iterator().next();
                }

                return null;
            } else {
                messages.put(message, count);
                return message;
            }
        }
    }
}