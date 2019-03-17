package io.moquette.persistence;

import com.hazelcast.core.*;
import io.moquette.BrokerConstants;
import io.netty.util.internal.StringUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static io.moquette.BrokerConstants.*;

public class ShardCommnuity extends Shard {
    private Member currentMember;

    protected ShardCommnuity() {
        super();
    }

    @Override
    public int[] getNetworkInterface() {
        return new int[0];
    }

    public void init(HazelcastInstance instance) {
        currentMember = instance.getCluster().getLocalMember();
    }

    public void addListener(Shard.MemberChangListener listener) {
    }
    public void removeListener(Shard.MemberChangListener listener) {
    }

    public boolean isCurrentNodeTarget(String target, TargetEntry.Type type) {
        return true;
    }

    public boolean isClusterMode() {
        return false;
    }
    public boolean isCurrentNode(Member member) {
        return true;
    }

    public Map<Member, Collection<String>> getMemberLocation(Collection<String> targets, TargetEntry.Type type) {
        Map<Member, Collection<String>> out = new HashMap<>();
        out.put(currentMember, targets);
        return out;
    }

    public Member getRandomMember() {
        return currentMember;
    }

    public Member getMember(String target, TargetEntry.Type type) {
        return currentMember;
    }

    public Member getMemberByUuid(String uuid) {
        return currentMember;
    }

}
