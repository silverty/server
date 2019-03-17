package io.moquette.persistence.remote;

import io.moquette.interception.HazelcastNotifyMsg;
import io.moquette.server.Server;

import java.io.Serializable;

public class RPCPublishNotification implements Runnable, Serializable {
    final HazelcastNotifyMsg message;

    public RPCPublishNotification(HazelcastNotifyMsg message) {
        this.message = message;
    }

    @Override
    public void run() {
        Server.getServer().getProcessor().internalNotifyMsg(message);
    }
}
