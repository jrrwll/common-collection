package org.dreamcat.common.collection;

import java.util.Objects;

/**
 * Create by tuke on 2020/4/4
 */
class SkipNode<K, V> {

    int hash;
    K key;
    V value;
    // skip node point in i level
    SkipNode<K, V>[] nodes;

    @SuppressWarnings("unchecked")
    SkipNode(int hash, K key, V value, int size) {
        this.hash = hash;
        this.key = key;
        this.value = value;
        this.nodes = new SkipNode[size];
    }

    public final K getKey() {
        return key;
    }

    public final V getValue() {
        return value;
    }

    public final String toString() {
        return key + "=" + value;
    }

    public final int hashCode() {
        return Objects.hashCode(key) ^ Objects.hashCode(value);
    }

    public final boolean equals(Object o) {
        if (o == this)
            return true;
        if (o instanceof SkipNode) {
            SkipNode<?, ?> node = (SkipNode<?, ?>) o;
            return Objects.equals(key, node.getKey()) &&
                    Objects.equals(value, node.getValue());
        }
        return false;
    }
}
