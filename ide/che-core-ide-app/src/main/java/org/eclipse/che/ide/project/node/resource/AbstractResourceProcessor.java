package org.eclipse.che.ide.project.node.resource;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.ide.api.project.node.resource.DeleteProcessor;
import org.eclipse.che.ide.api.project.node.resource.RenameProcessor;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;
import org.eclipse.che.ide.util.loging.Log;

import javax.annotation.Nonnull;

/**
 * @author Vlad Zhukovskiy
 */
public abstract class AbstractResourceProcessor<DataObject> implements DeleteProcessor<DataObject>, RenameProcessor<DataObject> {
    protected EventBus             eventBus;
    protected ProjectServiceClient projectService;
    protected DtoUnmarshallerFactory unmarshallerFactory;

    public AbstractResourceProcessor(EventBus eventBus, ProjectServiceClient projectService, DtoUnmarshallerFactory unmarshallerFactory) {
        this.eventBus = eventBus;
        this.projectService = projectService;
        this.unmarshallerFactory = unmarshallerFactory;
    }

    @Nonnull
    protected  <T> AsyncRequestCallback<T> _createCallback(@Nonnull final AsyncCallback<T> callback, @Nonnull Unmarshallable<T> u) {
        return new AsyncRequestCallback<T>(u) {
            @Override
            protected void onSuccess(T result) {
                callback.onSuccess(result);
            }

            @Override
            protected void onFailure(Throwable e) {
                callback.onFailure(e);
            }
        };
    }

    protected Function<Void, Promise<DataObject>> returnSelf(final DataObject data) {
        return new Function<Void, Promise<DataObject>>() {
            @Override
            public Promise<DataObject> apply(Void arg) throws FunctionException {
                Log.info(this.getClass(), "apply():51: " + data);
                return Promises.resolve(data);
            }
        };
    }
}
