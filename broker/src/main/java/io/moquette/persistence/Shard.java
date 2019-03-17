package io.moquette.persistence;

import com.hazelcast.core.*;
import io.moquette.BrokerConstants;
import io.netty.util.internal.StringUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static io.moquette.BrokerConstants.*;

abstract public class Shard {
    public interface MemberChangListener {
        void onTargetAddedToCurrentNode(TargetEntry target);
        void onTargetRemovedFromCurrentNode(TargetEntry target);
    }


    private static Shard INSTANCE;


    public static Shard Instance() {
        if (INSTANCE == null) {
            INSTANCE = new ShardCommercial();
        }
        return INSTANCE;
    }


    protected Shard() {

    }

    abstract public int[] getNetworkInterface();
    abstract public void init(HazelcastInstance instance);

    abstract public void addListener(MemberChangListener listener);
    abstract public void removeListener(MemberChangListener listener);

    abstract public boolean isCurrentNodeTarget(String target, TargetEntry.Type type);

    abstract public boolean isClusterMode();
    abstract public boolean isCurrentNode(Member member);

    abstract public Map<Member, Collection<String>> getMemberLocation(Collection<String> targets, TargetEntry.Type type);

    abstract public Member getRandomMember();
    abstract public Member getMember(String target, TargetEntry.Type type);

    abstract public Member getMemberByUuid(String uuid);
}
