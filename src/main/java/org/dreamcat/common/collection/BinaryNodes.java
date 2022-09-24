package org.dreamcat.common.collection;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;
import org.dreamcat.common.Pair;

/**
 * Create by tuke on 2020/5/29
 */
public final class BinaryNodes {

    private BinaryNodes() {
    }

    public static <N extends BinaryNode<N>> void preOrder(
            N node, Consumer<? super N> action) {
        LinkedList<N> stack = new LinkedList<>();
        N current = node;
        while (current != null || !stack.isEmpty()) {
            if (current == null) {
                current = stack.removeFirst().right;
                continue;
            }

            action.accept(current);
            stack.addFirst(current);
            current = current.left;
        }
    }

    public static <N extends BinaryNode<N>> void levelOrder(
            N node, ObjIntConsumer<? super N> action) {
        LinkedList<Pair<N, Integer>> queue = new LinkedList<>();
        queue.addLast(new Pair<>(node, 1));

        N current;
        while (!queue.isEmpty()) {
            Pair<N, Integer> pair = queue.removeFirst();
            current = pair.first();
            int level = pair.second();
            action.accept(current, level);

            if (current.left != null) {
                queue.addLast(new Pair<>(current.left, level + 1));
            }
            if (current.right != null) {
                queue.addLast(new Pair<>(current.right, level + 1));
            }
        }
    }

}
