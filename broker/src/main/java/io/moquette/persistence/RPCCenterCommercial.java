package io.moquette.persistence;

import com.hazelcast.core.HazelcastInstance;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import io.moquette.server.Server;
import io.moquette.server.config.IConfig;
import win.liyufan.im.ErrorCode;
import win.liyufan.im.RateLimiter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

import static io.moquette.BrokerConstants.HZ_Cluster_Master_Node;

public class RPCCenterCommercial extends RPCCenter {
    private final RateLimiter mLimitCounter = new RateLimiter(10, 1000);
    private boolean licensed;

    protected RPCCenterCommercial() {
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
        if (licensed) {
            return true;
        }

        CountDownLatch latch = new CountDownLatch(1);
        final List<ErrorCode> checkResult = new ArrayList<>();
        RPCCenter.getInstance().sendRequest(null, null, RPCCenter.CHECK_USER_MASTER_LICENSE, null, null, TargetEntry.Type.TARGET_TYPE_MASTER_NODE, new RPCCenter.Callback() {
            @Override
            public void onSuccess(byte[] result) {
                checkResult.add(ErrorCode.ERROR_CODE_SUCCESS);
                latch.countDown();
            }

            @Override
            public void onError(ErrorCode errorCode) {
                checkResult.add(errorCode);
                latch.countDown();
            }

            @Override
            public void onTimeout() {
                checkResult.add(ErrorCode.ERROR_CODE_TIMEOUT);
                latch.countDown();
            }

            @Override
            public Executor getResponseExecutor() {
                return command -> {
                    server.getImBusinessScheduler().execute(command);
                };
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (checkResult.get(0) != ErrorCode.ERROR_CODE_SUCCESS) {
            return false;
        }
        return true;
    }

    @Override
    public void decodeLicense(HazelcastInstance hazelcastInstance, String serverIp, String shortPort) {
        File licenseFile = licenseFile();
        if(licenseFile.exists()) {
            try {
                License license = License.decodeLicense(licenseFile);
                if (license != null) {
                    if (license.getIp().equals(serverIp) && license.getPort().equals(shortPort) && System.currentTimeMillis() < license.getExpiredTime()) {
                        RPCCenter.setLimitClientCount(license.getUserCount());
                        licensed = true;
                        hazelcastInstance.getCluster().getLocalMember().setStringAttribute(HZ_Cluster_Master_Node, "true");
                    }
                }
            } catch (Exception e) {

            }
        }
    }

    private static File licenseFile() {
        String configPath = System.getProperty("wildfirechat.path", null);
        return new File(configPath, IConfig.LICENSE_PATH);
    }

    @Override
    public boolean handleCommercialRequest(String request, String clientId, String from, int requestId) {
        if (request.equals(CHECK_USER_COUNT_LIMIT)) {
            RPCCenter.clientMaps.put(clientId, System.currentTimeMillis());
            if (RPCCenter.clientMaps.size() > RPCCenter.limitClientCount) {
                long now = System.currentTimeMillis();
                long d30 = (long) (15.0 * 24 * 60 * 60 * 1000);
                for (String key : RPCCenter.clientMaps.keySet()) {
                    if (now - RPCCenter.clientMaps.get(key) > d30) {
                        RPCCenter.clientMaps.remove(key);
                    }
                }
            }

            ErrorCode errorCode = ErrorCode.ERROR_CODE_SUCCESS;
            if (RPCCenter.clientMaps.size() > RPCCenter.limitClientCount) {
                errorCode = ErrorCode.ERROR_CODE_CLIENT_COUNT_OUT_OF_LIMIT;
            }
            String counts = RPCCenter.clientMaps.size() + ":" + RPCCenter.limitClientCount;

            RPCCenter.getInstance().sendResponse(errorCode.ordinal(), counts.getBytes(), from, requestId);
            return true;
        }
        if(request.equals(UPDATE_USER_COUNT)) {
            RPCCenter.clientMaps.put(clientId, System.currentTimeMillis());
            return true;
        }
        if(request.equals(CHECK_USER_MASTER_LICENSE)) {
            //检查是否是授权节点
            if (licensed) {
                RPCCenter.getInstance().sendResponse(ErrorCode.ERROR_CODE_SUCCESS.ordinal(), null, from, requestId);
            } else {
                RPCCenter.getInstance().sendResponse(ErrorCode.ERROR_CODE_NOT_RIGHT.ordinal(), null, from, requestId);
            }
            return true;
        }
        return false;
    }
}
