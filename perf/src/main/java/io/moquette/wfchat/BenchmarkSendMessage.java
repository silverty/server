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

import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.proto.WFCMessage;
import io.moquette.spi.impl.security.AES;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import win.liyufan.im.IMTopic;

import java.util.*;
import java.util.concurrent.CountDownLatch;

class BenchmarkSendMessage extends BenchmarkBase {
    private static final Logger LOG = LoggerFactory.getLogger(BenchmarkSendMessage.class);

    private CountDownLatch m_sendMsgLatch;


    BenchmarkSendMessage(String dialog_id, String userId) {
        super(dialog_id, userId);
    }

    public static void main(String[] args) {
        int count = 1;
        for (int i = 0; i < count; i++) {
            BenchmarkSendMessage publisher = new BenchmarkSendMessage("" + i, "user" + i);
            publisher.start();
            try {
                Thread.sleep(50);
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

    @Override
    void onConnected(String user) {
        int messageId = 0;
        while (true) {
            this.m_sendMsgLatch = new CountDownLatch(1);
            messageId++;

            MqttMessage publishMsg = new MqttMessage();
            publishMsg.setQos(1);
            publishMsg.setRetained(true);
            publishMsg.setId(messageId);
            WFCMessage.Message message = WFCMessage.Message.newBuilder()
                .setConversation(WFCMessage.Conversation.newBuilder().setType(ProtoConstants.ConversationType.ConversationType_Private).setTarget("0").setLine(0).build())
                .setFromUser(user_id)
                .setContent(WFCMessage.MessageContent.newBuilder().setType(1).setSearchableContent("hello " + messageId + " from " + user_id).build())
                .build();

            byte[] encryptedData = AES.AESEncrypt(message.toByteArray(), "");
            publishMsg.setPayload(encryptedData);

            if (connectionStatus != ConnectionStatus.Connected) {
                LOG.info("not connected of {}, wait 1000 msec", user_id);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }

            if (mQuitLatch.getCount() == 0) {
                break;
            }
            try {
                long currentTime = System.currentTimeMillis();
                client.publish(IMTopic.SendMessageTopic, publishMsg, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        LOG.info("Send Message: Successfully");
                        m_sendMsgLatch.countDown();
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        LOG.info("Send Message: failure", exception);
                        m_sendMsgLatch.countDown();
                    }
                });
                m_sendMsgLatch.await();
                long timeUsed = System.currentTimeMillis() - currentTime;
                LOG.info("send message use {} msec", timeUsed);

                if (timeUsed < 1000) {
                    Thread.sleep(1000 - timeUsed);
                }
            } catch (MqttException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
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
}
