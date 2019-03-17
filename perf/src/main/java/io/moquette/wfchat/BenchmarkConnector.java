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

import org.eclipse.paho.client.mqttv3.*;
import java.util.*;



class BenchmarkConnector extends BenchmarkBase {

    @Override
    void onConnected(String user) {

    }

    @Override
    void onConnectFailed(String user, String errorInfo) {

    }

    @Override
    void onDisconnected(String user) {

    }

    @Override
    void onConnectionLost(String user) {

    }

    @Override
    void onMessageArrived(String topic, MqttMessage message) {

    }

    @Override
    void onDeliveryComplete(IMqttDeliveryToken token) {

    }

    BenchmarkConnector(String dialog_id, String userId) {
        super(dialog_id, userId);
    }

    public static void main(String[] args) throws MqttException{
        int count = 10000;

        for (int i = 0; i < count; i++) {
            BenchmarkConnector publisher = new BenchmarkConnector("" + i, "user" + i);
            publisher.start();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("Press Q to exit!");
            String s = sc.next();
            if(s.equals("Q") || s.equals("q") || s.equalsIgnoreCase("quit")) {
                synchronized (BenchmarkBase.class) {
                    for (BenchmarkBase publisher : publishers) {
                        publisher.stop();
                    }
                }
                break;
            }
        }

    }
}
