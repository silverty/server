package io.moquette.persistence.remote;

import io.moquette.server.Server;

import java.io.Serializable;

public class RPCPublishChatroomMsg implements Runnable, Serializable {
    final String target;
    final int line;
    final long messageId;

    public RPCPublishChatroomMsg(String target, int line, long messageId) {
        this.target = target;
        this.line = line;
        this.messageId = messageId;
    }

    @Override
    public void run() {
        Server.getServer().getProcessor().internalNotifyChatroomMsg(target, line, messageId);
    }
}
