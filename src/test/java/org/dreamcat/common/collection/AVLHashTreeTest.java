package org.dreamcat.common.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.dreamcat.common.Timeit;
import org.dreamcat.common.function.ISupplier;
import org.dreamcat.common.util.RandomUtil;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Create by tuke on 2020/4/19
 */
@Disabled
class AVLHashTreeTest {

    @Test
    void testPut() {
        AVLHashTree<Integer, String> tree = new AVLHashTree<>();
        for (int i = 0; i < 32; i++) {
            tree.put(i, i * i + "");
        }

        List<List<String>> msgs = new ArrayList<>(5);
        tree.levelOrder((k, v, level) -> {
            int size = msgs.size();
            if (size < level) {
                msgs.add(new ArrayList<>());
            }
            msgs.get(level - 1).add(k + "");
        });

        String s = msgs.stream().map(line -> String.join("\t", line))
                .collect(Collectors.joining("\n"));
        System.out.println(s);
    }

    @Test
    void testRemove() {
        AVLHashTree<Integer, String> tree = new AVLHashTree<>();
        for (int i = 0; i < 32; i++) {
            tree.put(i, i * i + "");
        }

        for (int i = 16; i < 32; i++) {
            tree.remove(i);
        }

        List<List<String>> msgs = new ArrayList<>(5);
        tree.levelOrder((k, v, level) -> {
            int size = msgs.size();
            if (size < level) {
                msgs.add(new ArrayList<>());
            }
            msgs.get(level - 1).add(k + "");
        });

        String s = msgs.stream().map(line -> String.join("\t", line))
                .collect(Collectors.joining("\n"));
        System.out.println(s);
    }

    @Test
    void testSpeed() {
        System.out.println("\t\tHashMap\t\tAVLTree\t\tAVLHashMap\tRBHashMap");
        for (int i = 1; i < (1 << 20); i *= 2) {
            int finalI = i;
            long[] ts = Timeit.ofActions()
                    .addUnaryAction(
                            HashMap::new,
                            map -> {
                                for (int k = 0; k < finalI; k++) {
                                    map.put(k, k);
                                }
                            })
                    .addUnaryAction(
                            (ISupplier<AVLHashTree<Integer, Integer>, ?>) AVLHashTree::new,
                            tree -> {
                                for (int k = 0; k < finalI; k++) {
                                    tree.put(k, k);
                                }
                            })
                    .addUnaryAction(
                            (ISupplier<AVLHashMap<Integer, Integer>, ?>) AVLHashMap::new,
                            map -> {
                                for (int k = 0; k < finalI; k++) {
                                    map.put(k, k);
                                }
                            })
                    .addUnaryAction(
                            (ISupplier<RBHashTree<Integer, Integer>, ?>) RBHashTree::new,
                            tree -> {
                                for (int k = 0; k < finalI; k++) {
                                    tree.put(k, k);
                                }
                            })
                    .count(32).skip(4).run();

            String line = Arrays.stream(ts).mapToObj(it -> String.format("%6.3fus", it / 1000.))
                    .collect(Collectors.joining("\t"));
            System.out.printf("%6d\t%s\n", i, line);
        }

    }

    @Test
    void testSpeedAsc() {
        System.out.println("\t\tAVLTree\t\t  RBTree\t\tBTree");
        for (int i = 1; i < (1 << 20); i *= 2) {
            int finalI = i;
            long[] ts = Timeit.ofActions()
                    .addUnaryAction(
                            (ISupplier<AVLHashTree<Integer, Integer>, ?>) AVLHashTree::new,
                            tree -> {
                                for (int k = 0; k < finalI; k++) {
                                    tree.put(k, k);
                                }
                            })
                    .addUnaryAction(
                            (ISupplier<RBHashTree<Integer, Integer>, ?>) RBHashTree::new,
                            tree -> {
                                for (int k = 0; k < finalI; k++) {
                                    tree.put(k, k);
                                }
                            })
                    .addUnaryAction(
                            () -> new BTree<>(3, Integer[]::new),
                            tree -> {
                                for (int k = 0; k < finalI; k++) {
                                    tree.put(k);
                                }
                            })
                    .count(32).skip(4).run();

            String line = Arrays.stream(ts).mapToObj(it -> String.format("%6.3fus", it / 1000.))
                    .collect(Collectors.joining("\t"));
            System.out.printf("%6d\t%s\n", i, line);
        }
    }

    @Test
    void testSpeedRandom() {
        System.out.println("\t\tAVLTree\t\t  RBTree\t\tBTree");
        for (int i = 1; i < (1 << 20); i *= 2) {
            int finalI = i;
            long[] ts = Timeit.ofActions()
                    .addUnaryAction(
                            (ISupplier<AVLHashTree<String, Integer>, ?>) AVLHashTree::new,
                            tree -> {
                                for (int k = 0; k < finalI; k++) {
                                    tree.put(RandomUtil.uuid(), k);
                                }
                            })
                    .addUnaryAction(
                            (ISupplier<RBHashTree<String, Integer>, ?>) RBHashTree::new,
                            tree -> {
                                for (int k = 0; k < finalI; k++) {
                                    tree.put(RandomUtil.uuid(), k);
                                }
                            })
                    .addUnaryAction(
                            () -> new BTree<>(3, String[]::new),
                            tree -> {
                                for (int k = 0; k < finalI; k++) {
                                    tree.put(RandomUtil.uuid());

                                }
                                tree.printLevel();
                            })
                    .count(32).skip(4).run();

            String line = Arrays.stream(ts).mapToObj(it -> String.format("%6.3fus", it / 1000.))
                    .collect(Collectors.joining("\t"));
            System.out.printf("%6d\t%s\n", i, line);
        }
    }
}
