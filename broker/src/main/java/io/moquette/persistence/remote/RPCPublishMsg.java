package io.moquette.persistence.remote;

import io.moquette.interception.HazelcastIMNotifyMsg;
import io.moquette.server.Server;

import java.io.Serializable;

public class RPCPublishMsg implements Runnable, Serializable {
    final HazelcastIMNotifyMsg message;

    public RPCPublishMsg(HazelcastIMNotifyMsg message) {
        this.message = message;
    }

    @Override
    public void run() {
        Server.getServer().getProcessor().internalNotifyMsg(message);
    }
}
