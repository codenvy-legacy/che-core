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
package org.eclipse.che.ide.ui.listbox;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.LabelElement;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Custom list box widget for use instead of com.google.gwt.user.client.ui.ListBox which based on select tag.
 *
 * @author Oleksii Orel
 */
public class CustomListBox extends FocusWidget implements HasChangeHandlers {

    private static final CustomListBoxResources RESOURCES = GWT.create(CustomListBoxResources.class);
    private static final FlowPanel              panel     = new FlowPanel();

    static {
        RESOURCES.getCSS().ensureInjected();
        panel.getElement().setAttribute("tabindex", "1");
        Document.get().getBody().appendChild(panel.getElement());
    }

    private final LabelElement currentItemLabel = Document.get().createLabelElement();
    private final FlowPanel    popupPanel       = new FlowPanel();
    private final String       optionsGroupName = "listBox-" + Document.get().createUniqueId();

    private int     selectedIndex        = -1;
    private int     defaultSelectedIndex = -1;
    private boolean isPopupPanelVisible  = false;


    public static CustomListBox wrap(Element element) {
        // Assert that the element is attached.
        assert Document.get().getBody().isOrHasChild(element);

        CustomListBox customListBox = new CustomListBox(element);

        // Mark it attached and remember it for cleanup.
        customListBox.onAttach();
        RootPanel.detachOnWindowClose(customListBox);

        return customListBox;
    }
    
    /**
     * Creates an empty custom list box.
     */
    public CustomListBox() {
        super(Document.get().createDivElement());

        getElement().appendChild(currentItemLabel);
        getElement().appendChild(RESOURCES.arrow().getSvg().getElement());

        popupPanel.addStyleName(RESOURCES.getCSS().dropdown());

        Window.addResizeHandler(new ResizeHandler() {
            @Override
            public void onResize(ResizeEvent event) {
                if (!isPopupPanelVisible) {
                    return;
                }
                updateCurrentPopupPosition();
            }
        });

        addDomHandler(new MouseUpHandler() {
            @Override
            public void onMouseUp(MouseUpEvent event) {
                panel.clear();
                if (isPopupPanelVisible) {
                    isPopupPanelVisible = false;
                } else {
                    if (getElement().hasAttribute("disabled")) {
                        return;
                    }
                    updateCurrentPopupPosition();
                    panel.add(popupPanel);
                    panel.getElement().focus();
                    isPopupPanelVisible = true;
                }
            }
        }, MouseUpEvent.getType());

        Element panelElement = popupPanel.getElement();

        DOM.sinkEvents(panelElement, Event.ONCHANGE | Event.ONCLICK);
        DOM.setEventListener(panelElement, new EventListener() {
            @Override
            public void onBrowserEvent(Event event) {
                switch (DOM.eventGetType(event)) {
                    case Event.ONCLICK:
                        getElement().focus();
                        isPopupPanelVisible = false;
                        break;
                    case Event.ONCHANGE:
                        selectedIndex = -1;
                        NodeList<Element> selectionElements = popupPanel.getElement().getElementsByTagName("input");
                        for (int pos = 0; pos < selectionElements.getLength(); pos++) {
                            InputElement inputElement = (InputElement)selectionElements.getItem(pos);
                            if (inputElement.isChecked()) {
                                //update currentItemLabel
                                currentItemLabel.setInnerText(getItemText(pos));
                                selectedIndex = pos;
                                break;
                            }
                        }
                        fireChangeEvent();
                        if (selectedIndex == -1) {
                            currentItemLabel.setInnerText("");
                        }
                        break;
                }
            }
        });

        setStyleName(RESOURCES.getCSS().listBox());
    }

    private void updateCurrentPopupPosition() {
        panel.getElement().setAttribute("style", "top: " + getElement().getAbsoluteBottom() + "px;" +
                                                 "left: " + getElement().getAbsoluteLeft() + "px;" +
                                                 "width: " + getElement().getOffsetWidth() + "px;" +
                                                 "position:fixed;");
    }

    private void fireChangeEvent() {
        DomEvent.fireNativeEvent(Document.get().createChangeEvent(), this);
    }

    private InputElement getListItemElement(int index) {
        final Element optionElement = (Element)popupPanel.getElement().getChild(index);

        return (InputElement)optionElement.getElementsByTagName("input").getItem(0);
    }

    /**
     * This constructor may be used by subclasses to explicitly use an existing element.
     *
     * @param element
     *         the element to be used
     */
    protected CustomListBox(Element element) {
        super(element);
    }

    /**
     * Adds an ChangeHandler.
     *
     * @param handler
     *         the change handler
     */
    public HandlerRegistration addChangeHandler(ChangeHandler handler) {
        return addDomHandler(handler, ChangeEvent.getType());
    }

    /**
     * Adds an item to the list box.
     *
     * @param item
     *         the text of the item to be added
     */
    public void addItem(String item) {
        this.insertItem(item);
    }

    /**
     * Adds an item to the list box, specifying an initial value for the item.
     *
     * @param item
     *         the text of the item to be added
     * @param value
     *         the item's value, to be submitted if it is part of a
     *         {@link com.google.gwt.user.client.ui.FormPanel}; cannot be <code>null</code>
     */
    public void addItem(String item, String value) {
        this.insertItem(item, value);
    }

    /**
     * Removes all items from the list box.
     */
    public void clear() {
        selectedIndex = -1;
        currentItemLabel.setInnerText("");
        popupPanel.getElement().removeAllChildren();
    }

    /**
     * Gets the number of items present in the list box.
     *
     * @return the number of items
     */
    public int getItemCount() {
        return popupPanel.getElement().getChildCount();
    }

    /**
     * Gets the text associated with the item at the specified index.
     *
     * @param index
     *         the index of the item whose text is to be retrieved
     * @return the text associated with the item
     * @throws IndexOutOfBoundsException
     *         if the index is out of range
     */
    public String getItemText(int index) {
        checkIndex(index);
        final Element optionElement = (Element)popupPanel.getElement().getChild(index);
        final LabelElement labelElement = (LabelElement)optionElement.getElementsByTagName("label").getItem(0);

        return labelElement.getInnerText();
    }

    /**
     * Gets the text for currently selected item. If multiple items are selected,
     * this method will return the text of the first selected item.
     *
     * @return the text for selected item, or {@code null} if none is selected
     */
    public String getSelectedItemText() {
        int index = getSelectedIndex();

        return index == -1 ? null : getItemText(index);
    }

    /**
     * Gets the currently-selected item.
     *
     * @return the selected index, or <code>-1</code> if none is selected
     */
    public int getSelectedIndex() {
        return selectedIndex;
    }

    /**
     * Gets the value associated with the item at a given index.
     *
     * @param index
     *         the index of the item to be retrieved
     * @return the item's associated value
     * @throws IndexOutOfBoundsException
     *         if the index is out of range
     */
    public String getValue(int index) {
        checkIndex(index);
        final Element optionElement = (Element)popupPanel.getElement().getChild(index);
        final InputElement inputElement = (InputElement)optionElement.getElementsByTagName("input").getItem(0);

        return inputElement.getValue();
    }

    /**
     * Gets the value for currently selected item.
     *
     * @return the value for selected item, or {@code null} if none is selected
     */
    public String getValue() {
        int index = getSelectedIndex();

        return index == -1 ? null : getValue(index);
    }

    /**
     * Inserts an item into the custom list box.
     *
     * @param item
     *         the text of the item to be inserted
     */
    public void insertItem(String item) {
        this.insertItem(item, item);
    }

    /**
     * Inserts an item into the list box.
     *
     * @param item
     *         the text of the item to be inserted.
     * @param value
     *         the item's value.
     */
    public void insertItem(String item, String value) {
        //create new widget
        final RadioButton radioButton = new RadioButton(optionsGroupName, item);
        //remove the default gwt-RadioButton style
        radioButton.removeStyleName("gwt-RadioButton");
        //set value
        final InputElement inputElement = (InputElement)radioButton.getElement().getElementsByTagName("input").getItem(0);
        inputElement.removeAttribute("tabindex");
        inputElement.setAttribute("value", value);
        //set default state
        if (defaultSelectedIndex > -1 && popupPanel.getElement().getChildCount() == defaultSelectedIndex) {
            inputElement.setChecked(true);
            currentItemLabel.setInnerText(item);
        }
        //add to widget
        popupPanel.add(radioButton);
    }

    /**
     * Sets custom height inside widget as height and line-height properties.
     *
     * @param height
     */
    public void setHeight(String height) {
        this.getElement().getStyle().setProperty("height", height);
        currentItemLabel.setAttribute("style", "line-height: " + height + ";");
        popupPanel.getElement().setAttribute("style", "line-height: " + height + ";");
    }

    /**
     * Determines whether an individual list item is selected.
     *
     * @param index
     *         the index of the item to be tested
     * @return <code>true</code> if the item is selected
     * @throws IndexOutOfBoundsException
     *         if the index is out of range
     */
    public boolean isItemSelected(int index) {
        checkIndex(index);

        return selectedIndex == index;
    }

    /**
     * Removes the item at the specified index.
     *
     * @param index
     *         the index of the item to be removed
     * @throws IndexOutOfBoundsException
     *         if the index is out of range
     */
    public void removeItem(int index) {
        checkIndex(index);
        if (index == selectedIndex) {
            currentItemLabel.setInnerText("");
            selectedIndex = -1;
        }
        popupPanel.getElement().removeChild(popupPanel.getElement().getChild(index));
    }

    /**
     * Sets whether an individual list item is selected.
     *
     * @param index
     *         the index of the item to be selected or unselected
     * @param selected
     *         <code>true</code> to select the item
     */
    public void setItemSelected(int index, boolean selected) {
        if (index < 0 || index >= getItemCount()) {
            return;
        }
        if (selected) {
            selectedIndex = index;
            currentItemLabel.setInnerText(getItemText(index));
        }
        final InputElement inputElement = getListItemElement(index);
        inputElement.setChecked(selected);
    }

    /**
     * Sets the text associated with the item at a given index.
     *
     * @param index
     *         the index of the item to be set
     * @param text
     *         the item's new text
     * @throws IndexOutOfBoundsException
     *         if the index is out of range
     */
    public void setItemText(int index, String text) {
        checkIndex(index);
        final Element optionElement = (Element)popupPanel.getElement().getChild(index);
        final LabelElement labelElement = (LabelElement)optionElement.getElementsByTagName("label").getItem(0);
        labelElement.setInnerText(text);
        if (selectedIndex == index) {
            currentItemLabel.setInnerText(text);
        }
    }

    /**
     * Sets the currently selected index.
     *
     * @param index
     *         the index of the item to be selected
     */
    public void setSelectedIndex(int index) {
        if (index < 0) {
            return;
        }
        //set default index if not added options yet
        if (index >= getItemCount()) {
            defaultSelectedIndex = index;
            return;
        }
        selectedIndex = index;
        currentItemLabel.setInnerText(getItemText(index));
        final InputElement inputElement = getListItemElement(index);
        inputElement.setChecked(true);
    }

    /**
     * Sets the value associated with the item at a given index.
     *
     * @param index
     *         the index of the item to be set
     * @param value
     *         the item's new value
     * @throws IndexOutOfBoundsException
     *         if the index is out of range
     */
    public void setValue(int index, String value) {
        checkIndex(index);
        final InputElement inputElement = getListItemElement(index);
        inputElement.setValue(value);
    }

    /**
     * @see com.google.gwt.user.client.ui.UIObject#onEnsureDebugId(String)
     */
    @Override
    protected void onEnsureDebugId(String baseID) {
        super.onEnsureDebugId(baseID);

        ensureDebugId(this.popupPanel.getElement(), baseID, "popup-panel");
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= getItemCount()) {
            throw new IndexOutOfBoundsException();
        }
    }
}
