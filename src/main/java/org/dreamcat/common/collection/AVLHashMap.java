package org.dreamcat.common.collection;

import static org.dreamcat.common.collection.AVLHashNode.delete;
import static org.dreamcat.common.collection.AVLHashNode.drop;
import static org.dreamcat.common.collection.AVLHashNode.insert;
import static org.dreamcat.common.collection.AVLHashNode.select;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.dreamcat.common.collection.AVLHashNode;
import org.dreamcat.common.collection.AVLHashTree;
import org.dreamcat.common.util.ComparatorUtil;

/**
 * Create by tuke on 2020/4/18
 *
 * @see java.util.HashMap#put(Object, Object)
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class AVLHashMap<K extends Comparable<K>, V> extends AbstractMap<K, V> {

    static final int DEFAULT_INITIAL_CAPACITY = 16;
    static final int TREEIFY_THRESHOLD = 8;
    static final int UNTREEIFY_THRESHOLD = 6;
    static final int MAXIMUM_CAPACITY = 1 << 30;
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    Object[] table;
    int size;
    int threshold;

    private static int hash(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

    private static <K> int compare(K a, K b) {
        return ComparatorUtil.compare(a, b);
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        return false;
    }

    @Override
    public V get(Object key) {
        if (isEmpty()) return null;

        int hash = hash(key);
        int n = table.length - 1;
        Object o = table[hash & n];
        if (o instanceof Node) {
            Node<K, V> node = (Node<K, V>) o;
            for (; node != null; node = node.next) {
                if (compare(key, node.key) == 0) return node.value;
            }
            return null;
        } else if (o instanceof Tree) {
            Tree tree = (Tree) o;
            return (V) tree.get(key);
        } else {
            return null;
        }
    }

    @Override
    public V put(K key, V value) {
        if (table == null) resize();

        int hash = hash(key);
        int n = table.length - 1;
        Object o = table[hash & n];
        if (o instanceof Node) {
            Node<K, V> node = (Node<K, V>) o;
            Node<K, V> headNode = node;
            int linearSize = 0;
            boolean found = false;
            while (node.next != null) {
                if (compare(key, node.key) == 0) {
                    found = true;
                    break;
                }
                node = node.next;
                linearSize++;
            }
            if (found) {
                V oldValue = node.value;
                node.key = key;
                node.value = value;
                return oldValue;
            }
            node.next = new Node<>(hash, key, value);

            if (linearSize >= TREEIFY_THRESHOLD - 1) {
                table[hash & n] = treeify(headNode);
            }

            if (++size > threshold) resize();
            return null;
        } else if (o instanceof Tree) {
            Tree tree = (Tree) o;
            int treeSize = tree.size();
            V oldValue = (V) tree.put(hash, key, value);
            if (treeSize != tree.size) {
                if (++size > threshold) resize();
            }
            return oldValue;
        } else {
            table[hash & n] = new Node<>(hash, key, value);
            if (++size > threshold) resize();
            return null;
        }
    }

    @Override
    public V remove(Object key) {
        if (isEmpty()) return null;

        int hash = hash(key);
        int n = table.length - 1;
        Object o = table[hash & n];
        if (o instanceof Node) {
            Node<K, V> node = (Node<K, V>) o;
            Node<K, V> prev = null;
            while (node != null) {
                Node<K, V> next = node.next;
                if (node.key == key) {
                    // if node is the first head node
                    if (prev == null) {
                        table[hash & n] = next;
                    } else {
                        prev.next = next;
                    }
                    return node.value;
                }
                prev = node;
                node = next;
            }
            return null;
        } else if (o instanceof Tree) {
            Tree tree = (Tree) o;
            V oldValue = (V) tree.remove(key);
            if (tree.size < UNTREEIFY_THRESHOLD) {
                table[hash & n] = untreeify(tree);
            }
            return oldValue;
        } else {
            // not found
            return null;
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        m.forEach(this::put);
    }

    @Override
    public void clear() {
        if (table == null) return;
        Arrays.fill(table, null);
        size = 0;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return new AbstractSet<Entry<K, V>>() {
            @Override
            public int size() {
                return 0;
            }

            @Override
            public Iterator<Entry<K, V>> iterator() {
                return new Iter();
            }
        };
    }

    private void resize() {
        Object[] oldTab = table;
        if (oldTab == null) {
            table = new Object[DEFAULT_INITIAL_CAPACITY];
            threshold = (int) (DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
            return;
        }

        int oldCap = oldTab.length;
        if (oldCap >= MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return;
        }

        final int newCap = oldCap << 1;
        threshold = threshold << 1;

        Object[] newTab = new Object[newCap];
        for (int i = 0; i < oldCap; i++) {
            Object o = oldTab[i];
            if (o == null) continue;
            oldTab[i] = null;

            if (o instanceof Node) {
                Node<K, V> node = (Node<K, V>) o;
                while (node != null) {
                    Node<K, V> next = node.next;
                    node.next = null;

                    Object newBin = newTab[node.hash & (newCap - 1)];
                    if (newBin == null) {
                        newTab[node.hash & (newCap - 1)] = node;
                    } else if (newBin instanceof Node) {
                        Node<K, V> newBinNode = (Node<K, V>) newBin;
                        Node<K, V> headNode = newBinNode;
                        int linearSize = 0;
                        while (newBinNode.next != null) {
                            newBinNode = newBinNode.next;
                            linearSize++;
                        }
                        newBinNode.next = node;
                        if (linearSize >= TREEIFY_THRESHOLD - 1) {
                            newTab[node.hash & (newCap - 1)] = treeify(headNode);
                        }
                    } else if (newBin instanceof AVLHashTree) {
                        AVLHashTree newBinTree = (AVLHashTree) newBin;
                        newBinTree.put(node.key, node.value);
                    }
                    node = next;
                }
            } else if (o instanceof Tree) {
                Tree tree = (Tree) o;
                tree.forEach(it -> {
                    TreeNode<K, V> node = (TreeNode<K, V>) it;

                    Object newBin = newTab[node.hash & (newCap - 1)];
                    if (newBin == null) {
                        newTab[node.hash & (newCap - 1)] = node;
                    } else if (newBin instanceof Node) {
                        Node<K, V> newBinNode = (Node<K, V>) newBin;
                        Node<K, V> headNode = newBinNode;
                        int linearSize = 0;
                        while (newBinNode.next != null) {
                            newBinNode = newBinNode.next;
                            linearSize++;
                        }
                        newBinNode.next = new Node<>(node.hash, node.key, node.value);
                        if (linearSize >= TREEIFY_THRESHOLD - 1) {
                            newTab[node.hash & (newCap - 1)] = treeify(headNode);
                        }
                    } else if (newBin instanceof Tree) {
                        Tree newBinTree = (Tree) newBin;
                        newBinTree.put(node.hash, node.key, node.value);
                    }
                });
            }
        }

        table = newTab;
    }

    /// static class & methods

    private Tree treeify(Node<K, V> node) {
        Tree tree = new Tree();
        while (node != null) {
            tree.put(node.hash, node.key, node.value);
            Node<K, V> next = node.next;
            node.key = null;
            node.value = null;
            node.next = null;
            node = next;
        }
        return tree;
    }

    private Node<K, V> untreeify(Tree tree) {
        Node<K, V> headNode = null, prev = null;

        for (TreeNode root = tree.root; tree.popRoot(); root = tree.root) {

            if (prev == null) {
                headNode = new Node<>(root.hash, (K) root.key, (V) root.value);
                prev = headNode;
            }
            prev.next = new Node<>(root.hash, (K) root.key, (V) root.value);
        }
        tree.clear();
        return headNode;
    }

    static class Node<K, V> {

        int hash;
        K key;
        V value;
        Node<K, V> next;

        Node(int hash, K key, V value) {
            this.hash = hash;
            this.key = key;
            this.value = value;
        }
    }

    static class TreeNode<K, V> extends AVLHashNode<K, V> {

        int hash;

        TreeNode(int hash, K key, V value) {
            super(key, value);
            this.hash = hash;
        }
    }

    static class Tree<K, V> {

        private TreeNode<K, V> root;
        private int size;

        public int size() {
            return size;
        }

        public boolean isEmpty() {
            return size == 0;
        }

        public V get(K key) {
            if (isEmpty()) return null;
            AVLHashNode<K, V> hitNode = select(root, key);
            return hitNode == null ? null : hitNode.value;
        }

        public V put(int hash, K key, V value) {
            if (isEmpty()) {
                root = new TreeNode<>(hash, key, value);
                size++;
                return null;
            }
            AVLHashNode.WriteResult<K, V> result = AVLHashNode.WriteResult.empty();
            root = (TreeNode<K, V>) insert(root, key, value, false, result);
            V oldValue = result.value;
            if (result.applied && oldValue == null) size++;
            return oldValue;
        }

        public V remove(K key) {
            if (isEmpty()) return null;
            AVLHashNode.WriteResult<K, V> result = AVLHashNode.WriteResult.empty();
            root = (TreeNode<K, V>) delete(root, key, result);
            V oldValue = result.value;
            if (oldValue == null) size--;
            return oldValue;
        }

        public boolean remove(K key, V value) {
            if (isEmpty()) return false;
            AVLHashNode.WriteResult<K, V> result = AVLHashNode.WriteResult.empty();
            root = (TreeNode<K, V>) delete(root, key, value, true, result);
            if (result.applied) size--;
            return result.applied;
        }

        public void clear() {
            drop(root);
            root = null;
            size = 0;
        }

        public boolean popRoot() {
            if (root == null) return false;
            else return remove(root.key, root.value);
        }

        public void forEach(Consumer<TreeNode<K, V>> action) {
            if (root == null) return;
            root.inOrder(node -> {
                action.accept((TreeNode<K, V>) node);
            });
        }
    }

    class Iter implements Iterator<Entry<K, V>> {

        int pos;
        Node<K, V> node;
        Iterator treeIter;

        @Override
        public boolean hasNext() {
            if (pos >= table.length) return false;
            while (table[pos] == null) {
                pos++;
                if (pos >= table.length) return false;
            }
            return true;
        }

        @Override
        public Entry<K, V> next() {
            if (node != null) {
                Node<K, V> next = node.next;
                if (next != null) {
                    node = next;
                    return new AbstractMap.SimpleEntry<>(node.key, node.value);
                }
                return probingHead();
            }
            if (treeIter != null) {
                if (treeIter.hasNext()) {
                    AVLHashNode<K, V> hashNode = (AVLHashNode<K, V>) treeIter.next();
                    return new AbstractMap.SimpleEntry<>(hashNode.key, hashNode.value);
                }
                return probingHead();
            }
            return probingHead();
        }

        private Entry<K, V> probingHead() {
            if (pos >= table.length) return null;

            Object o;
            while ((o = table[pos]) == null) {
                pos++;
                if (pos >= table.length) return null;
            }
            if (o instanceof Node) {
                node = (Node<K, V>) o;
                return new AbstractMap.SimpleEntry<>(node.key, node.value);
            } else if (o instanceof Tree) {
                Tree tree = (Tree) o;
                TreeNode root = tree.root;
                treeIter = root.iterator();
                AVLHashNode<K, V> hashNode = (AVLHashNode<K, V>) treeIter.next();
                return new AbstractMap.SimpleEntry<>(hashNode.key, hashNode.value);
            }

            // never reach this
            throw new AssertionError();
        }
    }

}
