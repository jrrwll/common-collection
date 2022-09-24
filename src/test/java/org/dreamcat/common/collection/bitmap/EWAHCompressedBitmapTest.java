package org.dreamcat.common.collection.bitmap;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import lombok.SneakyThrows;
import org.dreamcat.common.util.ReflectUtil;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

/**
 * @author Jerry Will
 * @version 2022-02-15
 */
class EWAHCompressedBitmapTest {

    @Test
    void logicalWordIndexAndOffset() {
        Consumer<int[]> fn = it -> {
            for (int i : it) {
                System.out.println(i + "\t" + Arrays.toString(logicalWordIndexAndOffset(i)));
            }
        };
        fn.accept(new int[]{
                0, 1, 2, 62, 63,
                64, 65, 66,
                126, 127, 128, 129,
                510, 511, 512, 513});
    }

    @Test
    void wordSpanAndContinuousCount() {
        Consumer<List<int[]>> fn = it -> {
            for (int[] a : it) {
                long rlw = joinRLW(a[0], a[1]);
                int span = wordSpan(rlw);
                int count = wordContinuousCount(rlw);
                System.out.printf("%16d = %12d join %12d\n", rlw, span, count);
            }
        };

        fn.accept(Arrays.asList(
                new int[]{8, 1},
                new int[]{1, 62}
        ));
    }

    @Test
    void set() {
        EWAHCompressedBitmap bitmap = new EWAHCompressedBitmap();
        int[] bits = new int[]{513, 126, 66, 1, 0, 129, 511, 321, 384, 704, 1023};
        for (int bit : bits) {
            bitmap.set(bit);
        }
        for (int bit : bits) {
            System.out.println(bit + "\t" + bitmap.get(1));
        }
    }

    @Disabled
    @Test
    void testSetEdge() {
        EWAHCompressedBitmap bitmap = new EWAHCompressedBitmap();
        for (int i = Integer.MAX_VALUE; i >= 0; i -= 64) {
            bitmap.set(i);
            System.out.printf("%12d \t [%10d %2d] \t "
                            + "size=%12d \t span=%12d count=%d%n", i,
                    logicalWordIndexAndOffset(i)[0], logicalWordIndexAndOffset(i)[1],
                    bitmap.words.length,
                    wordSpan(bitmap.words[0]), wordContinuousCount(bitmap.words[0]));
        }
    }

    @SneakyThrows
    private static int wordSpan(long word) {
        Method wordSpan = ReflectUtil.retrieveMethod(EWAHCompressedBitmap.class,
                "wordSpan", long.class);
        wordSpan.setAccessible(true);
        return (int) wordSpan.invoke(null, word);
    }

    @SneakyThrows
    private static int wordContinuousCount(long word) { //
        Method wordContinuousCount = ReflectUtil.retrieveMethod(EWAHCompressedBitmap.class,
                "wordContinuousCount", long.class);
        wordContinuousCount.setAccessible(true);
        return (int) wordContinuousCount.invoke(null, word);
    }

    @SneakyThrows
    private static long joinRLW(int span, int count) {
        Method joinRLW = ReflectUtil.retrieveMethod(EWAHCompressedBitmap.class,
                "joinRLW", int.class, int.class);
        joinRLW.setAccessible(true);
        return (long) joinRLW.invoke(null, span, count);

    }

    @SneakyThrows
    private static int[] logicalWordIndexAndOffset(int bitIndex) {
        Method logicalWordIndexAndOffset = ReflectUtil.retrieveMethod(EWAHCompressedBitmap.class,
                "logicalWordIndexAndOffset", int.class);
        logicalWordIndexAndOffset.setAccessible(true);
        return (int[]) logicalWordIndexAndOffset.invoke(null, bitIndex);
    }
}
/*
###
513 = 8, 1
126 = 1, 62
66 = 1, 2
1 = 0, 1
0 = 0, 0
129 = 2, 1
511 = 7, 63
321 = 5, 1
384 = 6, 0
704 = 11, 0
1023 = 15, 63
###

# [512,704)
12884901896 = 8,3
[12884901896, 2, 0, 0]

# [64, 127), [512, 704)
4294967297 = 1,1    12884901894 = 6,3
[4294967297, 4611686018427387908, 12884901894, 2, 0, 0]

# [0, 127), [512, 704)
8589934592 = 0,2    12884901894 = 6,3
[8589934592, 3, 4611686018427387908, 12884901894, 2, 0, 0]

# [0, 191), [512, 704)
12884901888 = 0,3   12884901893 = 5,3
[12884901888, 3, 4611686018427387908, 2, 12884901893, 2, 0, 0]

# [0, 191), [448, 704)
12884901888 = 0,3   17179869188 = 4,4
[12884901888, 3, 4611686018427387908, 2, 17179869188, -9223372036854775808, 2, 0, 0]

# [0, 191), [320, 384), [448, 704)
12884901888 = 0,3   4294967298 = 2,1    17179869185 = 1,4
[12884901888, 3, 4611686018427387908, 2, 4294967298, 2, 17179869185, -9223372036854775808, 2, 0, 0]
*/
