package org.dreamcat.common.collection;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dreamcat.common.util.ComparatorUtil;

/**
 * Create by tuke on 2020/4/19
 * <p>
 * use Comparable and hashCode to compare
 *
 * @see ComparatorUtil#compare(Object, Object)
 */
@Getter
@AllArgsConstructor
public class AVLHashNode<K, V> extends AVLNode<AVLHashNode<K, V>> implements
        Comparable<AVLHashNode<K, V>>, HashNode<K, V> {

    protected K key;
    protected V value;

    static <K, V> AVLHashNode<K, V> insert(
            AVLHashNode<K, V> node, K key, V value,
            boolean onlyIfAbsent, WriteResult<K, V> result) {
        if (node == null) {
            if (result != null) result.applied = true;
            node = new AVLHashNode<>(key, value);
            return node;
        }

        // override it
        if (compare(key, node.key) == 0) {
            if (onlyIfAbsent) {
                // save old value to result
                if (result != null) result.update(false, node.key, node.value);
            } else {
                // save old value to result
                if (result != null) result.update(true, node.key, node.value);
                node.key = key;
                node.value = value;
            }
            return node;
        }

        if (compare(key, node.key) > 0) {
            node.right = insert(node.right, key, value, onlyIfAbsent, result);
        } else {
            node.left = insert(node.left, key, value, onlyIfAbsent, result);
        }
        return AVLNode.balance(node);
    }

    /// static tree methods

    static <K, V> AVLHashNode<K, V> delete(
            AVLHashNode<K, V> node, K key, WriteResult<K, V> result) {
        return delete(node, key, null, false, result);
    }

    static <K, V> AVLHashNode<K, V> delete(
            AVLHashNode<K, V> node, K key, V value,
            boolean matchValue, WriteResult<K, V> result) {
        if (node == null) return null;

        if (compare(key, node.key) == 0) {
            if (matchValue && value != node.value) {
                // save old value to result
                if (result != null) result.update(false, node.key, node.value);
                return node;
            }

            // save old value to result
            if (result != null) result.update(true, node.key, node.value);

            // delete self
            if (node.left != null && node.right != null) {
                // return the leftest node of the right node
                AVLHashNode<K, V> right = node.right;
                if (right.left == null) {
                    right.left = node.left;
                    nullify(node);
                    return right;
                } else {
                    AVLHashNode<K, V> leftest = popLeftest(right);
                    leftest.left = node.left;
                    leftest.right = right;
                    return leftest;
                }
            } else if (node.left != null) {
                AVLHashNode<K, V> left = node.left;
                nullify(node);
                return left;
            } else {
                AVLHashNode<K, V> right = node.right;
                nullify(node);
                return right;
            }
        } else if (compare(key, node.key) > 0) {
            node.right = delete(node.right, key, value, matchValue, result);
        } else {
            node.left = delete(node.left, key, value, matchValue, result);
        }

        return AVLNode.balance(node);
    }

    static <K, V> AVLHashNode<K, V> select(AVLHashNode<K, V> node, K key) {
        while (node != null) {
            if (compare(key, node.key) == 0) {
                return node;
            } else if (compare(key, node.key) > 0) {
                node = node.right;
            } else {
                node = node.left;
            }
        }
        return null;
    }

    static <K, V> AVLHashNode<K, V> update(AVLHashNode<K, V> node, K key, V value) {
        AVLHashNode<K, V> hitNode = select(node, key);
        if (hitNode != null) {
            hitNode.key = key;
            hitNode.value = value;
        }
        return hitNode;
    }

    static <K, V> void drop(AVLHashNode<K, V> node) {
        if (node == null) return;
        drop(node.left);
        drop(node.right);
        // help GC
        nullify(node);
    }

    private static <K, V> AVLHashNode<K, V> popLeftest(AVLHashNode<K, V> node) {
        if (node.left.left == null) {
            AVLHashNode<K, V> leftest = node.left;
            node.left = null;
            return leftest;
        }
        return popLeftest(node.left);
    }

    private static void nullify(AVLHashNode<?, ?> node) {
        node.left = null;
        node.right = null;
        node.key = null;
        node.value = null;
    }

    private static <K> int compare(K a, K b) {
        return ComparatorUtil.compare(a, b);
    }

    public V setValue(V value) {
        V oldValue = this.value;
        this.value = value;
        return oldValue;
    }

    /// static class & methods

    @Override
    public int compareTo(AVLHashNode<K, V> o) {
        return ComparatorUtil.compare(key, o.key);
    }

    static class WriteResult<K, V> {

        boolean applied;
        K key;
        V value;

        static <K, V> WriteResult<K, V> empty() {
            return new WriteResult<>();
        }

        void update(boolean applied, K key, V value) {
            //
            this.applied = applied;
            this.key = key;
            this.value = value;
        }
    }

}
