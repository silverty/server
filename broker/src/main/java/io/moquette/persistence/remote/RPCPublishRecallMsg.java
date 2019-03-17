package io.moquette.persistence.remote;

import io.moquette.interception.HazelcastRecallNotifyMsg;
import io.moquette.server.Server;

import java.io.Serializable;

public class RPCPublishRecallMsg implements Runnable, Serializable {
    final HazelcastRecallNotifyMsg message;

    public RPCPublishRecallMsg(HazelcastRecallNotifyMsg message) {
        this.message = message;
    }

    @Override
    public void run() {
        Server.getServer().getProcessor().internalNotifyMsg(message);
    }
}
