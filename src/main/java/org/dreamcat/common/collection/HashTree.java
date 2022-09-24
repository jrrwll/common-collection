package org.dreamcat.common.collection;

import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import org.dreamcat.common.function.TriConsumer;

/**
 * Create by tuke on 2020/4/25
 */
public interface HashTree<K, V, Node extends BinaryNode<Node> & HashNode<K, V>> {

    Node getRoot();

    int size();

    default boolean isEmpty() {
        return size() == 0;
    }

    boolean containsKey(K key);

    // Note that it's a slow level order for-each operation
    default boolean containsValue(V value) {
        Iterator<V> iter = valueIterator();
        while (iter.hasNext()) {
            if (iter.next() == value) return true;
        }
        return false;
    }

    // Note that `get(key) != null` is equivalent to `containsKey(key)`
    //  only when all values are not null
    V get(K key);

    V put(K key, V value);

    V remove(K key);

    default void putAll(HashTree<? extends K, ? extends V, ? extends Node> tree) {
        tree.forEach(this::put);
    }

    void clear();

    /// enhanced methods

    V getOrDefault(K key, V defaultValue);

    V putIfAbsent(K key, V value);

    boolean remove(K key, V value);

    boolean replace(K key, V oldValue, V newValue);

    V replace(K key, V value);

    default void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        if (isEmpty()) return;

        Node root = getRoot();
        root.inOrder(node -> {
            node.setValue(function.apply(node.getKey(), node.getValue()));
        });
    }

    /// for-each

    /**
     * immutable key iterator
     *
     * @return a immutable iterator
     */
    default Iterator<K> keyIterator() {
        return new KeyIter<>(getRoot());
    }

    default Iterator<V> valueIterator() {
        return new ValueIter<>(getRoot());
    }

    /**
     * in order for-each
     *
     * @param action for-each action
     */
    default void forEach(BiConsumer<? super K, ? super V> action) {
        inOrder(action);
    }

    default void preOrder(BiConsumer<? super K, ? super V> action) {
        if (isEmpty()) return;

        getRoot().preOrder(node -> {
            action.accept(node.getKey(), node.getValue());
        });
    }

    default void inOrder(BiConsumer<? super K, ? super V> action) {
        if (isEmpty()) return;

        getRoot().inOrder(node -> {
            action.accept(node.getKey(), node.getValue());
        });
    }

    default void postOrder(BiConsumer<? super K, ? super V> action) {
        if (isEmpty()) return;

        getRoot().postOrder(node -> {
            action.accept(node.getKey(), node.getValue());
        });
    }

    default void levelOrder(TriConsumer<? super K, ? super V, Integer> action) {
        if (isEmpty()) return;

        BinaryNodes.levelOrder(getRoot(), (node, level) -> {
            action.accept(node.getKey(), node.getValue(), level);
        });
    }

    class KeyIter<K, V, Node extends BinaryNode<Node> & HashNode<K, V>> implements Iterator<K> {

        private final BinaryNode.Iter<Node> delegate;

        public KeyIter(Node root) {
            this.delegate = new BinaryNode.Iter<>(root);
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public K next() {
            return delegate.next().getKey();
        }
    }

    class ValueIter<K, V, Node extends BinaryNode<Node> & HashNode<K, V>> implements Iterator<V> {

        private final BinaryNode.Iter<Node> delegate;

        public ValueIter(Node root) {
            this.delegate = new BinaryNode.Iter<>(root);
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public V next() {
            return delegate.next().getValue();
        }
    }
}
