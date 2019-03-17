package io.moquette.persistence.remote;

import io.moquette.server.Server;

import java.io.Serializable;

public class RPCPublishChatroomNotify implements Runnable, Serializable {
    final String user;
    final String clientId;
    final long messageHead;

    public RPCPublishChatroomNotify(String user, String clientId, long messageHead) {
        this.user = user;
        this.clientId = clientId;
        this.messageHead = messageHead;
    }

    @Override
    public void run() {
        Server.getServer().getProcessor().internalNotifyChatroomNotify(user, clientId, messageHead);
    }
}
