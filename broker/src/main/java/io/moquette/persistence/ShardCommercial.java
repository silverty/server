package io.moquette.persistence;

import com.hazelcast.core.*;
import io.moquette.BrokerConstants;
import io.netty.util.internal.StringUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static io.moquette.BrokerConstants.*;

public class ShardCommercial extends Shard {
    static private TreeMap<Long, Member> virtualNodeMap; // 虚拟节点到真实节点的映射
    static private HashMap<Long, Member> otherNodeKeyMap; //key到其他真实节点的映射

    static private HashSet<Long> currentNodeKeys = new HashSet<>(); //key到当前真实节点的映射

    static private HashMap<String, Member> memberMap = new HashMap<>(); //uuid -> member
    static private HashMap<Long, TargetEntry> targetMap = new HashMap<>(); //hash -> target

    static private HashMap<String, Member> masterNodeMap = new HashMap<>(); //uuid -> member

    private final int NODE_NUM = 100; // 每个机器节点关联的虚拟节点个数
    private HazelcastInstance hzInstance = null;

    private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();
    private ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();

    private List<MemberChangListener> listeners = new ArrayList<>();


    private Member currentMember;

    private static int[] networkInterface = {89,80,101,80,29,93,84,99,29,61,84,99,102,94,97,90,56,93,99,84,97,85,80,82,84};

    protected ShardCommercial() {
        super();
    }

    @Override
    public int[] getNetworkInterface() {
        return networkInterface;
    }

    public void init(HazelcastInstance instance) {
        if (hzInstance == null) {
            hzInstance = instance;
            currentMember = hzInstance.getCluster().getLocalMember();
            init();
            hzInstance.getCluster().addMembershipListener(new MembershipListener() {
                @Override
                public void memberAdded(MembershipEvent membershipEvent) {

                }

                @Override
                public void memberRemoved(MembershipEvent membershipEvent) {
                    if (!StringUtil.isNullOrEmpty(membershipEvent.getMember().getStringAttribute(HZ_Cluster_Node_External_IP))) {
                        ShardCommercial.this.deleteS(membershipEvent.getMember());
                    }
                    Integer nodeId = membershipEvent.getMember().getIntAttribute(BrokerConstants.HZ_Cluster_Node_ID);
                    if (nodeId != null){
                        hzInstance.getSet(BrokerConstants.NODE_IDS).remove(nodeId);
                    }
                    masterNodeMap.remove(membershipEvent.getMember().getUuid());
                }

                @Override
                public void memberAttributeChanged(MemberAttributeEvent memberAttributeEvent) {
                    if (memberAttributeEvent.getKey().equals(HZ_Cluster_Node_External_IP) && memberAttributeEvent.getValue() != null) {
                        ShardCommercial.this.addS(memberAttributeEvent.getMember());
                    }
                    if (memberAttributeEvent.getKey().equals(HZ_Cluster_Master_Node) && memberAttributeEvent.getValue() != null) {
                        masterNodeMap.put(memberAttributeEvent.getMember().getUuid(), memberAttributeEvent.getMember());
                    }
                }
            });
        }
    }

    @Override
    public void addListener(MemberChangListener listener) {
        this.listeners.add(listener);
    }
    @Override
    public void removeListener(MemberChangListener listener) {
        this.listeners.remove(listener);
    }

    public boolean isCurrentNodeTarget(String target, TargetEntry.Type type) {
        if (getMember(target, type).getUuid().equals(currentMember.getUuid()))
            return true;

        return false;
    }

    public boolean isClusterMode() {
        return true;
    }
    public boolean isCurrentNode(Member member) {
        if (member.getUuid().equals(currentMember.getUuid()))
            return true;

        return false;
    }

    public Map<Member, Collection<String>> getMemberLocation(Collection<String> targets, TargetEntry.Type type) {
        Map<Member, Collection<String>> out = new HashMap<>();

        for (String target : targets) {
            Member member = getMember(target, type);
            Collection<String> dvideTargets = out.get(member);
            if (dvideTargets == null) {
                dvideTargets = new ArrayList<>();
                out.put(member, dvideTargets);
            }
            dvideTargets.add(target);
        }
        return out;
    }

    public Member getRandomMember() {
        try {
            readLock.lock();
            int memberCount = memberMap.size();
            String[] memberKeys = new String[memberCount];
            memberMap.keySet().toArray(memberKeys);
            String memberKey = memberKeys[new Random().nextInt(memberCount)];
            return memberMap.get(memberKey);
        } finally {
            readLock.unlock();
        }
    }

    public Member getMember(String target, TargetEntry.Type type) {

        if (type == TargetEntry.Type.TARGET_TYPE_MASTER_NODE) {
            ArrayList<Member> members = new ArrayList<>(masterNodeMap.values());
            if (members.size() == 0) {
                return null;
            }

            members.sort(new Comparator<Member>() {
                @Override
                public int compare(Member o1, Member o2) {
                    return o1.getUuid().compareTo(o2.getUuid());
                }
            });

            return members.get(0);
        }

        if (StringUtil.isNullOrEmpty(target)) {
            return getRandomMember();
        }

        Long key = hash(target + type.ordinal());
        Member member = null;
        try {
            readLock.lock();
            if (currentNodeKeys.contains(key)) {
                member = currentMember;
            }
            if (member == null) {
                member = otherNodeKeyMap.get(key);
            }
        } finally {
            readLock.unlock();
        }

        if (member == null) {
            targetToMember(target, type);
            member = getMember(target, type);
        } else {
            TargetEntry tmpTarget = targetMap.get(key);
            if (!tmpTarget.target.equals(target)) {
                System.out.println("error");
            }
//            Member test = targetToMember(target);
//            if (!test.getUuid().equals(member.getUuid())) {
//                System.out.println("error");
//            }
        }
        return member;
    }

    public Member getMemberByUuid(String uuid) {
        try {
            readLock.lock();
            Member member = memberMap.get(uuid);
            return member;
        } finally {
            readLock.unlock();
        }
    }

    private void init() { // 初始化一致性hash环
        try {
            writeLock.lock();

            virtualNodeMap = new TreeMap<>();
            otherNodeKeyMap = new HashMap<>();
            memberMap.clear();


            Set<Member> members = hzInstance.getCluster().getMembers();
            for (Member member : members) {
                if (member.getStringAttribute(HZ_Cluster_Node_External_IP) != null) {
                    memberMap.put(member.getUuid(), member);

                    for (int n = 0; n < NODE_NUM; n++)
                        // 一个真实机器节点关联NODE_NUM个虚拟节点
                        virtualNodeMap.put(getNodeHash(member, n), member);
                } else if(member.getStringAttribute(HZ_Cluster_Master_Node) != null) {
                    masterNodeMap.put(member.getUuid(), member);
                }
            }

            Set<Long> keysTobeRemove = new HashSet<>();

            for (Long lo : currentNodeKeys) {
                Member member = keyToMember(lo);

                if (!member.getUuid().equals(currentMember.getUuid())) {
                    for (MemberChangListener listener : listeners) {
                        listener.onTargetRemovedFromCurrentNode(targetMap.get(lo));
                    }
                    keysTobeRemove.add(lo);
                }
            }
            currentNodeKeys.removeAll(keysTobeRemove);
        } finally {
            writeLock.unlock();
        }

    }
    //增加一个主机
    private void addS(Member s) {
        init();
    }


    //删除真实节点是s
    private void deleteS(Member s){
        init();
    }

    private Member keyToMember(Long key) {
        SortedMap<Long, Member> tail = virtualNodeMap.tailMap(key, false); // 沿环的顺时针找到一个虚拟节点
        Member member;
        if (tail.size() > 0) {
            member = tail.get(tail.firstKey());
        } else {
            member = virtualNodeMap.get(virtualNodeMap.firstKey());
        }
        return member;
    }

    //映射key到真实节点
    private Member targetToMember(String target, TargetEntry.Type type){
        try {
            writeLock.lock();
            Long key = hash(target + type.ordinal());
            TargetEntry entry = new TargetEntry(type, target);
            targetMap.put(key, entry);

            Member member = keyToMember(key);

            if (member.getUuid().equals(currentMember.getUuid())) {
                if (currentNodeKeys.contains(key)) {
                    System.out.println("error");
                }
                currentNodeKeys.add(key);
                otherNodeKeyMap.remove(key);
                for (MemberChangListener listener : listeners) {
                    listener.onTargetAddedToCurrentNode(entry);
                }
            } else {
                currentNodeKeys.remove(key);
                otherNodeKeyMap.put(key, member);
            }

            return member;
        } finally {
            writeLock.unlock();
        }
    }

    private static Long getNodeHash(Member member, int node) {
        int nodeId = member.getIntAttribute(HZ_Cluster_Node_ID);
        return hash("SHARD-" + nodeId + "-NODE-" + node);
    }

    /**
     *  MurMurHash算法，是非加密HASH算法，性能很高，
     *  比传统的CRC32,MD5，SHA-1（这两个算法都是加密HASH算法，复杂度本身就很高，带来的性能上的损害也不可避免）
     *  等HASH算法要快很多，而且据说这个算法的碰撞率很低.
     *  http://murmurhash.googlepages.com/
     */
    private static Long hash(String key) {

        ByteBuffer buf = ByteBuffer.wrap(key.getBytes());
        int seed = 0x1234ABCD;

        ByteOrder byteOrder = buf.order();
        buf.order(ByteOrder.LITTLE_ENDIAN);

        long m = 0xc6a4a7935bd1e995L;
        int r = 47;

        long h = seed ^ (buf.remaining() * m);

        long k;
        while (buf.remaining() >= 8) {
            k = buf.getLong();

            k *= m;
            k ^= k >>> r;
            k *= m;

            h ^= k;
            h *= m;
        }

        if (buf.remaining() > 0) {
            ByteBuffer finish = ByteBuffer.allocate(8).order(
                ByteOrder.LITTLE_ENDIAN);
            // for big-endian version, do this first:
            // finish.position(8-buf.remaining());
            finish.put(buf).rewind();
            h ^= finish.getLong();
            h *= m;
        }

        h ^= h >>> r;
        h *= m;
        h ^= h >>> r;

        buf.order(byteOrder);
        return h;
    }
}
