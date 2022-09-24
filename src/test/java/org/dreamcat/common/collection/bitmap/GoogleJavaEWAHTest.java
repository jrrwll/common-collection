package org.dreamcat.common.collection.bitmap;

import com.googlecode.javaewah.EWAHCompressedBitmap;
import org.junit.jupiter.api.Test;

/**
 * @author Jerry Will
 * @version 2022-02-17
 */
class GoogleJavaEWAHTest {

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
}
