package org.eclipse.che.api.core.model.project.type;

/**
 * @author gazarenkov
 */
public interface Attribute {

    String getId();

    String getProjectType();

    String getDescription();

    boolean isRequired();

    boolean isVariable();

    String getName();

    Value getValue() /*throws ValueStorageException*/;
}
