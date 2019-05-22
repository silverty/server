package io.moquette.persistence;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.Member;
import io.moquette.interception.HazelcastIMNotifyMsg;
import io.moquette.interception.HazelcastNotifyMsg;
import io.moquette.interception.HazelcastRecallNotifyMsg;
import io.moquette.persistence.remote.*;
import io.moquette.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import common.cn.wildfirechat.ErrorCode;
import win.liyufan.im.RateLimiter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class RPCCenterCommunity extends RPCCenter {
    private final RateLimiter mLimitCounter = new RateLimiter(10, 10);

    protected RPCCenterCommunity() {
    }

    @Override
    public boolean isRateLimited(int type) {
        if (!mLimitCounter.isGranted(type + "")) {
            return true;
        }
        return false;
    }

    @Override
    public boolean checkAllowedRun(Server server) {
        return true;
    }

    @Override
    public void decodeLicense(HazelcastInstance hazelcastInstance, String serverIp, String shortPort) {

    }

    @Override
    public boolean handleCommercialRequest(String request, String clientId, String from, int requestId) {
        return false;
    }
}
