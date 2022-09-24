package org.dreamcat.common.collection;

import org.junit.jupiter.api.Test;

/**
 * Create by tuke on 2020/4/4
 */
class SkipListTest {

    /**
     * 0.01	212
     * 0.05	138
     * 0.10	106
     * 0.15	87
     * 0.20	74
     * 0.25	63
     * 0.30	55
     * 0.35	48
     * 0.40	42
     * 0.45	36
     * 0.50	31
     * 0.55	27
     * 0.60	23
     * 0.65	19
     * 0.70	16
     * 0.72	15
     * 0.73	14
     * 0.75	13
     * 0.77	12
     * 0.78	11
     * 0.80	10
     * 0.82	9
     * 0.84	8
     * 0.85	7
     * 0.87	6
     * 0.89	5
     * 0.90	4
     * 0.93	3
     * 0.95	2
     * 0.97	1
     */
    @Test
    void testProb() {
        for (int i = 1; i <= 99; i++) {
            float prob = (float) (i * 0.01);
            System.out.printf("%.2f\t%d\n", prob, SkipList.maxLevel(prob));
        }
    }

    @Test
    void test() {
        SkipList<Integer, Integer> list = new SkipList<>(0.25);
        for (int i = 0; i < 100; i++) {
            list.put(i, i);
        }

        System.out.println(list.prettyToString(2));

        System.out.println("size" + list.size());
        for (int i = 0; i < 100; i++) {
            System.out.println(i + " " + list.get(i));
        }
    }
}
