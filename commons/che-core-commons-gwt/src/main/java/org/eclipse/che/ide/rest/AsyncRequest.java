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
package org.eclipse.che.ide.rest;

import org.eclipse.che.ide.commons.exception.JobNotFoundException;
import org.eclipse.che.ide.commons.exception.ServerException;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestBuilder.Method;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window.Location;

/** Wrapper under RequestBuilder to simplify the stuffs. */
public class AsyncRequest {
    protected RequestBuilder     builder;
    protected AsyncRequestLoader loader;
    protected boolean            async;
    protected String             loaderMessage;
    protected int delay = 5000;
    protected RequestStatusHandler    handler;
    protected String                  requestStatusUrl;
    private   AsyncRequestCallback<?> callback;
    private AsyncRequestCallback<String> initCallback = new AsyncRequestCallback<String>(new LocationUnmarshaller()) {
        {
            setSuccessCodes(new int[]{Response.SC_ACCEPTED});
        }

        @Override
        protected void onSuccess(String result) {
            requestStatusUrl = result;
            if (handler != null) {
                handler.requestInProgress(requestStatusUrl);
            }

            requestTimer.schedule(delay);
        }

        @Override
        protected void onFailure(Throwable exception) {
            if (handler != null) {
                handler.requestError(requestStatusUrl, exception);
            }
            callback.onError(null, exception);
        }
    };
    private Timer requestTimer = new Timer() {
        @Override
        public void run() {
            RequestBuilder request = new RequestBuilder(RequestBuilder.GET, requestStatusUrl);
            request.setCallback(new RequestCallback() {

                public void onResponseReceived(Request request, Response response) {
                    if (Response.SC_NOT_FOUND == response.getStatusCode()) {
                        callback.onError(request, new JobNotFoundException(response));
                        if (handler != null) {
                            handler.requestError(requestStatusUrl, new JobNotFoundException(response));
                        }
                    } else if (response.getStatusCode() != Response.SC_ACCEPTED) {
                        callback.onResponseReceived(request, response);
                        if (handler != null) {
                            // check is response successful, for correct handling failed responses
                            if (callback.isSuccessful(response))
                                handler.requestFinished(requestStatusUrl);
                            else
                                handler.requestError(requestStatusUrl, new ServerException(response));
                        }
                    } else {
                        if (handler != null)
                            handler.requestInProgress(requestStatusUrl);

                        requestTimer.schedule(delay);
                    }
                }

                public void onError(Request request, Throwable exception) {
                    if (handler != null)
                        handler.requestError(requestStatusUrl, exception);

                    callback.onError(request, exception);
                }
            });
            try {
                request.send();
            } catch (RequestException e) {
                e.printStackTrace();
                if (handler != null) {
                    handler.requestError(requestStatusUrl, e);
                }
                callback.onFailure(e);
            }
        }
    };

    /**
     * Create new {@link AsyncRequest} instance.
     *
     * @param method
     *         request method
     * @param url
     *         request URL
     * @param async
     *         if <b>true</b> - request will be sent in asynchronous mode
     */
    protected AsyncRequest(Method method, String url, boolean async) {
        if (async) {
            if (url.contains("?")) {
                url += "&async=true";
            } else {
                url += "?async=true";
            }
        }
        this.builder = new RequestBuilder(method, getCheckedURL(url));
        this.loader = new EmptyLoader();
        this.async = async;
    }

    /** @deprecated use {@link AsyncRequestFactory} instead. */
    @Deprecated
    protected AsyncRequest(RequestBuilder builder) {
        this.builder = builder;
        this.loader = new EmptyLoader();
        async = false;
    }

    /** @deprecated use {@link AsyncRequestFactory} instead. */
    @Deprecated
    protected AsyncRequest(RequestBuilder builder, boolean async) {
        this(builder);
        this.async = async;
    }

    private static native String getProxyServiceContext() /*-{
        return $wnd.proxyServiceContext;
    }-*/;

    private static String getCheckedURL(String url) {
        String proxyServiceContext = getProxyServiceContext();
        if (proxyServiceContext == null || "".equals(proxyServiceContext)) {
            return url;
        }

        if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            return url;
        }

        String currentHost = Location.getProtocol() + "//" + Location.getHost();
        if (url.startsWith(currentHost)) {
            return url;
        }

        return proxyServiceContext + "?url=" + URL.encodeQueryString(url);
    }

    /** @deprecated use {@link AsyncRequestFactory} instead. */
    @Deprecated
    public static final AsyncRequest build(Method method, String url) {
        return build(method, url, false);
    }

    /**
     * Build AsyncRequest with run REST Service in async mode.
     *
     * @param method
     *         HTTP method
     * @param url
     *         of service
     * @param async
     *         is run async
     * @return instance of {@link AsyncRequest}
     * @deprecated use {@link AsyncRequestFactory} instead.
     */
    @Deprecated
    public static final AsyncRequest build(Method method, String url, boolean async) {
        if (async) {
            if (url.contains("?")) {
                url += "&async=true";
            } else {
                url += "?async=true";
            }
        }
        String checkedURL = getCheckedURL(url);
        return new AsyncRequest(new RequestBuilder(method, checkedURL), async);
    }

    public final AsyncRequest header(String header, String value) {
        builder.setHeader(header, value);
        return this;
    }

    public final AsyncRequest user(String user) {
        builder.setUser(user);
        return this;
    }

    public final AsyncRequest password(String password) {
        builder.setPassword(password);
        return this;
    }

    public final AsyncRequest data(String requestData) {
        builder.setRequestData(requestData);
        return this;
    }

    public final AsyncRequest loader(AsyncRequestLoader loader) {
        this.loader = loader;
        return this;
    }

    public final AsyncRequest loader(AsyncRequestLoader loader, String loaderMessage) {
        this.loader = loader;
        this.loaderMessage = loaderMessage;
        return this;
    }

    /**
     * Set delay between requests to async REST Service<br>
     * (Default: 5000 ms).
     *
     * @param delay
     *         the amount of time to wait between request resending (in milliseconds)
     * @return this {@code AsyncRequest}
     */
    public final AsyncRequest delay(int delay) {
        this.delay = delay;
        return this;
    }

    /**
     * Set handler of async REST Service status.
     *
     * @param handler
     * @return
     */
    public final AsyncRequest requestStatusHandler(RequestStatusHandler handler) {
        this.handler = handler;
        return this;
    }

    private void sendRequest(AsyncRequestCallback<?> callback) throws RequestException {
        callback.setLoader(loader, loaderMessage);
        callback.setRequest(this);
        builder.setCallback(callback);

        if (loaderMessage == null) {
            loader.show();
        } else {
            loader.show(loaderMessage);
        }

        builder.send();
    }

    /**
     * Sends an HTTP request based on the current {@link AsyncRequest} configuration.
     *
     * @param callback
     *         the response handler to be notified when the request fails or completes
     */
    public final void send(AsyncRequestCallback<?> callback) {
        this.callback = callback;
        try {
            if (async) {
                sendRequest(initCallback);
            } else {
                sendRequest(callback);
            }
        } catch (RequestException e) {
            callback.onFailure(e);
        }
    }

    /**
     * Returns the callback of current {@link AsyncRequest}, or null if no callback was set.
     *
     * @return the callback that to be notified when the request fails or completes
     */
    public AsyncRequestCallback<?> getCallback() {
        return callback;
    }

    private class EmptyLoader implements AsyncRequestLoader {
        @Override
        public void hide() {
        }

        @Override
        public void hide(String message) {

        }

        @Override
        public void show() {
        }

        @Override
        public void show(String message) {

        }
    }

    public RequestBuilder getRequestBuilder() {
        return builder;
    }
}
