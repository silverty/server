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

package io.moquette.server;

import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import io.moquette.interception.HazelcastIMNotifyMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HazelcastIMListener implements MessageListener<HazelcastIMNotifyMsg> {

    private static final Logger LOG = LoggerFactory.getLogger(HazelcastIMListener.class);

    private final Server server;

    public HazelcastIMListener(Server server) {
        this.server = server;
    }

    @Override
    public void onMessage(Message<HazelcastIMNotifyMsg> msg) {
        try {
            if (!msg.getPublishingMember().equals(server.getHazelcastInstance().getCluster().getLocalMember())) {
                HazelcastIMNotifyMsg hzMsg = msg.getMessageObject();

               server.internalNotifyMsg(hzMsg);
            }
        } catch (Exception ex) {
            LOG.error("error polling hazelcast msg queue", ex);
        }
    }
}
