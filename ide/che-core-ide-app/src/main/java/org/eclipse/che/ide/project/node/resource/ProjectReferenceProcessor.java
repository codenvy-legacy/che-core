package org.eclipse.che.ide.project.node.resource;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ProjectReference;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.api.project.node.HasDataObject;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;

import javax.annotation.Nonnull;

/**
 * @author Vlad Zhukovskiy
 */
public class ProjectReferenceProcessor extends AbstractResourceProcessor<ProjectReference> {
    @Inject
    public ProjectReferenceProcessor(EventBus eventBus,
                                     ProjectServiceClient projectService,
                                     DtoUnmarshallerFactory unmarshallerFactory) {
        super(eventBus, projectService, unmarshallerFactory);
    }

    @Override
    public Promise<ProjectReference> delete(@Nonnull HasDataObject<ProjectReference> node) {
        return null;
    }

    @Override
    public Promise<ProjectReference> rename(@Nonnull HasDataObject<ProjectReference> node, @Nonnull String newName) {
        return null;
    }
}
