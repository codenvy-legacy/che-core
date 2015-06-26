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
//package org.eclipse.che.ide.ui.toolbar;
//
//import com.google.gwt.user.client.ui.AcceptsOneWidget;
//import com.google.inject.Inject;
//
///**
// * Manages Toolbar items, change style.
// *
// * @author Oleksii Orel
// */
//public class ToolbarMainPresenter extends ToolbarPresenter {
//
//    private native void log(String msg) /*-{
//        console.log(msg);
//    }-*/;
//
//    private ToolbarView      view;
//    private ToolbarResources res;
//
//    @Inject
//    public ToolbarMainPresenter(ToolbarView view, ToolbarResources res) {
//        super(view);
//
//        log("ToolbarMainPresenter view: " + view.hashCode());
//
//        this.view = view;
//        this.res = res;
//    }
//
//    @Override
//    public void go(AcceptsOneWidget container) {
//
//        log("go ToolbarMainPresenter");
//
//        view.asWidget().setStyleName(res.toolbar().toolbarMenuPanel());
//        container.setWidget(view);
//    }
//
//}
