package org.dreamcat.common.collection.bitmap;

import org.dreamcat.common.util.ByteUtil;

/**
 * A thread-unsafe impl for compressed bitmap
 * <p>
 * sparse format: rlw / lw = 1
 * 2^32 = 2^26 * 2^6, so the max length words = 2^26 * 2 < max_int
 * full format: rlw / lw --> 0, so the length words = 2^26 + 1
 *
 * @author Jerry Will
 * @version 2022-02-06
 */
public class EWAHCompressedBitmap {

    private static final int MAX_SPARSE_LENGTH = 1 << 27;
    long[] words; // [Running Length Word] : [Literal Word] = 1 : 3

    // ==== ==== ==== ====    ==== ==== ==== ====    ==== ==== ==== ====

    /**
     * Returns the value of the bit with the specified index
     *
     * @param bitIndex the index of the bit to be cleared
     * @return the value of the bit with the specified index
     * @throws IndexOutOfBoundsException if the specified index is negative
     * @see java.util.BitSet#get(int)
     */
    public boolean get(int bitIndex) {
        long flag = getInternal(bitIndex);
        int logicalWordOffset = ByteUtil.highBits(flag);
        if (logicalWordOffset == -1) {
            return false;
        }

        int wordIndex = ByteUtil.lowBits(flag);
        long lw = words[wordIndex];
        return (lw & (1L << logicalWordOffset)) != 0;
    }

    /**
     * get offset and wordIndex
     *
     * @param bitIndex bit index
     * @return high 32 bits: offset in [0, 64); low 32 bits: wordIndex
     */
    private long getInternal(int bitIndex) {
        checkRange(bitIndex);
        if (words == null) return ByteUtil.joinBits(-1, 0); // not found

        int[] logicalWord = logicalWordIndexAndOffset(bitIndex);
        int logicalWordIndex = logicalWord[0], logicalWordOffset = logicalWord[1];

        int wordCount = words.length;
        for (int rlwIndex = 0, span = 0; rlwIndex < wordCount; ) {
            // [0,3] [10,4]    wordIndex = 0-2, 12-15
            long word = words[rlwIndex];
            span += wordSpan(word);
            int continuous = wordContinuousCount(word);
            int nextSpan = span + continuous;

            // logicalWordIndex in [span, nextSpan)
            if (logicalWordIndex < nextSpan) {
                int lwIndex = (rlwIndex + 1) + logicalWordIndex - span;
                return ByteUtil.joinBits(logicalWordOffset, lwIndex);
            }
            // next loop
            rlwIndex += continuous + 1;
            span = nextSpan;
        }
        // not found
        return ByteUtil.joinBits(-1, 0);
    }

    // ==== ==== ==== ====    ==== ==== ==== ====    ==== ==== ==== ====

    /**
     * Sets the bit specified by the index to {@code false}.
     *
     * @param bitIndex the index of the bit to be cleared
     * @throws IndexOutOfBoundsException if the specified index is negative
     * @see java.util.BitSet#clear(int)
     */
    public void clear(int bitIndex) {
        long flag = getInternal(bitIndex);
        int logicalWordOffset = ByteUtil.highBits(flag);
        if (logicalWordOffset == -1) {
            return;
        }

        int wordIndex = ByteUtil.lowBits(flag);
        words[wordIndex] &= ~(1L << logicalWordOffset);
    }

    // ==== ==== ==== ====    ==== ==== ==== ====    ==== ==== ==== ====

    /**
     * Sets the bit at the specified index to the specified value.
     *
     * @param bitIndex a bit index
     * @param value    a boolean value to set
     * @throws IndexOutOfBoundsException if the specified index is negative
     */
    public void set(int bitIndex, boolean value) {
        if (value) {
            set(bitIndex);
        } else {
            clear(bitIndex);
        }
    }

    /**
     * set bit
     *
     * @param bitIndex bit index
     * @see java.util.BitSet#set(int)
     */
    public void set(int bitIndex) {
        checkRange(bitIndex);
        // add initial
        if (words == null) {
            setInitial(bitIndex);
            return;
        }

        int wordCount = words.length;
        int prevWordIndex = -1, wordIndex = -1;
        int span = 0, continuous = 0;
        int start, end = 0, prevEnd = 0;

        for (; ; ) {
            wordIndex += continuous + 1;
            if (wordIndex >= wordCount) break;

            long word = words[wordIndex];
            span = wordSpan(word);
            continuous = wordContinuousCount(word);
            start = prevEnd + (span << 6);
            end = start + (continuous << 6);

            // cap case: bitIndex in [end, nextStart)
            if (bitIndex < start) {
                if (prevWordIndex == -1) {
                    // left edge case: bitIndex in [0, start)
                    setFirst(bitIndex, span, continuous);
                    return;
                }
                setCap(prevWordIndex, wordIndex, bitIndex - prevEnd);
                return;
            }
            // hit case: bitIndex in [nextStart, nextEnd)
            else if (bitIndex < end || end < 0) {
                // end < 0 unsigned int
                setHit(wordIndex, bitIndex - start);
                return;
            }

            // next loop
            prevWordIndex = wordIndex;
            prevEnd = end;
        }

        // right edge case: bitIndex in [lastEnd, oo)
        int wordBitIndex = bitIndex - end;
        setLast(prevWordIndex, span, continuous, wordBitIndex);
    }

    private void setInitial(int bitIndex) {
        int[] logicalWord = logicalWordIndexAndOffset(bitIndex);
        int logicalWordIndex = logicalWord[0], logicalWordOffset = logicalWord[1];

        words = new long[4];
        words[0] = joinRLW(3, logicalWordIndex);
        words[1] = 1L << logicalWordOffset;
    }

    private void setFirst(
            int bitIndex, int span, int continuous) {
        int[] logicalWord = logicalWordIndexAndOffset(bitIndex);
        int logicalWordIndex = logicalWord[0], logicalWordOffset = logicalWord[1];

        // recompute span
        int nextSpan = span - logicalWordIndex - 1;
        // merge next rlw
        if (nextSpan == 0) {
            long[] newWords = new long[words.length + 1]; // grow
            newWords[0] = joinRLW(continuous + 1, logicalWordIndex);
            newWords[1] = 1L << logicalWordOffset; // only one LW
            System.arraycopy(words, 1, newWords, 2, words.length - 1);
            words = newWords;
        }
        // add first
        else {
            words[0] = joinRLW(continuous, nextSpan);
            long[] newWords = new long[words.length + 2]; // grow
            newWords[0] = joinRLW(1, logicalWordIndex); // RLW
            newWords[1] = 1L << logicalWordOffset; // only one LW
            System.arraycopy(words, 0, newWords, 2, words.length);
            words = newWords;
        }
    }

    private void setHit(int wordIndex, int wordBitIndex) {
        int[] logicalWord = logicalWordIndexAndOffset(wordBitIndex);
        int logicalWordIndex = logicalWord[0], logicalWordOffset = logicalWord[1];
        int realWordIndex = wordIndex + logicalWordIndex + 1;
        words[realWordIndex] |= 1L << logicalWordOffset;
    }

    private void setCap(int wordIndex, int nextWordIndex, int wordBitIndex) {
        long word = words[wordIndex];
        int span = wordSpan(word), continuous = wordContinuousCount(word);
        long nextWord = words[nextWordIndex];
        int nextSpan = wordSpan(nextWord), nextContinuous = wordContinuousCount(nextWord);
        int wordCount = words.length;

        int[] logicalWord = logicalWordIndexAndOffset(wordBitIndex);
        int logicalWordIndex = logicalWord[0], logicalWordOffset = logicalWord[1];

        long[] newWords;
        boolean nearLeft = logicalWordIndex == 0, nearRight = logicalWordIndex == nextSpan - 1;
        // join left word and right word
        if (nearLeft && nearRight) {
            words[wordIndex] = joinRLW(continuous + nextContinuous + 1, span);
            words[nextWordIndex] = 1L << logicalWordOffset;
            return;
        }
        // join left word
        else if (nearLeft) {
            newWords = new long[wordCount + 1]; // grow 1
            words[wordIndex] = joinRLW(continuous + 1, span);
            words[nextWordIndex] = joinRLW(nextContinuous, nextSpan - 1);
            newWords[nextWordIndex] = 1L << logicalWordOffset;
            System.arraycopy(words, 0, newWords, 0, nextWordIndex);
            System.arraycopy(words, nextWordIndex, newWords, nextWordIndex + 1, wordCount - nextWordIndex);
        }
        // join right word
        else if (nearRight) {
            newWords = new long[wordCount + 1]; // grow 1
            words[nextWordIndex] = joinRLW(nextContinuous + 1, nextSpan - 1);
            int middle = nextWordIndex + 1;
            newWords[middle] = 1L << logicalWordOffset;
            System.arraycopy(words, 0, newWords, 0, middle);
            System.arraycopy(words, middle, newWords, middle + 1, wordCount - middle);
        }
        // insert into the middle
        else {
            newWords = new long[wordCount + 2]; // grow 2
            words[nextWordIndex] = joinRLW(nextContinuous, nextSpan - logicalWordIndex - 1);
            newWords[nextWordIndex] = joinRLW(1, logicalWordIndex);
            newWords[nextWordIndex + 1] = 1L << logicalWordOffset;
            System.arraycopy(words, 0, newWords, 0, nextWordIndex);
            System.arraycopy(words, nextWordIndex, newWords, nextWordIndex + 2, wordCount - nextWordIndex);
        }
        words = newWords;
    }

    private void setLast(int prevWordIndex, int span, int continuous, int wordBitIndex) {
        int[] logicalWord = logicalWordIndexAndOffset(wordBitIndex);
        int logicalWordIndex = logicalWord[0], logicalWordOffset = logicalWord[1];
        int wordCount = words.length;

        long[] newWords;
        int growCount = expectGrowCount(continuous);
        boolean enoughNear = logicalWordIndex < growCount;
        // grow rlw
        if (enoughNear) {
            int newWordCount = wordCount + growCount;
            newWords = new long[newWordCount];
            words[prevWordIndex] = joinRLW(continuous + growCount, span);
            System.arraycopy(words, 0, newWords, 0, wordCount);
            newWords[wordCount + logicalWordIndex] = 1L << logicalWordOffset;
        }
        // add last
        else {
            newWords = new long[wordCount + 4];
            newWords[wordCount] = joinRLW(3, logicalWordIndex);
            newWords[wordCount + 1] = 1L << logicalWordOffset;
            System.arraycopy(words, 0, newWords, 0, wordCount);
        }
        words = newWords;
    }

    // ---- ---- ---- ----    ---- ---- ---- ----    ---- ---- ---- ----

    /**
     * Sets the bits from the specified {@code fromIndex} (inclusive) to the
     * specified {@code toIndex} (exclusive) to {@code true}.
     *
     * @param fromIndex index of the first bit to be set
     * @param toIndex   index after the last bit to be set
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative,
     *                                   or {@code toIndex} is negative, or {@code fromIndex} is
     *                                   larger than {@code toIndex}
     */
    public void set(int fromIndex, int toIndex) {
        checkRange(fromIndex, toIndex);
        if (fromIndex == toIndex) return;

        throw new RuntimeException("no impl"); // todo impl
    }

    // ==== ==== ==== ====    ==== ==== ==== ====    ==== ==== ==== ====

    private int expectGrowCount(int continuous) {
        int wordCount = words.length;
        int growBase = continuous + 1;
        if (growBase >= (1 << 16)) {
            growBase = growBase >> 1;
        }
        return Math.min(growBase, MAX_SPARSE_LENGTH - wordCount);
    }

    /**
     * compute the word index & offset in logical
     *
     * @param bitIndex bit index
     * @return [logicalWordIndex, logicalWordOffset]
     */
    private static int[] logicalWordIndexAndOffset(int bitIndex) {
        int index = 0;
        while (bitIndex > 63) {
            bitIndex -= 64;
            index++;
        }
        return new int[]{index, bitIndex};
    }

    /**
     * the span width between this word and previous <strong>running length word<strong/>
     *
     * @param word some RLW
     * @return span width
     */
    private static int wordSpan(long word) {
        return ByteUtil.lowBits(word);
    }

    /**
     * how many <strong>literal word</strong> on the next
     *
     * @param word some RLW
     * @return literal words count
     */
    private static int wordContinuousCount(long word) { //
        return ByteUtil.highBits(word);
    }

    /**
     * compute a RLW
     *
     * @param count literal words count
     * @param span  span width
     * @return RLW
     */
    private static long joinRLW(int count, int span) {
        return ByteUtil.joinBits(count, span);
    }

    private void checkRange(int bitIndex) {
        if (bitIndex < 0) throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);
    }

    private static void checkRange(int fromIndex, int toIndex) {
        if (fromIndex < 0)
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
        if (toIndex < 0)
            throw new IndexOutOfBoundsException("toIndex < 0: " + toIndex);
        if (fromIndex > toIndex)
            throw new IndexOutOfBoundsException("fromIndex: " + fromIndex +
                    " > toIndex: " + toIndex);
    }
}