package org.eclipse.che.api.core.model.project.type;

import java.util.List;

/**
 * @author gazarenkov
 */
public interface ProjectType {

    boolean isPersisted();

    String getId();

    String getDisplayName();

    List<Attribute> getAttributes();

    List<ProjectType> getParents();

    String getDefaultRecipe();

    boolean isTypeOf(String typeId);

    Attribute getAttribute(String name);

    boolean canBeMixin();

    boolean canBePrimary();
}
