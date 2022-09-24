package org.dreamcat.common.collection;

/**
 * Create by tuke on 2020/4/25
 */
public interface HashNode<K, V> {

    K getKey();

    V getValue();

    V setValue(V newValue);
}
