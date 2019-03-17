/*
 * Copyright (c) 2012-2017 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The Apache License v2.0 is available at
 * http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package io.moquette.wfchat;

import com.google.gson.Gson;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.pojos.InputRoute;
import io.moquette.spi.impl.security.AES;
import io.moquette.spi.impl.security.TokenAuthenticator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.CountDownLatch;


abstract class BenchmarkBase implements MqttCallback {
    static {
        Locale.setDefault(Locale.ROOT);
    }
    private static final Logger LOG = LoggerFactory.getLogger(BenchmarkBase.class);

    final String user_id;
    IMqttAsyncClient client;
    CountDownLatch mQuitLatch;

    ConnectionStatus connectionStatus;
    String serverIp;
    long port;
    final String dialog_id;
    static int connectedCount = 0;
    static int connectFailedCount = 0;
    static int connectionLostCount = 0;

    private CountDownLatch m_connectedLatch;

    static Set<BenchmarkBase> publishers = new HashSet<>();
    enum ConnectionStatus {
        Connecting, Connected, ConnectFailed, ConnectionLost
    }
    private Thread thread;

    abstract void onConnected(String user);
    abstract void onConnectFailed(String user, String errorInfo);
    abstract void onDisconnected(String user);
    abstract void onConnectionLost(String user);
    abstract void onMessageArrived(String topic, MqttMessage message);
    abstract void onDeliveryComplete(IMqttDeliveryToken token);

    private synchronized void updateStatus(String user, ConnectionStatus type, String server) {
        this.connectionStatus = type;
        StringBuilder sb = new StringBuilder(user);
        switch (type) {
            case Connected:
                connectedCount++;
                sb.append("is connected to " + server + ":" + port);
                sb.append(":" + port);
                break;
            case ConnectFailed:
                connectFailedCount++;
                sb.append("connect failure to " + server + ":" + port);
                break;
            case ConnectionLost:
                connectedCount--;
                connectionLostCount++;
                sb.append("connection lost from " + server + ":" + port);
                break;
        }
        System.out.println(sb.toString());
        System.out.println("Stat: Connected(" + connectedCount + "), ConnectFailed(" + connectFailedCount + "), Lost(" + connectionLostCount + ")");
    }

    BenchmarkBase(String dialog_id, String userId) {
        this.dialog_id = dialog_id;
        this.user_id = userId;
    }

    public void start() {
        synchronized (BenchmarkBase.class) {
            publishers.add(this);
        }
        thread = new Thread(()-> {
            try {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                connect();
            } catch (MqttException e) {
                e.printStackTrace();
            }

            try {
                waitFinish();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }

    public void stop() {
        mQuitLatch.countDown();
    }

    private boolean route(String userId, String token) {
        HttpPost httpPost;
        String result;
        try{
            HttpClient httpClient = new DefaultHttpClient();
            httpPost = new HttpPost("http://localhost:1983/api/route");
            InputRoute inputRoute = new InputRoute();
            inputRoute.setUserId(userId);
            inputRoute.setClientId(getClientId());
            inputRoute.setToken(token);

            StringEntity entity = new StringEntity(new Gson().toJson(inputRoute), Charset.forName("UTF-8"));
            entity.setContentEncoding("UTF-8");
            entity.setContentType("application/json");
            httpPost.setEntity(entity);


            HttpResponse response = httpClient.execute(httpPost);
            if(response != null){
                HttpEntity resEntity = response.getEntity();
                if(resEntity != null){
                    result = EntityUtils.toString(resEntity);
                    RestResult restResult = new Gson().fromJson(result, RestResult.class);
                    if (restResult.getCode() == 0) {
                        Map<String, Object> maps = (Map<String, Object>)restResult.getResult();
                        List<String> serverIPs = (List<String>)maps.get("serverIPs");
                        serverIp = serverIPs.get(0);
                        double doublePort = (Double)maps.get("longPort");
                        port = (long)doublePort;
                        return true;
                    }
                }
            }
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return false;
    }

    private String getClientId() {
        return "Client" + dialog_id;
    }
    private void connect() throws MqttException {
        MqttConnectOptions connectOptions = new MqttConnectOptions();
        TokenAuthenticator authenticator = new TokenAuthenticator();
        String strToken = authenticator.generateToken(user_id);

        if (!route(user_id, strToken)) {
            return;
        }
        String tmpDir = System.getProperty("java.io.tmpdir");
        MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir);
        MqttAsyncClient pub = new MqttAsyncClient("tcp://" + serverIp + ":" + port, getClientId(), dataStore);

        this.client = pub;

        this.client.setCallback(this);

        connectOptions.setUserName(user_id);

        byte[] encryptedToken = AES.AESEncrypt(strToken, null);

        connectOptions.setPassword(encryptedToken);

        connectOptions.setCleanSession(true);
        this.client.connect(connectOptions, null, new IMqttActionListener() {
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                LOG.error("CON: connect fail", exception);
                updateStatus(user_id, ConnectionStatus.ConnectFailed, serverIp);
                onConnectFailed(user_id, exception.toString());
            }

            public void onSuccess(IMqttToken asyncActionToken) {
                LOG.info("PUB: Successfully connected to server");
                updateStatus(user_id, ConnectionStatus.Connected, serverIp);
                m_connectedLatch.countDown();
            }
        });

        this.mQuitLatch = new CountDownLatch(1);
        this.m_connectedLatch = new CountDownLatch(1);

        try {
            m_connectedLatch.await();
            onConnected(user_id);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void waitFinish() throws InterruptedException {
        mQuitLatch.await();
        try {
            this.client.disconnect();
        } catch (MqttException mex) {
            LOG.error("Disconnect error", mex);
        }
        onDisconnected(user_id);

        synchronized (BenchmarkBase.class) {
            publishers.remove(this);
            if (publishers.isEmpty()) {
                System.exit(0);
            }
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        updateStatus(user_id, ConnectionStatus.ConnectionLost, serverIp);
        onConnectionLost(user_id);
        if (thread != null) {
            thread.stop();
        }
        //restart
        start();
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        onMessageArrived(topic, message);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        onDeliveryComplete(token);
    }
}
