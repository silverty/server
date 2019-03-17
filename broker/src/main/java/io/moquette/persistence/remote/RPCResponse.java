package io.moquette.persistence.remote;

import io.moquette.persistence.RPCCenter;

import java.io.Serializable;

public class RPCResponse implements Runnable, Serializable {
    final int requestId;
    final byte[] message;
    final int errorCode;

    public RPCResponse(byte[] message, int requestId, int errorCode) {
        this.requestId = requestId;
        this.message = message;
        this.errorCode = errorCode;
    }

    @Override
    public void run() {
        RPCCenter.getInstance().onReceiveResponse(message, requestId, errorCode);
    }
}
