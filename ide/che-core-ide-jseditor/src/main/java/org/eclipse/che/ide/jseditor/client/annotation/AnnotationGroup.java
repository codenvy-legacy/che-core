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
package org.eclipse.che.ide.jseditor.client.annotation;

import java.util.List;

import org.eclipse.che.ide.api.text.annotation.Annotation;

import elemental.dom.Element;

/**
 * A display for an annotation group (on the same line).
 */
public interface AnnotationGroup {

    /**
     * Add an annotation to the group.
     * 
     * @param annotation the annotation description
     * @param offset the position offset for the annotation
     */
    void addAnnotation(Annotation annotation, int offset);

    /**
     * Remove an annotation for the group.
     * 
     * @param annotation the annotation to remove
     * @param offset the offset for the annotation
     */
    void removeAnnotation(Annotation annotation, int offset);

    /**
     * Return the annotation group DOM element.
     * 
     * @return the element
     */
    Element getElement();

    /**
     * Return all annotation messages for this group.
     * 
     * @return the messages
     */
    List<String> getMessages();

    /**
     * Returns the number of annotations in the group.
     * 
     * @return the number of annotations
     */
    int getAnnotationCount();
}
