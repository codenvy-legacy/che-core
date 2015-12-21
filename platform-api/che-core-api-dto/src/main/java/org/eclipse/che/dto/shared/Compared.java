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
package org.eclipse.che.dto.shared;

import com.google.common.annotations.Beta;

import org.eclipse.che.dto.generator.DtoImplClientTemplate;
import org.eclipse.che.dto.generator.DtoImplServerTemplate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Include DTO's specific value into equals and hashcode mechanism.
 * By default all DTO's values are involved in equals and hashcode methods.
 * So sometimes there is a need to include in equals only one or two fields
 * for example for files it will be enough to compare by storable path and
 * not by modified date.
 * <p/>
 * If no one method is annotated in DTO, then all these methods will participate
 * in constructing equals and hashcode method.
 *
 * @author Vlad Zhukovskyi
 * @see DtoImplClientTemplate#emitEqualsAndHashCode(java.util.List, java.lang.StringBuilder)
 * @see DtoImplServerTemplate#emitEqualsAndHashCode(java.util.List, java.lang.StringBuilder)
 */
@Beta
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Compared {
}
