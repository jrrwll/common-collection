package org.dreamcat.common.collection;

import lombok.Getter;
import org.dreamcat.common.util.ComparatorUtil;

/**
 * Create by tuke on 2020/4/19
 */
@Getter
public class RBHashNode<K, V> extends RBNode<RBHashNode<K, V>> implements
        Comparable<RBHashNode<K, V>>, HashNode<K, V> {

    protected K key;
    protected V value;

    public RBHashNode(boolean red, K key, V value) {
        this.red = red;
        this.key = key;
        this.value = value;
    }

    public RBHashNode(boolean red, K key, V value, RBHashNode<K, V> parent) {
        this(red, key, value);
        this.parent = parent;
    }

    static <K, V> RBHashNode<K, V> insert(
            RBHashNode<K, V> root, K key, V value,
            boolean onlyIfAbsent, WriteResult<K, V> result) {
        RBHashNode<K, V> hitNode = search(root, key);

        if (compare(key, hitNode.key) == 0) {
            if (onlyIfAbsent) {
                result.update(false, hitNode.key, hitNode.value);
                return root;
            }

            result.update(true, hitNode.key, hitNode.value);
            hitNode.key = key;
            hitNode.value = value;
            return root;
        }

        RBHashNode<K, V> node = new RBHashNode<>(true, key, value, hitNode);
        result.update(true);
        if (compare(key, hitNode.key) > 0) {
            hitNode.right = node;
        } else {
            hitNode.left = node;
        }

        return balanceInsertion(root, node);
    }

    static <K, V> RBHashNode<K, V> delete(RBHashNode<K, V> root, K key, WriteResult<K, V> result) {
        return delete(root, key, null, false, result);
    }

    static <K, V> RBHashNode<K, V> delete(
            RBHashNode<K, V> root, K key, V value,
            boolean matchValue, WriteResult<K, V> result) {
        RBHashNode<K, V> x = search(root, key);
        if (compare(key, x.key) != 0) {
            return root;
        }
        if (matchValue && value != x.value) {
            // save old value to result
            if (result != null) result.update(false, x.key, x.value);
            return root;
        }
        // save old value to result
        if (result != null) result.update(true, x.key, x.value);

        RBHashNode<K, V> replacement;
        if (x.left != null && x.right != null) {
            RBHashNode<K, V> xl = x.left, xr = x.right;

            // find leftest successor
            RBHashNode<K, V> s = xr, sl;
            while ((sl = s.left) != null)
                s = sl;
            // swap colors
            boolean c = s.red;
            s.red = x.red;
            x.red = c;

            RBHashNode<K, V> sr = s.right;
            RBHashNode<K, V> p = x.parent;
            // replacement is sr != null ? sr : x
            /*
            x is s's direct parent
            ---- ---- ---- ----    ---- ---- ---- ----    ---- ---- ---- ----
                          p
                x
            xl        s
                 NULL  sr
            ---- ---- ---- ----    ---- ---- ---- ----    ---- ---- ---- ----
                             p
                  s
            xl          x
                   NULL    sr
            ---- ---- ---- ----    ---- ---- ---- ----    ---- ---- ---- ----
            */
            if (s == xr) {
                x.parent = s;
                s.right = x;
            }
            /*
            ---- ---- ---- ----    ---- ---- ---- ----    ---- ---- ---- ----
                                  p
                  x
            xl               xr
                         ...
                       sp
                     s
                NULL  sr
            ---- ---- ---- ----    ---- ---- ---- ----    ---- ---- ---- ----
                                 p
                 s
            xl               xr
                          ...
                      sp
                   x     sr
            ---- ---- ---- ----    ---- ---- ---- ----    ---- ---- ---- ----
            */
            else {
                RBHashNode<K, V> sp = s.parent;
                if ((x.parent = sp) != null) {
                    if (s == sp.left)
                        sp.left = x;
                    else
                        sp.right = x;
                }
                s.right = xr;
                xr.parent = s;
            }
            x.left = null;
            if ((x.right = sr) != null)
                sr.parent = x;

            s.left = xl;
            xl.parent = s;
            if ((s.parent = p) == null)
                root = s;
            else if (x == p.left)
                p.left = s;
            else
                p.right = s;
            // replacement sr or x when sr is null
            if (sr != null)
                replacement = sr;
            else
                replacement = x;
        } else if (x.left != null) {
            replacement = x.left;
        } else if (x.right != null) {
            replacement = x.right;
        } else {
            replacement = x;
        }

        boolean c = x.red;
        if (replacement != x) {
            // use sr replace x, and nullify x
            RBHashNode<K, V> p = replacement.parent = x.parent;
            if (p == null)
                root = replacement;
            else if (x == p.left)
                p.left = replacement;
            else
                p.right = replacement;
            //x.left = x.right = x.parent = null;
            x.nullify();
        }

        // only balance when deleting a black node
        root = c ? root : balanceDeletion(root, replacement);

        // detach replacement
        if (replacement == x) {
            RBHashNode<K, V> p = x.parent;
            x.parent = null;
            if (p != null) {
                if (x == p.left)
                    p.left = null;
                else if (x == p.right)
                    p.right = null;
            }
        }
        return root;
    }

    /// static tree methods

    static <K, V> RBHashNode<K, V> select(RBHashNode<K, V> node, K key) {
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

    static <K, V> RBHashNode<K, V> update(RBHashNode<K, V> node, K key, V value) {
        RBHashNode<K, V> hitNode = select(node, key);
        if (hitNode != null) {
            hitNode.key = key;
            hitNode.value = value;
        }
        return hitNode;
    }

    static <K, V> void drop(RBHashNode<K, V> node) {
        if (node == null) return;
        drop(node.left);
        drop(node.right);
        // help GC
        node.nullify();
    }

    // return hit node or parent node
    private static <K, V> RBHashNode<K, V> search(RBHashNode<K, V> root, K key) {
        RBHashNode<K, V> node = root;
        while (compare(key, node.key) != 0) {
            if (compare(key, node.key) > 0) {
                if (node.right == null) return node;
                else node = node.right;
            } else {
                if (node.left == null) return node;
                else node = node.left;
            }
        }
        return node.parent;
    }

    private static <K> int compare(K a, K b) {
        return ComparatorUtil.compare(a, b);
    }

    public V setValue(V value) {
        V oldValue = this.value;
        this.value = value;
        return oldValue;
    }

    protected void nullify() {
        super.nullify();
        key = null;
        value = null;
    }

    /// static class & methods

    @Override
    public int compareTo(RBHashNode<K, V> o) {
        return ComparatorUtil.compare(key, o.key);
    }

    static class WriteResult<K, V> {

        boolean applied;
        K key;
        V value;
        RBHashNode<K, V> node;

        static <K, V> WriteResult<K, V> empty() {
            return new WriteResult<>();
        }

        void update(boolean applied, K key, V value) {
            this.applied = applied;
            this.key = key;
            this.value = value;
        }

        void update(boolean applied) {
            this.applied = applied;
        }
    }

}
