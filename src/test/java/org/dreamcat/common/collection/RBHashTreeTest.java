package org.dreamcat.common.collection;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Create by tuke on 2020/4/19
 */
class RBHashTreeTest {

    @Test
    void testPut() {
        RBHashTree<Integer, String> tree = new RBHashTree<>();
        for (int i = 0; i < 1000; i++) {
            tree.put(i, null);
        }

        List<List<String>> msgs = new ArrayList<>(5);
        tree.levelOrder((k, v, red, level) -> {
            int size = msgs.size();
            if (size < level) {
                msgs.add(new ArrayList<>());
            }
            msgs.get(level - 1).add(k + "(" + (red ? "R" : "B") + ")");
        });

        String s = msgs.stream().map(line -> String.join("\t", line))
                .collect(Collectors.joining("\n"));
        System.out.println(s);
    }

}
