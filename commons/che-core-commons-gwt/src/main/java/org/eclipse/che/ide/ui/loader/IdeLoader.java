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
package org.eclipse.che.ide.ui.loader;

import org.eclipse.che.ide.rest.AsyncRequestLoader;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.inject.Inject;

import java.util.HashMap;
import java.util.Map;


/**
 * The loader for rest request.
 *
 * @author Andrey Plotnikov
 * @author Sergii Leschenko
 */
public class IdeLoader implements AsyncRequestLoader {
    protected final String DEFAULT_MESSAGE = "Loading ...";

    private final MessageHeap messageHeap;
    private final ViewLoader  loader;

    /**
     * Create loader.
     */
    @Inject
    public IdeLoader(LoaderResources resources) {
        messageHeap = new MessageHeap();
        loader = new ViewLoader(resources);
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
        loader.setMessage(message);
        loader.center();
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
            loader.setMessage(newMessage);
        } else {
            loader.hide();
        }
    }

    private static class ViewLoader extends PopupPanel {
        private Grid grid;

        public ViewLoader(LoaderResources resources) {
            resources.Css().ensureInjected();
            FlowPanel container = new FlowPanel();
            HTML pinionWidget = new HTML("<i></i><i></i>");
            pinionWidget.getElement().setClassName(resources.Css().pinion());
            grid = new Grid(1, 2);
            grid.setWidget(0, 0, pinionWidget);
            container.add(grid);
            this.add(container);
            this.ensureDebugId("loader");

            setGlassEnabled(true);
            getGlassElement().getStyle().setOpacity(0);
            getGlassElement().getStyle().setZIndex(9999998);
            getElement().getStyle().setZIndex(9999999);
        }

        public void setMessage(String message) {
            grid.setText(0, 1, message);
        }

        public String getMessage() {
            return grid.getText(0, 1);
        }
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
                if (!loader.getMessage().equals(message)) {
                    return loader.getMessage();
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