package org.dreamcat.common.collection;

/**
 * Create by tuke on 2020/4/18
 */
public abstract class AVLNode<Node extends AVLNode<Node>> extends BinaryNode<Node> {

    protected int height;

    ///  balance

    protected static <Node extends AVLNode<Node>> Node balance(Node node) {
        int diff = getHeight(node.left) - getHeight(node.right);
        if (diff == 2) {
            if (getHeight(node.left.left) >= getHeight(node.left.right)) {
                node = rotateLeft(node);
            } else {
                node = doubleRotateLeftRight(node);
            }
        } else if (diff == -2) {
            if (getHeight(node.right.right) >= getHeight(node.right.left)) {
                node = rotateRighr(node);
            } else {
                node = doubleRotateRightLeft(node);
            }
        }

        node.height = updateHeight(node);
        return node;
    }

    /*
    Left-Left Rotation, when Dx > y
    ---- ---- ---- ----    ---- ---- ---- ----    ---- ---- ---- ----
                k2
        k1				z
    Dx		y
    ---- ---- ---- ----    ---- ---- ---- ----    ---- ---- ---- ----
                k1
        Dx				k2
                    y		z
    ---- ---- ---- ----    ---- ---- ---- ----    ---- ---- ---- ----
     */
    private static <Node extends AVLNode<Node>> Node rotateLeft(Node k2) {
        Node k1 = k2.left;
        k2.left = k1.right;
        k1.right = k2;

        k2.height = updateHeight(k2);
        k1.height = updateHeight(k1);
        return k1;
    }

    /*
    Right-Right Rotation, when y < Dz
    ---- ---- ---- ----    ---- ---- ---- ----    ---- ---- ---- ----
                k1
        x				k2
                    y		Dz
    ---- ---- ---- ----    ---- ---- ---- ----    ---- ---- ---- ----
                k2
        k1				Dz
    x		y
    ---- ---- ---- ----    ---- ---- ---- ----    ---- ---- ---- ----
     */
    private static <Node extends AVLNode<Node>> Node rotateRighr(Node k1) {
        Node k2 = k1.right;
        k1.right = k2.left;
        k2.left = k1;

        k1.height = updateHeight(k1);
        k2.height = updateHeight(k2);
        return k2;
    }

    /*
    Left-Right Rotation
    ---- ---- ---- ----    ---- ---- ---- ----    ---- ---- ---- ----
                        k3
             k1					  d
    a				k2
                  b   c
    ---- ---- ---- ----    ---- ---- ---- ----    ---- ---- ---- ----
                        k3
             k2					  d
       k1		   c
    a     b
    ---- ---- ---- ----    ---- ---- ---- ----    ---- ---- ---- ----
                        k2
             k1					  k3
          a		 b			   c	  d
    ---- ---- ---- ----    ---- ---- ---- ----    ---- ---- ---- ----
     */
    private static <Node extends AVLNode<Node>> Node doubleRotateLeftRight(Node k3) {
        // first rotateRR k1
        k3.left = rotateRighr(k3.left);
        // then rotateLL k3
        return rotateLeft(k3);
    }

    /*
    Right-Left Rotation
    ---- ---- ---- ----    ---- ---- ---- ----    ---- ---- ---- ----
                        k1
             a					  k3
                            k2          d
                          b    c
    ---- ---- ---- ----    ---- ---- ---- ----    ---- ---- ---- ----
                        k1
             a					  k2
                              b    		k3
                                      c    d
    ---- ---- ---- ----    ---- ---- ---- ----    ---- ---- ---- ----
                        k2
             k1					  k3
          a		 b			   c	  d
    ---- ---- ---- ----    ---- ---- ---- ----    ---- ---- ---- ----
     */
    private static <Node extends AVLNode<Node>> Node doubleRotateRightLeft(Node k1) {
        // first rotateRR k1
        k1.right = rotateLeft(k1.right);
        // then rotateLL k1
        return rotateRighr(k1);
    }

    /// static utils

    private static <Node extends AVLNode<Node>> int updateHeight(Node node) {
        return Math.max(getHeight(node.left), getHeight(node.right)) + 1;
    }

    private static <Node extends AVLNode<Node>> int getHeight(Node node) {
        return node != null ? node.height : 0;
    }
}
