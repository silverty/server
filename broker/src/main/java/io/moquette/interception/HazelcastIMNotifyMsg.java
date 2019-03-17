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
import java.util.List;

public class HazelcastIMNotifyMsg implements Serializable {

    private static final long serialVersionUID = -1431584655134928273L;
    private final String sender;
    private final int conversationType;
    private final String target;
    private final int line;
    private final Collection<String> receivers;
    private final int pullType;
    private final long messageHead;
    private final String clientId;
    private final String pushContent;
    private final int messageContentType;
    private final long serverTime;
    private final int mentionType;
    private final List<String> mentionTargets;
    private final int persistFlag;

    public HazelcastIMNotifyMsg(String sender, int conversationType, String target, int line, long messageHead, Collection<String> receivers, String pushContent, String exceptClientId, int pullType, int messageContentType, long serverTime, int mentionType, List<String> mentionTargets, int persistFlag) {
        this.sender = sender;
        this.conversationType = conversationType;
        this.target = target;
        this.line = line;
        this.messageHead = messageHead;
        this.receivers = receivers;
        this.pushContent = pushContent;
        this.pullType = pullType;
        this.messageContentType = messageContentType;
        this.serverTime = serverTime;
        this.clientId = exceptClientId;
        this.mentionType = mentionType;
        this.mentionTargets = mentionTargets;
        this.persistFlag = persistFlag;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public String getSender() {
        return sender;
    }

    public int getConversationType() {
        return conversationType;
    }

    public String getTarget() {
        return target;
    }

    public int getLine() {
        return line;
    }

    public Collection<String> getReceivers() {
        return receivers;
    }

    public int getPullType() {
        return pullType;
    }

    public long getMessageHead() {
        return messageHead;
    }

    public String getClientId() {
        return clientId;
    }

    public String getPushContent() {
        return pushContent;
    }

    public int getMessageContentType() {
        return messageContentType;
    }

    public long getServerTime() {
        return serverTime;
    }

    public int getMentionType() {
        return mentionType;
    }

    public List<String> getMentionTargets() {
        return mentionTargets;
    }

    public int getPersistFlag() {
        return persistFlag;
    }
}
