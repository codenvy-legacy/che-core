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
package org.eclipse.che.ide.navigation;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.ui.window.Window;
import org.eclipse.che.ide.util.loging.Log;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

/**
 * The implementation of {@link NavigateToFileView} view.
 *
 * @author Ann Shumilova
 * @author Artem Zatsarynnyy
 */
@Singleton
public class NavigateToFileViewImpl extends Window implements NavigateToFileView {

    interface NavigateToFileViewImplUiBinder extends UiBinder<Widget, NavigateToFileViewImpl> {
    }

    @UiField(provided = true)
    SuggestBox files;

    @UiField(provided = true)
    CoreLocalizationConstant locale;

    private ActionDelegate delegate;
    private HandlerRegistration handlerRegistration;

    @Inject
    public NavigateToFileViewImpl(CoreLocalizationConstant locale, NavigateToFileViewImplUiBinder uiBinder) {
        this.locale = locale;
        this.setTitle(locale.navigateToFileViewTitle());
        files = new SuggestBox(new MySuggestOracle());

        files.getValueBox().addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent event) {
                if (KeyCodes.KEY_ESCAPE == event.getNativeKeyCode()) {
                    close();
                }
            }
        });

        files.addSelectionHandler(new SelectionHandler<SuggestOracle.Suggestion>() {
            @Override
            public void onSelection(SelectionEvent<SuggestOracle.Suggestion> event) {
                delegate.onFileSelected();
            }
        });

        Widget widget = uiBinder.createAndBindUi(this);
        this.setWidget(widget);
        getFooter().setVisible(false);
    }

    /** {@inheritDoc} */
    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        files.setEnabled(false);
        this.hide();
    }

    /** {@inheritDoc} */
    @Override
    public void showDialog() {
        this.files.setEnabled(true);
        new Timer() {
            @Override
            public void run() {
                String warning = locale.navigateToFileSearchIsCaseSensitive();
                files.setText(warning);
                files.getValueBox().selectAll();

                files.setFocus(true);
            }
        }.schedule(300);

        handlerRegistration = files.getValueBox().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                String value = files.getText();
                String warning = locale.navigateToFileSearchIsCaseSensitive();

                if (value.equals(warning)) {
                    files.setText("");
                }
                handlerRegistration.removeHandler();
            }
        });
        super.show();
    }

    @Override
    protected void onClose() {
        //Do nothing
    }

    /** {@inheritDoc} */
    @Override
    public String getItemPath() {
        return files.getValue();
    }

    /** {@inheritDoc} */
    @Override
    public void clearInput() {
        files.getValueBox().setValue("");
    }

    private class MySuggestOracle extends SuggestOracle {
        /** {@inheritDoc} */
        @Override
        public boolean isDisplayStringHTML() {
            return true;
        }

        @Override
        public void requestSuggestions(final Request request, final Callback callback) {
            delegate.onRequestSuggestions(request.getQuery(), new AsyncCallback<List<ItemReference>>() {
                /** {@inheritDoc} */
                @Override
                public void onSuccess(List<ItemReference> result) {
                    final List<SuggestOracle.Suggestion> suggestions = new ArrayList<>(result.size());
                    for (final ItemReference item : result) {
                        suggestions.add(new SuggestOracle.Suggestion() {
                            @Override
                            public String getDisplayString() {
                                return getDisplayName(item);
                            }

                            @Override
                            public String getReplacementString() {
                                return item.getPath();
                            }
                        });
                    }

                    callback.onSuggestionsReady(request, new Response(suggestions));
                }

                /** {@inheritDoc} */
                @Override
                public void onFailure(Throwable caught) {
                    Log.error(NavigateToFileViewImpl.class, "Failed to search files.");
                }
            });
        }

        /** Returns the formed display name of the specified path. */
        private String getDisplayName(ItemReference item) {
            final String path = item.getPath();
            final String itemName = path.substring(path.lastIndexOf('/') + 1);
            final String itemPath = path.replaceFirst("/", "");
            String displayString = itemName + "   (" + itemPath.substring(0, itemPath.length() - itemName.length() - 1) + ")";

            String[] parts = displayString.split(" ");
            if (parts.length > 1) {
                displayString = parts[0];
                displayString += " <span style=\"color: #989898;\">";
                for (int i = 1; i < parts.length; i++) {
                    displayString += parts[i];
                }
                displayString += "</span>";
            }
            return displayString;
        }
    }
}
