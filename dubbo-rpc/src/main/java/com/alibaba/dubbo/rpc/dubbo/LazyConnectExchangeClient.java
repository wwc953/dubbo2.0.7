/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.dubbo;

import java.net.InetSocketAddress;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.exchange.ExchangeClient;
import com.alibaba.dubbo.remoting.exchange.ExchangeHandler;
import com.alibaba.dubbo.remoting.exchange.Exchangers;
import com.alibaba.dubbo.remoting.exchange.ResponseFuture;
import com.alibaba.dubbo.remoting.exchange.support.header.HeaderExchangeClient;
import com.alibaba.dubbo.rpc.RpcConstants;

/**
 * dubbo protocol support class.
 * 
 * @author chao.liuc
 */
final class LazyConnectExchangeClient implements ExchangeClient {

    private final static Logger logger = LoggerFactory.getLogger(LazyConnectExchangeClient.class); 

    private final URL                     url;
    private final ExchangeHandler         requestHandler;
    private volatile HeaderExchangeClient client;
    private final Lock                    connectLock = new ReentrantLock();
    //lazy connect 如果没有初始化时的连接状态
    private final boolean                 initialState ;

    public LazyConnectExchangeClient(URL url, ExchangeHandler requestHandler) {
        //lazy connect ,need set send.reconnect = true, to avoid channel bad status. 
        this.url = url.addParameter(Constants.SEND_RECONNECT_KEY, Boolean.TRUE.toString());
        this.requestHandler = requestHandler;
        this.initialState = url.getBooleanParameter(RpcConstants.LAZY_CONNECT_INITIAL_STATE_KEY,RpcConstants.DEFAULT_LAZY_CONNECT_INITIAL_STATE);
    }

    private void initClient() throws RemotingException {
        if (client != null )
            return;
        if (logger.isInfoEnabled()) {
            logger.info("Lazy connect to " + url);
        }
        connectLock.lock();
        try {
            if (client != null)
                return;
            this.client = new HeaderExchangeClient(Exchangers.connect(url, requestHandler));
//            if (client == )
        } finally {
            connectLock.unlock();
        }
    }

    public ResponseFuture request(Object request) throws RemotingException {
        initClient();
        return client.request(request);
    }

    public URL getUrl() {
        return url;
    }

    public InetSocketAddress getRemoteAddress() {
        return client.getRemoteAddress();
    }

    public ResponseFuture request(Object request, int timeout) throws RemotingException {
        initClient();
        return client.request(request, timeout);
    }

    public ChannelHandler getChannelHandler() {
        checkClient();
        return client.getChannelHandler();
    }

    public boolean isConnected() {
        if (client == null) {
            return initialState;
        } else {
            return client.isConnected();
        }
    }

    public InetSocketAddress getLocalAddress() {
        checkClient();
        return client.getLocalAddress();
    }

    public ExchangeHandler getExchangeHandler() {
        checkClient();
        return client.getExchangeHandler();
    }

    public void send(Object message) throws RemotingException {
        initClient();
        client.send(message);
    }

    public void send(Object message, boolean sent) throws RemotingException {
        initClient();
        client.send(message, sent);
    }

    public boolean isClosed() {
        if (client != null)
            return client.isClosed();
        else
            return true;
    }

    public void close() {
        if (client != null)
            client.close();
    }

    public void close(int timeout) {
        if (client != null)
            client.close(timeout);
    }

    public void reset(URL url) {
        checkClient();
        client.reset(url);
    }

    public void reconnect() throws RemotingException {
        checkClient();
        client.reconnect();
    }

    public Object getAttribute(String key) {
        checkClient();
        return client.getAttribute(key);
    }

    public void setAttribute(String key, Object value) {
        checkClient();
        client.setAttribute(key, value);
    }

    public void removeAttribute(String key) {
        checkClient();
        client.removeAttribute(key);
    }

    public boolean hasAttribute(String key) {
        checkClient();
        return client.hasAttribute(key);
    }

    private void checkClient() {
        if (client == null) {
            throw new IllegalStateException(
                    "LazyConnectExchangeClient state error. the client has not be init .url:" + url);
        }
    }
}