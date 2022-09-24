package org.dreamcat.common.collection.heap;

import java.util.AbstractQueue;
import java.util.Iterator;

/**
 * @author Jerry Will
 * @version 2022-06-05
 */
public class FibonacciHeap<E> extends AbstractQueue<E>
        implements java.io.Serializable {

    int size;

    @Override
    public boolean add(E e) {
        return offer(e);
    }

    @Override
    public boolean offer(E e) {
        return false;
    }

    @Override
    public E poll() {
        return null;
    }

    @Override
    public E peek() {

        return null;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public Iterator<E> iterator() {
        return new Itr();
    }

    private final class Itr implements Iterator<E> {

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public E next() {
            return null;
        }
    }

}
