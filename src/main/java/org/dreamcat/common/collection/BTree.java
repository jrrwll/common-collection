package org.dreamcat.common.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.dreamcat.common.collection.BTreeNode.Result;

/**
 * Create by tuke on 2020/4/25
 */
@RequiredArgsConstructor
public class BTree<E> {

    private final int order;
    private final IntFunction<E[]> arrGen;
    private BTreeNode<E> root;
    private int size;

    public BTreeNode<E> getRoot() {
        return root;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean contains(E element) {
        if (isEmpty()) return false;

        Result<E> result = Result.empty();
        BTreeNode.search(root, element, result);
        return result.applied;
    }

    public E get(E element) {
        if (isEmpty()) return null;

        Result<E> result = Result.empty();
        BTreeNode.search(root, element, result);
        return result.applied ? result.element : null;
    }

    public E put(E element) {
        if (isEmpty()) {
            E[] elements = arrGen.apply(1);
            elements[0] = element;
            root = new BTreeNode<>(null, elements, null, order, 0);
            size++;
            return null;
        }
        Result<E> result = Result.empty();
        root = BTreeNode.insert(root, root, element, false, result);
        E oldValue = result.element;
        if (result.applied) size++;
        return oldValue;
    }

    public E remove(final E element) {
        if (isEmpty()) return null;
        Result<E> result = Result.empty();
        root = BTreeNode.delete(root, root, element, it -> it == element, result);
        E oldValue = result.element;
        if (result.applied) size--;
        return oldValue;
    }

    public void clear() {
        while (root != null && root.elements != null && root.elements.length > 0) {
            remove(root.elements[0]);
        }
    }

    // ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ====

    public void preOrder(Consumer<? super E> action) {
        if (isEmpty()) return;

        getRoot().preOrder(node -> {
            E[] elements = node.elements;
            for (E e : elements) {
                action.accept(e);
            }
        });
    }

    public void postOrder(Consumer<? super E> action) {
        if (isEmpty()) return;

        getRoot().postOrder(node -> {
            E[] elements = node.elements;
            for (E e : elements) {
                action.accept(e);
            }
        });
    }

    public void levelOrder(BiConsumer<? super E, Integer> action) {
        if (isEmpty()) return;

        getRoot().levelOrder((node, level) -> {
            E[] elements = node.elements;
            for (E e : elements) {
                action.accept(e, level);
            }
        });
    }

    // ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ====

    public void printLevel() {
        if (isEmpty()) {
            System.out.println("[]");
            return;
        }

        List<List<String>> lines = new ArrayList<>(5);
        root.levelOrder((node, level) -> {
            E[] elements = node.elements;
            int size = lines.size();
            if (size < level) {
                lines.add(new ArrayList<>());
            }
            lines.get(level - 1).add(Arrays.deepToString(elements));
        });

        String s = lines.stream().map(line -> String.join(" ", line))
                .collect(Collectors.joining("\n"));
        System.out.println(s);
    }
}
