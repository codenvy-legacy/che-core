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
package org.eclipse.che.ide.api.action;

import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.util.ListenerManager;
import com.google.gwt.resources.client.ImageResource;

import org.vectomatic.dom.svg.ui.SVGResource;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

/**
 * The presentation of an action in a specific place in the user interface.
 *
 * @author Evgen Vidolob
 * @author Vlad Zhukovskyi
 */
public final class Presentation {

    private Map<String, Object> userMap = new HashMap<>();

    /**
     * Defines tool tip for button at tool bar or text for element at menu
     * value: String
     */
    public static final String PROP_TEXT        = "text";

    /** value: String */
    public static final String PROP_DESCRIPTION = "description";

    /** value: Icon */
    public static final String PROP_ICON        = "icon";

    /** value: Boolean */
    public static final String PROP_VISIBLE     = "visible";

    /** The actual value is a Boolean. */
    public static final String PROP_ENABLED     = "enabled";

    private ListenerManager<PropertyChangeListener> myChangeSupport;
    private String                                  text;
    private String                                  myDescription;
    private ImageResource                           myIcon;
    private SVGResource                             mySVGIcon;

    private boolean myVisible;
    private boolean myEnabled;

    public Presentation() {
        myChangeSupport = ListenerManager.create();
        myVisible = true;
        myEnabled = true;
    }

    public Presentation(final String text) {
        this();
        this.text = text;
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        myChangeSupport.add(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        myChangeSupport.remove(l);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        String oldText = this.text;
        this.text = text;
        firePropertyChange(PROP_TEXT, oldText, text);
    }

    public String getDescription() {
        return myDescription;
    }

    public void setDescription(String description) {
        String oldDescription = myDescription;
        myDescription = description;
        firePropertyChange(PROP_DESCRIPTION, oldDescription, myDescription);
    }

    public ImageResource getIcon() {
        return myIcon;
    }

    public SVGResource getSVGIcon() {
        return mySVGIcon;
    }

    public void setIcon(ImageResource icon) {
        ImageResource oldIcon = myIcon;
        myIcon = icon;
        firePropertyChange(PROP_ICON, oldIcon, myIcon);
    }

    public void setSVGIcon(SVGResource icon) {
        SVGResource oldIcon = mySVGIcon;
        mySVGIcon = icon;
        firePropertyChange(PROP_ICON, oldIcon, mySVGIcon);
    }

    public boolean isVisible() {
        return myVisible;
    }

    public void setVisible(boolean visible) {
        boolean oldVisible = myVisible;
        myVisible = visible;
        firePropertyChange(PROP_VISIBLE, oldVisible ? Boolean.TRUE : Boolean.FALSE, myVisible ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * Returns the state of this action.
     *
     * @return <code>true</code> if action is enabled, <code>false</code> otherwise
     */
    public boolean isEnabled() {
        return myEnabled;
    }

    /**
     * Sets whether the action enabled or not. If an action is disabled, {@link Action#actionPerformed}
     * won't be called. In case when action represents a button or a menu item, the
     * representing button or item will be greyed out.
     *
     * @param enabled
     *         <code>true</code> if you want to enable action, <code>false</code> otherwise
     */
    public void setEnabled(boolean enabled) {
        boolean oldEnabled = myEnabled;
        myEnabled = enabled;
        firePropertyChange(PROP_ENABLED, oldEnabled ? Boolean.TRUE : Boolean.FALSE, myEnabled ? Boolean.TRUE : Boolean.FALSE);
    }

    public final void setEnabledAndVisible(boolean enabled) {
        setEnabled(enabled);
        setVisible(enabled);
    }

    public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        final PropertyChangeEvent event = new PropertyChangeEvent(this, propertyName, oldValue, newValue);
        myChangeSupport.dispatch(new ListenerManager.Dispatcher<PropertyChangeListener>() {
            @Override
            public void dispatch(PropertyChangeListener listener) {
                listener.onPropertyChange(event);
            }
        });
    }

    public Presentation clone() {
        Presentation presentation = new Presentation(getText());
        presentation.myDescription = myDescription;
        presentation.myEnabled = myEnabled;
        presentation.myIcon = myIcon;
        presentation.mySVGIcon = mySVGIcon;
        presentation.myVisible = myVisible;
        return presentation;
    }

    public void putClientProperty(@NotNull String key, @Nullable Object value) {
        Object oldValue = userMap.get(key);
        userMap.put(key, value);
        firePropertyChange(key, oldValue, value);
    }

    /**
     * Return user client property by specified key.
     *
     * @param key
     *         user client property key
     * @return object, stored by property key
     */
    public Object getClientProperty(String key) {
        if (key == null) {
            return null;
        }

        return userMap.get(key);
    }

    @Override
    public String toString() {
        return text + " (" + myDescription + ")";
    }

}
