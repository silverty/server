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

package io.moquette.interception;

import cn.wildfirechat.proto.WFCMessage;

import java.io.Serializable;
import java.util.Collection;

public class HazelcastRecallNotifyMsg implements Serializable {


    private final long messageUid;
    private final String operatorId;
    private final Collection<String> receivers;
    private final String clientId;

    public HazelcastRecallNotifyMsg(long messageUid, String operatorId, Collection<String> receivers, String clientId) {
        this.messageUid = messageUid;
        this.operatorId = operatorId;
        this.receivers = receivers;
        this.clientId = clientId;
    }

    public long getMessageUid() {
        return messageUid;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public Collection<String> getReceivers() {
        return receivers;
    }

    public String getClientId() {
        return clientId;
    }
}
