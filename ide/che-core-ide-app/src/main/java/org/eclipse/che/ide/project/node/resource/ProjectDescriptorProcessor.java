package org.eclipse.che.ide.project.node.resource;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.ide.api.event.CloseCurrentProjectEvent;
import org.eclipse.che.ide.api.project.node.HasDataObject;
import org.eclipse.che.ide.project.event.DescriptorRemovedEvent;
import org.eclipse.che.ide.project.node.ModuleDescriptorNode;
import org.eclipse.che.ide.project.node.ProjectDescriptorNode;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.util.loging.Log;

import javax.annotation.Nonnull;

/**
 * @author Vlad Zhukovskiy
 */
public class ProjectDescriptorProcessor extends AbstractResourceProcessor<ProjectDescriptor> {

    @Inject
    public ProjectDescriptorProcessor(EventBus eventBus, ProjectServiceClient projectService, DtoUnmarshallerFactory unmarshallerFactory) {
        super(eventBus, projectService, unmarshallerFactory);
    }

    @Override
    public Promise<ProjectDescriptor> delete(@Nonnull HasDataObject<ProjectDescriptor> node) {
        if (node instanceof ProjectDescriptorNode) {
            //delete project
            Log.info(this.getClass(), "delete():32: " + "delete called on project node");
            eventBus.fireEvent(new CloseCurrentProjectEvent());
        } else if (node instanceof ModuleDescriptorNode) {
            //delete module
            Log.info(this.getClass(), "delete():37: " + "delete called on module node");

            eventBus.fireEvent(new DescriptorRemovedEvent(node.getData()));
        }

        return Promises.resolve(node.getData());
    }

    @Override
    public Promise<ProjectDescriptor> rename(@Nonnull HasDataObject<ProjectDescriptor> node, @Nonnull String newName) {
        return null;
    }
}
