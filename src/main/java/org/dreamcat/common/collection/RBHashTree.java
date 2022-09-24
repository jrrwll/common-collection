package org.dreamcat.common.collection;

import static org.dreamcat.common.collection.RBHashNode.delete;
import static org.dreamcat.common.collection.RBHashNode.drop;
import static org.dreamcat.common.collection.RBHashNode.insert;
import static org.dreamcat.common.collection.RBHashNode.select;

import org.dreamcat.common.function.QuaConsumer;

/**
 * Create by tuke on 2020/4/19
 */
public class RBHashTree<K, V> implements HashTree<K, V, RBHashNode<K, V>> {

    private RBHashNode<K, V> root;
    private int size;

    public RBHashNode<K, V> getRoot() {
        return root;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean containsKey(K key) {
        if (isEmpty()) return false;
        RBHashNode<K, V> hitNode = select(root, key);
        return hitNode != null;
    }

    // Note that `get(key) != null` is equivalent to `containsKey(key)`
    //  only when all values are not null
    public V get(K key) {
        if (isEmpty()) return null;
        RBHashNode<K, V> hitNode = select(root, key);
        return hitNode == null ? null : hitNode.value;
    }

    public V put(K key, V value) {
        if (isEmpty()) {
            root = new RBHashNode<>(false, key, value);
            size++;
            return null;
        }

        RBHashNode.WriteResult<K, V> result = RBHashNode.WriteResult.empty();
        root = RBHashNode.insert(root, key, value, false, result);

        V oldValue = result.value;
        if (result.applied && oldValue == null) size++;
        return oldValue;
    }

    public V remove(K key) {
        if (isEmpty()) return null;
        RBHashNode.WriteResult<K, V> result = RBHashNode.WriteResult.empty();
        root = delete(root, key, result);
        V oldValue = result.value;
        if (oldValue == null) size--;
        return oldValue;
    }

    public void clear() {
        drop(root);
        root = null;
        size = 0;
    }

    // ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ====

    /// enhanced methods
    public V getOrDefault(K key, V defaultValue) {
        if (isEmpty()) return defaultValue;
        RBHashNode<K, V> hitNode = select(root, key);
        return hitNode == null ? defaultValue : hitNode.value;
    }

    public V putIfAbsent(K key, V value) {
        if (isEmpty()) {
            root = new RBHashNode<>(false, key, value);
            size++;
            return null;
        }
        RBHashNode.WriteResult<K, V> result = RBHashNode.WriteResult.empty();
        root = insert(root, key, value, true, result);
        V oldValue = result.value;
        if (result.applied && oldValue == null) size++;
        return oldValue;
    }

    public boolean remove(K key, V value) {
        if (isEmpty()) return false;
        RBHashNode.WriteResult<K, V> result = RBHashNode.WriteResult.empty();
        root = delete(root, key, value, true, result);
        if (result.applied) size--;
        return result.applied;
    }

    public boolean replace(K key, V oldValue, V newValue) {
        if (isEmpty()) return false;
        RBHashNode<K, V> hitNode = select(root, key);
        if (hitNode == null || hitNode.value != oldValue) return false;
        hitNode.value = newValue;
        return true;
    }

    public V replace(K key, V value) {
        RBHashNode<K, V> hitNode = select(root, key);
        if (hitNode == null) return null;
        V oldValue = hitNode.value;
        hitNode.value = value;
        return oldValue;
    }

    // ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ====

    public void levelOrder(QuaConsumer<? super K, ? super V, Boolean, Integer> action) {
        if (isEmpty()) return;

        getRoot().levelOrder((node, level) -> {
            action.accept(node.getKey(), node.getValue(), node.red, level);
        });
    }

}
