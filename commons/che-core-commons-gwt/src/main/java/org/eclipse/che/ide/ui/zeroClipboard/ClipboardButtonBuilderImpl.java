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
package org.eclipse.che.ide.ui.zeroclipboard;

import org.eclipse.che.ide.MimeType;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.vectomatic.dom.svg.ui.SVGImage;

import javax.validation.constraints.NotNull;

/**
 * Implementation of ClipboardButtonBuilder is able to create "copy to clipboard" or "select button"
 * according to state of ZeroClipboard library.
 *
 * @author Oleksii Orel
 * @author Kevin Pollet
 */
public class ClipboardButtonBuilderImpl implements ClipboardButtonBuilder {

    private final ZeroClipboardResources res;
    private       Widget                 resourceWidget;
    private       Widget                 parentWidget;
    private       SVGImage               svgImage;
    private       String                 mimeType;
    private       String                 promptReadyToCopy;
    private       String                 promptAfterCopy;
    private       String                 promptCopyError;
    private       String                 promptReadyToSelect;


    @Inject
    public ClipboardButtonBuilderImpl(ZeroClipboardResources res, ZeroClipboardConstant locale) {
        this.res = res;
        promptReadyToCopy = locale.promptReadyToCopy();
        promptAfterCopy = locale.promptAfterCopy();
        promptCopyError = locale.promptCopyError();
        promptReadyToSelect = locale.promptReadyToSelect();
        mimeType = MimeType.TEXT_PLAIN;
    }

    @Override
    public ClipboardButtonBuilder withResourceWidget(Widget resourceWidget) {
        this.resourceWidget = resourceWidget;
        return this;
    }

    @Override
    public ClipboardButtonBuilder withParentWidget(Widget parentWidget) {
        this.parentWidget = parentWidget;
        return this;
    }

    @Override
    public ClipboardButtonBuilder withSvgImage(@NotNull SVGImage svgImage) {
        this.svgImage = svgImage;
        return this;
    }

    @Override
    public ClipboardButtonBuilder withMimeType(@NotNull String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    @Override
    public ClipboardButtonBuilder withPromptReadyToCopy(@NotNull String promptReadyToCopy) {
        this.promptReadyToCopy = promptReadyToCopy;
        return this;
    }

    @Override
    public ClipboardButtonBuilder withPromptAfterCopy(@NotNull String promptAfterCopy) {
        this.promptAfterCopy = promptAfterCopy;
        return this;
    }

    @Override
    public ClipboardButtonBuilder withPromptCopyError(@NotNull String promptCopyError) {
        this.promptCopyError = promptCopyError;
        return this;
    }

    @Override
    public ClipboardButtonBuilder withPromptReadyToSelect(@NotNull String promptReadyToSelect) {
        this.promptReadyToSelect = promptReadyToSelect;
        return this;
    }

    @Override
    public Element build() {
        Element button = null;
        if (resourceWidget != null) {
            Element buttonImage = svgImage != null ? svgImage.getElement() : new SVGImage(res.clipboard()).getElement();
            button = buildCopyToClipboardButton(resourceWidget.getElement(),
                                                buttonImage,
                                                res.clipboardCss().clipboardButton(),
                                                mimeType,
                                                promptReadyToCopy,
                                                promptAfterCopy,
                                                promptCopyError,
                                                promptReadyToSelect);
            append(button);
        }
        return button;
    }

    /**
     * Append to parentWidget as a child element.
     *
     * @param element
     */
    private void append(Element element) {
        if (parentWidget == null && (resourceWidget == null || resourceWidget.getParent() == null)) {
            return;
        }
        Widget parent = parentWidget != null ? parentWidget : resourceWidget.getParent();
        parent.getElement().appendChild(element);
    }

    /**
     * Build ZeroClipboard button.
     *
     * @param textBox
     * @param image
     * @param className
     * @param readyCopyPrompt
     * @param afterCopyPrompt
     * @param copyErrorPrompt
     * @param readySelectPrompt
     */
    private native Element buildCopyToClipboardButton(Element textBox, Element image, String className,
                                                      String mimeType, String readyCopyPrompt, String afterCopyPrompt,
                                                      String copyErrorPrompt, String readySelectPrompt) /*-{
        var button = document.createElement('div');
        var tooltip = document.createElement('span');
        button.setAttribute('class', className);
        button.appendChild(image);
        button.appendChild(tooltip);
        if (typeof $wnd.ZeroClipboard !== 'undefined') {
            var client = new $wnd.ZeroClipboard(button);
            client.on('ready', function (event) {
                tooltip.innerHTML = readyCopyPrompt;
                client.on('copy', function (event) {
                    var data;
                    if (mimeType === 'text/plain') {
                        data = textBox.value;
                        if (!data) {
                            data = textBox.innerText;
                        }
                    } else {
                        data = textBox.innerHTML;
                    }
                    event.clipboardData.setData(mimeType, data);
                });
                client.on('aftercopy', function (event) {
                    tooltip.innerHTML = afterCopyPrompt;
                    client.unclip();
                    setTimeout(function () {
                        client.clip(button);
                        tooltip.innerHTML = readyCopyPrompt;
                    }, 3000);
                });
            });
            client.on('error', function (event) {
                console.log('ZeroClipboard error of type "' + event.name + '": ' + event.message);
                tooltip.innerHTML = copyErrorPrompt;
                $wnd.ZeroClipboard.destroy();
                setTimeout(function () {
                    tooltip.innerHTML = readyCopyPrompt;
                }, 5000);
            });
        }
        else {
            tooltip.innerHTML = readySelectPrompt;
            button.onclick = function () {
                if (typeof textBox.select !== 'undefined') {
                    textBox.select();
                } else if ($wnd.getSelection()) {
                    var range = document.createRange();
                    range.selectNodeContents(textBox);
                    $wnd.getSelection().removeAllRanges();
                    $wnd.getSelection().addRange(range);
                }
            };
        }
        return button;
    }-*/;
}
