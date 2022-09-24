package org.dreamcat.common.collection;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.ObjIntConsumer;

/**
 * Create by tuke on 2020/4/18
 */
@SuppressWarnings("unchecked")
public abstract class BinaryNode<N extends BinaryNode<N>> implements Iterable<N> {

    protected N left;
    protected N right;

    /// for-each methods

    public void preOrder(Consumer<? super N> action) {
        action.accept((N) this);
        if (left != null) {
            left.preOrder(action);
        }
        if (right != null) {
            right.preOrder(action);
        }
    }

    public void inOrder(Consumer<? super N> action) {
        if (left != null) {
            left.inOrder(action);
        }
        action.accept((N) this);
        if (right != null) {
            right.inOrder(action);
        }
    }

    public void postOrder(Consumer<? super N> action) {
        if (left != null) {
            left.postOrder(action);
        }
        if (right != null) {
            right.postOrder(action);
        }
        action.accept((N) this);
    }

    public void levelOrder(ObjIntConsumer<? super N> action) {
        BinaryNodes.levelOrder((N) this, action);
    }

    // level order iterator

    @Override
    public Iterator<N> iterator() {
        return new Iter<>((N) this);
    }

    protected static class Iter<N extends BinaryNode<N>> implements Iterator<N> {

        private final LinkedList<N> levelNodes;

        Iter(N node) {
            levelNodes = new LinkedList<>();
            if (node != null) {
                levelNodes.addLast(node);
            }
        }

        @Override
        public boolean hasNext() {
            return !levelNodes.isEmpty();
        }

        @Override
        public N next() {
            if (!hasNext()) throw new NoSuchElementException();

            N node = levelNodes.removeFirst();
            if (node.left != null) levelNodes.addLast(node.left);
            if (node.right != null) levelNodes.addLast(node.right);
            return node;
        }
    }

}
