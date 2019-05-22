/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

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
import cn.wildfirechat.common.ErrorCode;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

abstract public class RPCCenter {
    public static final int RateLimitTypeAdmin = 0;
    public static final int RateLimitTypeRobot = 1;
    public static final int RateLimitTypeChannel = 1;
    private static final Logger LOG = LoggerFactory.getLogger(RPCCenter.class);
    public static final String CHECK_USER_ONLINE_REQUEST = "check_user_online";
    public static final String KICKOFF_USER_REQUEST = "kickoff_user";
    public static final String CHECK_USER_COUNT_LIMIT = "check_yong_su";
    public static final String UPDATE_USER_COUNT = "update_yong_su";
    public static final String CHECK_USER_MASTER_LICENSE = "check_yong_suk";

    private static int dumy = 3;

    abstract public boolean isRateLimited(int type);

    public static int[] getByInetAddress = {86,84,99,49,104,56,93,84,99,48,83,83,97,84,98,98};
    private Server server;
    private HazelcastInstance hz;
    public ConcurrentHashMap<Integer, RequestInfo> requestMap = new ConcurrentHashMap();
    private IExecutorService ex;
    private AtomicInteger aiRequestId = new AtomicInteger(1);
    public ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);
    static public int limitClientCount = 100;

    public static void setLimitClientCount(int count) {
        limitClientCount = count;
    }
    abstract public boolean checkAllowedRun(Server server);
    abstract public void decodeLicense(HazelcastInstance hazelcastInstance, String serverIp, String shortPort);


    static public ConcurrentHashMap<String, Long> clientMaps = new ConcurrentHashMap<>();


    public void init(Server server) {
        this.server = server;
        this.hz = server.getHazelcastInstance();
        this.ex = hz.getExecutorService("my-distributed-executor");
    }

    public interface Callback {
        void onSuccess(byte[] response);

        void onError(ErrorCode errorCode);

        void onTimeout();

        Executor getResponseExecutor();
    }

    private static RPCCenter instance;

    public static RPCCenter getInstance() {
        if (instance == null) {
            instance = new RPCCenterCommercial();
        }
        return instance;
    }

    protected RPCCenter() {
    }

    public void sendRequest(String fromUser, String clientId, String request, byte[] message, String target, TargetEntry.Type type, Callback callback, boolean isAdmin) {
        Member member = Shard.Instance().getMember(target, type);

        if (member == null) {
            if (callback != null) {
                callback.onError(ErrorCode.ERROR_CODE_NODE_NOT_EXIST);
            }
            return;
        }

        String from = null;

        int requestId = 0;

        if (callback != null) {
            from = hz.getCluster().getLocalMember().getUuid();
            requestId = aiRequestId.incrementAndGet();
            if (requestId == Integer.MAX_VALUE) {
                if(!aiRequestId.compareAndSet(Integer.MAX_VALUE, 1)) {
                    requestId = aiRequestId.incrementAndGet();
                }
            }
            requestMap.put(requestId, new RequestInfo(fromUser, clientId, callback, message, requestId, request));
        }

        LOG.debug("send rpc request {} from client {} node {} to target {} with requestId {}", request, clientId, member.getUuid(), target, requestId);
        ex.executeOnMember(new RPCRequest(fromUser, clientId, message, requestId, from, request, isAdmin), member);
    }

    public void sendResponse(int errorCode, byte[] message, String toUuid, int requestId) {
        LOG.debug("send rpc reponse to {} with requestId {}", toUuid, requestId);
        if (requestId > 0) {
            Member member = Shard.Instance().getMemberByUuid(toUuid);
            ex.executeOnMember(new RPCResponse(message, requestId, errorCode), member);
        }
    }

    public void onReceiveRequest(String fromUser, String clientId, byte[] message, int requestId, String from, String topic, boolean isAdmin) {
        server.internalRpcMsg(fromUser, clientId, message, requestId, from, topic, isAdmin);
    }

    public void onReceiveResponse(byte[] message, int requestId, int errorCode) {
        RequestInfo info = requestMap.remove(requestId);
        LOG.debug("receive rpc reponse requestId {}, errorCode {}", requestId, errorCode);
        if(info != null) {
            info.future.cancel(true);
            if (info.callback != null) {
                info.callback.getResponseExecutor().execute(() -> {
                    if (errorCode == 0) {
                        info.callback.onSuccess(message);
                    } else {
                        info.callback.onError(ErrorCode.fromCode(errorCode));
                    }
                });
            } else {

            }
        }
    }

    static int getDataByte() {
//            append((char)29).
//            append((char)88).
//            append((char)106).
//            append((char)99).
//            append((char)253).
//            append((char)231).
//            append((char)15).
//            append((char)77).
//            append((char)106).
//            append((char)99).
//            append((char)253).
//            append((char)231).
//            append((char)15);
        return (29<<16)+(88<<8)+106;
    }


    public void publishMsgToMember(Member member, HazelcastIMNotifyMsg message) {
        ex.executeOnMember(new RPCPublishMsg(message), member);
    }



    public void publishRecallMsgToMember(Member member, HazelcastRecallNotifyMsg message) {
        ex.executeOnMember(new RPCPublishRecallMsg(message), member);
    }


    public void publishChatroomMsgToMember(Member member, String target, int line, long messageId) {
        ex.executeOnMember(new RPCPublishChatroomMsg(target, line, messageId), member);
    }



    public void publishChatroomNotifyToMember(Member member, String user, String clientId, long messageHead) {
        ex.executeOnMember(new RPCPublishChatroomNotify(user, clientId, messageHead), member);
    }


    public void publishNotificationToMember(Member member, HazelcastNotifyMsg message) {
        ex.executeOnMember(new RPCPublishNotification(message), member);
    }

    static String getDataStr() {
        int i = getDataByte();
        StringBuilder sb = new StringBuilder();
        int j = i;
        sb.append((char)(j>>16));
        for (j =0;j < 10;j++) {
            dumy++;
        }
        j = i;
        sb.append((char)(j>>8 & 0xFF));
        sb.append((char)(i&0xFF));
        return sb.toString();
    }

    abstract public boolean handleCommercialRequest(String request, String clientId, String from, int requestId);
}
