package org.dreamcat.common.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.dreamcat.common.util.StringUtil;

/**
 * Create by tuke on 2020/4/4
 */
public class SkipList<K, V> {

    private final float prob;
    private final int maxLevel;
    private final SkipNode<K, V> headNode;
    private final SkipNode<K, V> tailNode;
    private final SkipNode<K, V>[] last;
    // element count
    private int size;
    // skip levels
    private int levels;

    // default maxLevel is 10
    public SkipList() {
        this((0.8F));
    }

    // prob: probability of level, make maxLevel equals logN / log(1/p) - 1, N=2^32
    @SuppressWarnings("unchecked")
    public SkipList(double prob) {
        this.prob = (float) prob;
        this.maxLevel = maxLevel(this.prob);
        this.levels = 0;
        this.size = 0;
        this.headNode = new SkipNode<>(Integer.MIN_VALUE, null, null, maxLevel + 1);
        this.tailNode = new SkipNode<>(Integer.MAX_VALUE, null, null, 0);
        this.last = new SkipNode[maxLevel + 1];
        for (int i = 0; i <= maxLevel; i++) {
            headNode.nodes[i] = tailNode;
        }
    }

    /**
     * @param key key
     * @return Note that 0, empty string and null has same hash
     * @see HashMap#put(Object, Object)
     */
    static int hash(Object key) {
        return (key == null) ? 0 : key.hashCode();
    }

    static int maxLevel(float prob) {
        return (int) Math.ceil(-32 * Math.log(prob) / Math.log(2)) - 1;
    }

    public int size() {
        return size;
    }

    public V getOrDefault(K key, V defaultValue) {
        V v = get(key);
        return v != null ? v : defaultValue;
    }

    public V get(K key) {
        // empty list
        if (size == 0) return null;

        int hash = hash(key);
        SkipNode<K, V> beforeNode = headNode;
        for (int i = levels; i >= 0; i--) {
            while (beforeNode.nodes[i].hash < hash) {
                beforeNode = beforeNode.nodes[i];
            }
        }
        SkipNode<K, V> zeroNode = beforeNode.nodes[0];

        if (zeroNode.hash == hash) {
            return zeroNode.value;
        }
        return null;
    }

    public void put(K key, V value) {
        int hash = hash(key);
        SkipNode<K, V> node = search(hash);
        // already exist
        if (node.hash == hash) {
            node.key = key;
            node.value = value;
            return;
        }

        int level = allotLevel();
        if (level > levels) {
            level = ++levels;
            last[level] = headNode;
        }

        SkipNode<K, V> newNode = new SkipNode<>(hash, key, value, level + 1);
        for (int i = 0; i <= level; i++) {
            newNode.nodes[i] = last[i].nodes[i];
            last[i].nodes[i] = newNode;
        }
        size++;
    }

    public void remove(K key) {
        int hash = hash(key);
        SkipNode<K, V> node = search(hash);
        // not exist
        if (node.hash != hash) return;

        for (int i = 0; i < levels; i++) {
            if (last[i].nodes[i] != node) break;

            last[i].nodes[i] = node.nodes[i];
        }

        while (levels > 0 && headNode.nodes[levels] == tailNode) levels--;
        size--;

        // help GC
        node.key = null;
        node.value = null;
        int len = node.nodes.length;
        for (int i = 0; i < len; i++) node.nodes[i] = null;
    }

    public String prettyToString() {
        return prettyToString(4);
    }

    public String prettyToString(int width) {
        List<Integer> hashes = new ArrayList<>(size);
        SkipNode<K, V> node = headNode.nodes[0];
        while (node.key != null) {
            hashes.add(node.hash);
            node = node.nodes[0];
        }

        Integer[][] chains = new Integer[levels + 1][hashes.size()];
        for (int i = levels; i >= 1; i--) {
            SkipNode<K, V> iNode = headNode.nodes[i];
            while (iNode.key != null) {
                int hash = iNode.hash;
                int ind = hashes.indexOf(hash);
                chains[levels - i][ind] = hash;
                iNode = iNode.nodes[0];
            }
        }
        chains[levels] = hashes.toArray(new Integer[0]);
        return Arrays.stream(chains)
                .map(chain -> Arrays.stream(chain)
                        .map(it -> it == null ?
                                StringUtil.repeat(' ', width) :
                                String.format("%" + width + "d", it))
                        .collect(Collectors.joining(" ")))
                .collect(Collectors.joining("\n"));
    }

    // search and build last
    final SkipNode<K, V> search(int hash) {
        SkipNode<K, V> beforeNode = headNode;
        for (int i = levels; i >= 0; i--) {
            while (true) {
                SkipNode<K, V> iNode = beforeNode.nodes[i];
                if (iNode.hash >= hash) break;
                beforeNode = iNode;
            }
            last[i] = beforeNode;
        }
        return beforeNode.nodes[0];
    }

    // allocate a random level
    final int allotLevel() {
        int level = 0;
        while (Math.random() <= prob) level++;
        return Math.min(level, maxLevel);
    }
}
