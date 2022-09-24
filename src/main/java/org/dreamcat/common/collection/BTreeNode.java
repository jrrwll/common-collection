package org.dreamcat.common.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import org.dreamcat.common.Pair;
import org.dreamcat.common.util.ArrayUtil;
import org.dreamcat.common.util.ComparatorUtil;
import org.dreamcat.common.util.ObjectUtil;

/**
 * Create by tuke on 2020/4/24
 */
@AllArgsConstructor
public class BTreeNode<E> implements Iterable<BTreeNode<E>> {

    protected BTreeNode<E> parent;
    // p e p e p
    protected E[] elements;
    // null if no children
    protected BTreeNode<E>[] nodes;
    // B-Tree of order m
    protected int order;
    // index in parent
    protected int index;

    static <E> BTreeNode<E> insert(BTreeNode<E> root, BTreeNode<E> x, E element,
            boolean onlyIfAbsent, Result<E> result) {
        E[] elements = x.elements;

        Pair<Boolean, Integer> writeResult = Pair.empty();
        int i = ArrayUtil.binarySearchMin(elements, element, ComparatorUtil::compare);
        if (writeResult.hasFirst()) {
            // save hit element to result
            if (result != null) result.update(false, elements[i], i);

            if (onlyIfAbsent) {
                return root;
            }
            elements[i] = element;
            return root;
        } else {
            i = writeResult.second();
        }

        BTreeNode<E>[] nodes = x.nodes;
        if (nodes != null) {
            return insert(root, nodes[i], element, onlyIfAbsent, result);
        }
        // success to insert rather than update
        if (result != null) result.update(true);

        int len = elements.length;
        // such as insert 3, i = 2 & elements = [1, 2, 4 ,5, 6]
        E[] newElements = Arrays.copyOf(elements, len + 1);
        // newElements = [1, 2, elem, ...], newNodes = [p1, p2, null, ...]
        newElements[i] = element;
        // newNodes[i] = null;
        if (i < len) {
            // newKeys = [1, 2, elem, 3, 4, 5]
            System.arraycopy(elements, i, newElements, i + 1, len - i);
        }
        x.elements = newElements;
        return balanceInsertion(root, x);
    }

    static <E> BTreeNode<E> delete(BTreeNode<E> root, BTreeNode<E> x, E element, Predicate<E> match,
            Result<E> result) {
        E[] elements = x.elements;

        Pair<Boolean, Integer> writeResult = Pair.empty();
        int i = ArrayUtil.binarySearchMin(elements, element, ComparatorUtil::compare);
        if (writeResult.hasFirst()) {
            if (!match.test(element)) {
                if (result != null) result.update(false, elements[i], i);
                return root;
            }
            // save hit element to result
            if (result != null) result.update(true, elements[i], i);
            return delete(root, x, i);
        } else {
            i = writeResult.second();
        }

        BTreeNode<E>[] nodes = x.nodes;
        if (nodes != null) {
            return delete(root, nodes[i], element, match, result);
        }

        return root;
    }

    static <E> BTreeNode<E> search(BTreeNode<E> root, E element, Result<E> result) {
        if (root == null) return null;
        E[] elements = root.elements;

        Pair<Boolean, Integer> writeResult = Pair.empty();
        int i = ArrayUtil.binarySearchMin(elements, element, ComparatorUtil::compare);
        if (writeResult.hasFirst()) {
            // save hit element to result
            if (result != null) result.update(true, elements[i], i);
            return root;
        } else {
            i = writeResult.second();
        }
        BTreeNode<E> node = root.nodes[i];
        if (node == null) {
            return root;
        }

        return search(node, element, result);
    }

    /// balance for B-Tree
    @SuppressWarnings("unchecked")
    static <E> BTreeNode<E> balanceInsertion(BTreeNode<E> root, BTreeNode<E> x) {
        E[] elements = x.elements;
        int order = x.order;
        int len = elements.length;
        if (len < order) return root;

        BTreeNode<E> p = x.parent;
        BTreeNode<E>[] nodes = x.nodes;
        int index = x.index;

        // need split, such as [1, 2, 3, 4, 5, 6, 7] to [1, 2, 3], 4, [5, 6, 7]
        int halfLen = len / 2;
        E[] leftElements, rightElements;
        E middleElement;

        leftElements = Arrays.copyOf(elements, halfLen);
        rightElements = Arrays.copyOfRange(elements, halfLen + 1, len);
        middleElement = elements[halfLen];

        BTreeNode<E> leftNode = new BTreeNode<>(p, leftElements, null, order, index);
        BTreeNode<E> rightNode = new BTreeNode<>(p, rightElements, null, order, index + 1);
        if (nodes != null) {
            BTreeNode<E>[] leftNodes = Arrays.copyOf(nodes, halfLen + 1);
            BTreeNode<E>[] rightNodes = Arrays.copyOfRange(nodes, halfLen + 1, len + 1);
            // update index
            int rlen = rightNodes.length;
            for (int ri = 0; ri < rlen; ri++) {
                rightNodes[ri].index = ri;
                rightNodes[ri].parent = rightNode;
            }

            int llen = leftNodes.length;
            for (int li = 0; li < llen; li++) {
                leftNodes[li].index = li;
                leftNodes[li].parent = leftNode;
            }

            leftNode.nodes = leftNodes;
            rightNode.nodes = rightNodes;
        }

        if (p == null) {
            E[] rootElements = Arrays.copyOf(elements, 1);
            rootElements[0] = middleElement;

            BTreeNode<E>[] rootNodes = (BTreeNode<E>[]) new BTreeNode[]{leftNode, rightNode};
            BTreeNode<E> newRoot = new BTreeNode<>(null, rootElements, rootNodes, order, 0);
            leftNode.parent = newRoot;
            rightNode.parent = newRoot;

            return newRoot;
        }

        E[] parentElements = p.elements;
        BTreeNode<E>[] parentNodes = p.nodes;
        E[] newParentElements = Arrays.copyOf(parentElements, parentElements.length + 1);
        BTreeNode<E>[] newParentNodes = Arrays.copyOf(parentNodes, parentNodes.length + 1);

        Pair<Boolean, Integer> writeResult = Pair.empty();
        ArrayUtil.binarySearchMin(parentElements, middleElement, ComparatorUtil::compare);
        int ind = writeResult.second();
        newParentElements[ind] = middleElement;
        if (ind < parentElements.length) {
            System.arraycopy(parentElements, ind, newParentElements, ind + 1,
                    newParentElements.length - ind - 1);
        }

        newParentNodes[index] = leftNode;
        newParentNodes[index + 1] = rightNode;
        // if not the last node
        if (index < parentNodes.length - 1) {
            System.arraycopy(parentNodes, index + 1, newParentNodes, index + 2,
                    parentNodes.length - index - 1);
        }

        p.elements = newParentElements;
        p.nodes = newParentNodes;

        return balanceInsertion(root, p);
    }

    static <E> BTreeNode<E> delete(BTreeNode<E> root, BTreeNode<E> x, int ind) {
        BTreeNode<E>[] nodes = x.nodes;

        // not a leaf node
        if (nodes != null) {
            BTreeNode<E> s = nodes[ind];
            while (s.nodes != null) {
                s = s.nodes[s.nodes.length - 1];
            }
            int newInd = s.elements.length - 1;
            E replacement = s.elements[newInd];
            x.elements[ind] = replacement;
            return delete(root, s, newInd);
        }

        E[] elements = x.elements;
        int len = elements.length;

        E[] newElements = Arrays.copyOf(elements, len - 1);
        if (ind < len - 1) {
            System.arraycopy(elements, ind + 1, newElements, ind, len - 1 - ind);
        }
        x.elements = newElements;
        return balanceDeletion(root, x);
    }

    // borrow or merge node
    static <E> BTreeNode<E> balanceDeletion(BTreeNode<E> root, BTreeNode<E> x) {
        // Note that remove empty root, and make its unique child as new root
        if (x == root) {
            if (root.nodes != null && root.nodes.length == 1) {
                BTreeNode<E> newRoot = root.nodes[0];
                root.nullify();
                return newRoot;
            }
            return root;
        }

        E[] elements = x.elements;
        BTreeNode<E>[] nodes = x.nodes;
        int len = elements.length;
        int minChildrenCount = x.minChildrenCount();
        if (len + 1 >= minChildrenCount) return root;

        int index = x.index;
        // borrow from sibling
        BTreeNode<E> p = x.parent;
        BTreeNode<E>[] pns = p.nodes;
        E[] pes = p.elements;
        // if x is p's last child
        if (index == pns.length - 1) {
            BTreeNode<E> s = pns[index - 1];
            E[] ses = s.elements;
            BTreeNode<E>[] sns = s.nodes;
            if (ses.length >= minChildrenCount) {
                // borrow last element of previous sibling
                s.elements = Arrays.copyOf(ses, ses.length - 1);

                E[] newElements = Arrays.copyOf(elements, len + 1);
                System.arraycopy(elements, 0, newElements, 1, len);
                x.elements = newElements;

                // Note that parent's move to x, and sibling's to parent
                x.elements[0] = pes[index - 1];
                pes[index - 1] = ses[ses.length - 1];

                if (nodes != null) {
                    s.nodes = Arrays.copyOf(sns, sns.length - 1);
                    x.nodes = Arrays.copyOf(nodes, nodes.length + 1);
                    System.arraycopy(nodes, 0, x.nodes, 1, nodes.length);

                    sns[sns.length - 1].index = nodes.length;
                    sns[sns.length - 1].parent = x;
                    x.nodes[nodes.length] = sns[sns.length - 1];
                }
                return root;
            }

            // merge with sibling and parent element
            E[] mergedElements = Arrays.copyOf(ses, len + ses.length + 1);
            mergedElements[ses.length] = pes[index - 1];
            System.arraycopy(elements, 0, mergedElements, ses.length + 1, len);

            BTreeNode<E> mergedNode = new BTreeNode<>(p, mergedElements, null, x.order, index - 1);
            if (nodes != null) {
                BTreeNode<E>[] mergedNodes = Arrays.copyOf(sns, nodes.length + sns.length);
                System.arraycopy(nodes, 0, mergedNodes, sns.length, nodes.length);
                for (int mi = 0, nlen = nodes.length + sns.length; mi < nlen; mi++) {
                    mergedNodes[mi].index = mi;
                    mergedNodes[mi].parent = mergedNode;
                }
                mergedNode.nodes = mergedNodes;
            }

            p.nodes = Arrays.copyOf(pns, pns.length - 1);
            // index - 1
            p.nodes[pns.length - 1 - 1] = mergedNode;
            p.elements = Arrays.copyOf(pes, pes.length - 1);

            return balanceDeletion(root, p);
        } else {
            BTreeNode<E> s = pns[index + 1];
            E[] ses = s.elements;
            BTreeNode<E>[] sns = s.nodes;
            if (ses.length >= minChildrenCount) {
                s.elements = Arrays.copyOfRange(ses, 1, ses.length);
                x.elements = Arrays.copyOf(elements, len + 1);
                // Note that parent's move to x, and sibling's to parent
                x.elements[len] = p.elements[index];
                p.elements[index] = ses[0];

                if (nodes != null) {
                    s.nodes = Arrays.copyOfRange(sns, 1, sns.length);
                    // update indexes
                    for (int si = 0, snlen = s.nodes.length; si < snlen; si++) {
                        s.nodes[si].index = si;
                    }
                    x.nodes = Arrays.copyOf(nodes, nodes.length + 1);

                    sns[0].index = nodes.length;
                    sns[0].parent = x;
                    x.nodes[nodes.length] = sns[0];
                }
                return root;
            }

            // merge with sibling and parent element
            E[] mergedElements = Arrays.copyOf(elements, len + ses.length + 1);
            mergedElements[len] = pes[index];
            System.arraycopy(ses, 0, mergedElements, len + 1, ses.length);

            BTreeNode<E> mergedNode = new BTreeNode<>(p, mergedElements, null, x.order, index);
            if (nodes != null) {
                BTreeNode<E>[] mergedNodes = Arrays.copyOf(nodes, nodes.length + sns.length);
                System.arraycopy(sns, 0, mergedNodes, nodes.length, sns.length);
                for (int mi = 0, nlen = nodes.length + sns.length; mi < nlen; mi++) {
                    mergedNodes[mi].index = mi;
                    mergedNodes[mi].parent = mergedNode;
                }
                mergedNode.nodes = mergedNodes;
            }

            p.nodes = Arrays.copyOf(pns, pns.length - 1);
            if (index < pns.length - 2) {
                System.arraycopy(pns, index + 2, p.nodes, index + 1, pns.length - 2 - index);
            }
            p.nodes[index] = mergedNode;
            if (index < p.nodes.length - 1) {
                for (int pi = index + 1, pnlen = p.nodes.length; pi < pnlen; pi++) {
                    p.nodes[pi].index = pi;
                }
            }

            p.elements = Arrays.copyOf(pes, pes.length - 1);
            if (index < pes.length - 1) {
                System.arraycopy(pes, index + 1, p.elements, index, pes.length - 1 - index);
            }

            return balanceDeletion(root, p);
        }
    }

    // 7/8-order B-Tree has at least 4 children
    public int minChildrenCount() {
        // if order is odd
        if ((order & 1) == 1) {
            return order / 2 + 1;
        } else {
            return order / 2;
        }
    }

    public void nullify() {
        parent = null;
        if (ObjectUtil.isNotEmpty(elements)) {
            for (int i = 0, len = elements.length; i < len; i++) {
                elements[i] = null;
            }
        }
        if (ObjectUtil.isNotEmpty(nodes)) {
            for (int i = 0, len = nodes.length; i < len; i++) {
                nodes[i] = null;
            }
        }
    }

    // ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ====

    public void preOrder(Consumer<? super BTreeNode<E>> action) {
        action.accept(this);
        if (nodes != null) {
            for (BTreeNode<E> node : nodes) {
                node.preOrder(action);
            }
        }
    }

    public void postOrder(Consumer<? super BTreeNode<E>> action) {
        if (nodes != null) {
            for (BTreeNode<E> node : nodes) {
                node.postOrder(action);
            }
        }
        action.accept(this);
    }

    public void levelOrder(BiConsumer<? super BTreeNode<E>, Integer> action) {
        levelOrder(Collections.singletonList(this), 1, action);
    }

    // ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ====

    private void levelOrder(
            List<BTreeNode<E>> levelNodes, int level,
            BiConsumer<? super BTreeNode<E>, Integer> action) {
        if (ObjectUtil.isEmpty(levelNodes)) return;

        List<BTreeNode<E>> nextLevelNodes = new ArrayList<>();
        for (BTreeNode<E> node : levelNodes) {
            action.accept(node, level);
            if (node.nodes != null) {
                nextLevelNodes.addAll(Arrays.asList(node.nodes));
            }
        }
        levelOrder(nextLevelNodes, level + 1, action);
    }

    // ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ==== ====

    @Override
    public Iterator<BTreeNode<E>> iterator() {
        return new Iter<>(this);
    }

    protected static class Iter<E> implements Iterator<BTreeNode<E>> {

        private List<BTreeNode<E>> levelNodes;
        private List<BTreeNode<E>> nextLevelNodes;
        private int pos;

        Iter(BTreeNode<E> node) {
            if (node != null) {
                levelNodes = Collections.singletonList(node);
            } else {
                levelNodes = Collections.emptyList();
            }
            nextLevelNodes = new ArrayList<>();
            pos = 0;
        }

        @Override
        public boolean hasNext() {
            return !nextLevelNodes.isEmpty() || pos < levelNodes.size();
        }

        @Override
        public BTreeNode<E> next() {
            if (!hasNext()) throw new NoSuchElementException();

            BTreeNode<E> node = levelNodes.get(pos);
            if (node.nodes != null) {
                nextLevelNodes.addAll(Arrays.asList(node.nodes));
            }
            if (pos == levelNodes.size() - 1) {
                pos = 0;
                levelNodes = nextLevelNodes;
                nextLevelNodes = new ArrayList<>();
            } else {
                pos++;
            }
            return node;
        }
    }

    static class Result<E> {

        boolean applied;
        E element;
        int index;

        static <E> Result<E> empty() {
            return new Result<>();
        }

        void update(boolean applied) {
            this.applied = applied;
        }

        void update(boolean applied, E element, int index) {
            this.applied = applied;
            this.element = element;
            this.index = index;
        }
    }
}

/*
Note that in insert case
----- ----- ----- -----    ----- ----- ----- -----    ----- ----- ----- -----
[0]
----- ----- ----- -----    ----- ----- ----- -----    ----- ----- ----- -----
[0,1]
----- ----- ----- -----    ----- ----- ----- -----    ----- ----- ----- -----
[0,1,2]

   [1]
[0]   [2]
----- ----- ----- -----    ----- ----- ----- -----    ----- ----- ----- -----
   [1]
[0]   [2,3]
----- ----- ----- -----    ----- ----- ----- -----    ----- ----- ----- -----
   [1]
[0]   [2,3,4]

    [1,3]
[0]  [2]  [4]
----- ----- ----- -----    ----- ----- ----- -----    ----- ----- ----- -----
    [1,3]
[0]  [2]  [4,5]
----- ----- ----- -----    ----- ----- ----- -----    ----- ----- ----- -----
    [1,3]
[0]  [2]  [4,5,6]

      [1,3,5]
[0]  [2]  [4] [6]

        [3]
   [1]       [5]
[0]  [2]  [4]  [6]
----- ----- ----- -----    ----- ----- ----- -----    ----- ----- ----- -----
        [3]
   [1]       [5]
[0]  [2]  [4]  [6,7]
----- ----- ----- -----    ----- ----- ----- -----    ----- ----- ----- -----
        [3]
   [1]       [5]
[0]  [2]  [4]  [6,7,8]

        [3]
   [1]        [5,7]
[0]  [2]  [4]  [6] [8]
----- ----- ----- -----    ----- ----- ----- -----    ----- ----- ----- -----
 */

/*
Note that in delete case
----- ----- ----- -----    ----- ----- ----- -----    ----- ----- ----- -----
        [3]
   [1]        [5,7]
[0]  [2]  [4]  [6] [8]
----- ----- ----- -----    ----- ----- ----- -----    ----- ----- ----- -----
        [3]
   [1]        [5,7]
[]   [2]  [4]  [6] [8]

        [3]
  []          [5,7]
[1,2]     [4]  [6] [8]

         [5]
    [3]       [7]
[1,2] [4]  [6]  [8]
----- ----- ----- -----    ----- ----- ----- -----    ----- ----- ----- -----
         [5]
    [3]     [7]
[2] [4]  [6]  [8]
----- ----- ----- -----    ----- ----- ----- -----    ----- ----- ----- -----
        [5]
   [3]       [7]
[]   [4]  [6]  [8]

      [5]
  []      [7]
[3,4]  [6]  [8]

      [5,7]
[3,4]  [6]  [8]
----- ----- ----- -----    ----- ----- ----- -----    ----- ----- ----- -----
      [5,7]
[4]  [6]  [8]
----- ----- ----- -----    ----- ----- ----- -----    ----- ----- ----- -----
   [5,7]
[]  [6]  [8]

    [7]
[5,6]  [8]
----- ----- ----- -----    ----- ----- ----- -----    ----- ----- ----- -----
    [7]
[6]  [8]
----- ----- ----- -----    ----- ----- ----- -----    ----- ----- ----- -----
    [7]
[]    [8]

  []
[7,8]

[7,8]
----- ----- ----- -----    ----- ----- ----- -----    ----- ----- ----- -----
[8]
----- ----- ----- -----    ----- ----- ----- -----    ----- ----- ----- -----
 */

/*
Note that merge with sibling
----- ----- ----- -----    ----- ----- ----- -----    ----- ----- ----- -----
                                       [15]
                 [7]	                                       [23]
        [3]	               [11]	                   [19]	                   [27]
   [1]	    [5]	      [9]        [13]	     [17]	     [21]	     [25]	     [29]
[0]	 [2] [4]  [6]  [8]  [10]  [12]  [14]  [16]  [18]  [20]  [22]  [24]  [26]  [28]  [30, 31]
----- ----- ----- -----    ----- ----- ----- -----    ----- ----- ----- -----
                                       [15]
                 [7]	                                       [23]
        [3]	               [11]	                   [19]	                   [27]
   [1] 	    [5]	      [9]        [13]	     [17]	     [21]	     [25]	     [29]
[]  [2]	 [4]  [6]  [8]  [10]  [12]  [14]  [16]  [18]  [20]  [22]  [24]  [26]  [28]  [30, 31]

                                       [15]
                 [7]	                                       [23]
       [3]	               [11]	                   [19]	                   [27]
   [] 	    [5]	      [9]        [13]	     [17]	     [21]	     [25]	     [29]
[1,2]	 [4]  [6]  [8]  [10]  [12]  [14]  [16]  [18]  [20]  [22]  [24]  [26]  [28]  [30, 31]

                                       [15]
                 [7]	                                       [23]
         []	             [11]	                [19]	                [27]
       [3,5]	    [9]        [13]	     [17]	     [21]	     [25]	     [29]
[1,2]  [4]  [6]  [8]  [10]  [12]  [14]  [16]  [18]  [20]  [22]  [24]  [26]  [28]  [30, 31]

                                       [15]
                     []	                                      [23]
                   [7,11]        	            [19]	                [27]
       [3,5]	    [9]        [13]	     [17]	     [21]	     [25]	     [29]
[1,2]  [4]  [6]  [8]  [10]  [12]  [14]  [16]  [18]  [20]  [22]  [24]  [26]  [28]  [30, 31]

                                              [15,23]
                   [7,11]        	           [19]	                    [27]
       [3,5]	    [9]        [13]	     [17]	     [21]	     [25]	     [29]
[1,2]  [4]  [6]  [8]  [10]  [12]  [14]  [16]  [18]  [20]  [22]  [24]  [26]  [28]  [30, 31]
----- ----- ----- -----    ----- ----- ----- -----    ----- ----- ----- -----
*/

/*
Note that borrow from sibling
----- ----- ----- -----    ----- ----- ----- -----    ----- ----- ----- -----
                    [10,26]
   [7]              [17,20]              [30]
[3]  [8]   [12, 13]  [18]  [23]   [27,28]   [33]
----- ----- ----- -----    ----- ----- ----- -----    ----- ----- ----- -----
                    [10,26]
   [7]              [17,20]              [30]
[]  [8]    [12, 13]  [18]  [23]   [27,28]   [33]

                    [10,26]
   []               [17,20]              [30]
  [7,8]    [12, 13]  [18]  [23]   [27,28]   [33]

                    [17,26]
     [10]            [20]            [30]
[7,8]  [12, 13]  [18]  [23]   [27,28]   [33]
*/

/*
Note that delete the root
----- ----- ----- -----    ----- ----- ----- -----    ----- ----- ----- -----
               [7]
       [3]             [11]
  [1]      [5]     [9]      [13]
[0] [2] [4] [6] [8] [10] [12] [14, 15]
----- ----- ----- -----    ----- ----- ----- -----    ----- ----- ----- -----
               [6]
       [3]             [11]
  [1]      [5]     [9]      [13]
[0] [2] [4] [6] [8] [10] [12] [14, 15]

               [6]
       [3]             [11]
  [1]      [5]     [9]      [13]
[0] [2] [4] [] [8] [10] [12] [14, 15]

               [6]
       [3]             [11]
  [1]      []     [9]      [13]
[0] [2]  [4,5] [8] [10] [12] [14, 15]

               [6]
       []             [11]
    [1,3]         [9]       [13]
[0] [2]  [4,5] [8] [10] [12] [14, 15]

                  []
                [6,11]
    [1,3]        [9]       [13]
[0] [2] [4,5] [8] [10] [12] [14, 15]
----- ----- ----- -----    ----- ----- ----- -----    ----- ----- ----- -----

----- ----- ----- -----    ----- ----- ----- -----    ----- ----- ----- -----

 */
