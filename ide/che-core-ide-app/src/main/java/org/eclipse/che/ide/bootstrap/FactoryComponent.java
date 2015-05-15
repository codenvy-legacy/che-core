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

package org.eclipse.che.ide.bootstrap;

//import org.eclipse.che.api.factory.dto.Factory;
//import org.eclipse.che.api.factory.gwt.client.FactoryServiceClient;
//import org.eclipse.che.ide.api.app.AppContext;
//import org.eclipse.che.ide.core.Component;
//import org.eclipse.che.ide.rest.AsyncRequestCallback;
//import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
//import org.eclipse.che.ide.util.Config;
//import org.eclipse.che.ide.util.loging.Log;
//import com.google.gwt.core.client.Callback;
//import com.google.inject.Inject;
//import com.google.inject.Singleton;
//
///**
// * @author Evgen Vidolob
// */
//@Singleton
//public class FactoryComponent implements Component {
//
//    private final FactoryServiceClient   factoryService;
//    private final DtoUnmarshallerFactory dtoUnmarshallerFactory;
//    private final AppContext             appContext;
//
//    @Inject
//    public FactoryComponent(FactoryServiceClient factoryService, DtoUnmarshallerFactory dtoUnmarshallerFactory,
//                            AppContext appContext) {
//        this.factoryService = factoryService;
//        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
//        this.appContext = appContext;
//    }
//
//    @Override
//    public void start(final Callback<Component, Exception> callback) {
//        String factoryParams = Config.getStartupParam("id");
//        if (factoryParams != null) {
//            factoryService.getFactory(factoryParams,
//                                      new AsyncRequestCallback<Factory>(dtoUnmarshallerFactory.newUnmarshaller(Factory.class)) {
//                                          @Override
//                                          protected void onSuccess(Factory factory) {
//                                              appContext.setFactory(factory);
//                                              callback.onSuccess(FactoryComponent.this);
//                                          }
//
//                                          @Override
//                                          protected void onFailure(Throwable error) {
//                                              Log.error(FactoryComponent.class, "Unable to load Factory", error);
//                                              callback.onSuccess(FactoryComponent.this);
//                                          }
//                                      }
//                                     );
//        } else {
//            callback.onSuccess(this);
//        }
//    }
//}
