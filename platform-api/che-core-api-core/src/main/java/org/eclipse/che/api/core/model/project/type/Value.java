package org.eclipse.che.api.core.model.project.type;

import java.util.List;

/**
 * @author gazarenkov
 */
public interface Value {
    String getString();

    void setString(String str);

    List<String> getList();

    void setList(List<String> list);
}
