package org.dreamcat.common.collection;

import static org.dreamcat.common.collection.AVLHashNode.delete;
import static org.dreamcat.common.collection.AVLHashNode.drop;
import static org.dreamcat.common.collection.AVLHashNode.insert;
import static org.dreamcat.common.collection.AVLHashNode.select;

/**
 * Create by tuke on 2020/4/19
 */
public class AVLHashTree<K, V> implements HashTree<K, V, AVLHashNode<K, V>> {

    private AVLHashNode<K, V> root;
    private int size;

    @Override
    public AVLHashNode<K, V> getRoot() {
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
        AVLHashNode<K, V> hitNode = select(root, key);
        return hitNode != null;
    }

    public V get(K key) {
        if (isEmpty()) return null;
        AVLHashNode<K, V> hitNode = select(root, key);
        return hitNode == null ? null : hitNode.value;
    }

    public V put(K key, V value) {
        if (isEmpty()) {
            root = new AVLHashNode<>(key, value);
            size++;
            return null;
        }
        AVLHashNode.WriteResult<K, V> result = AVLHashNode.WriteResult.empty();
        root = insert(root, key, value, false, result);
        V oldValue = result.value;
        if (result.applied && oldValue == null) size++;
        return oldValue;
    }

    public V remove(K key) {
        if (isEmpty()) return null;
        AVLHashNode.WriteResult<K, V> result = AVLHashNode.WriteResult.empty();
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

    /// enhanced methods

    public V getOrDefault(K key, V defaultValue) {
        if (isEmpty()) return defaultValue;
        AVLHashNode<K, V> hitNode = select(root, key);
        return hitNode == null ? defaultValue : hitNode.value;
    }

    public V putIfAbsent(K key, V value) {
        if (isEmpty()) {
            root = new AVLHashNode<>(key, value);
            size++;
            return null;
        }
        AVLHashNode.WriteResult<K, V> result = AVLHashNode.WriteResult.empty();
        root = insert(root, key, value, true, result);
        V oldValue = result.value;
        if (result.applied && oldValue == null) size++;
        return oldValue;
    }

    public boolean remove(K key, V value) {
        if (isEmpty()) return false;
        AVLHashNode.WriteResult<K, V> result = AVLHashNode.WriteResult.empty();
        root = delete(root, key, value, true, result);
        if (result.applied) size--;
        return result.applied;
    }

    public boolean replace(K key, V oldValue, V newValue) {
        if (isEmpty()) return false;
        AVLHashNode<K, V> hitNode = select(root, key);
        if (hitNode == null || hitNode.value != oldValue) return false;
        hitNode.value = newValue;
        return true;
    }

    public V replace(K key, V value) {
        AVLHashNode<K, V> hitNode = select(root, key);
        if (hitNode == null) return null;
        V oldValue = hitNode.value;
        hitNode.value = value;
        return oldValue;
    }
}
