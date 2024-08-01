/**
 * Copyright 2000-2001 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

/* Taken from Oracle Java code, package sun.nio.cs.UTF
 *
 */

package com.solace.labs.aaron;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

public class ReportingCharsetDecoder extends CharsetDecoder {

    // UTF-16 surrogate-character ranges
    //
    public static final char MIN_HIGH = '\uD800';
    public static final char MAX_HIGH = '\uDBFF';
    public static final char MIN_LOW  = '\uDC00';
    public static final char MAX_LOW  = '\uDFFF';
    public static final char MIN = MIN_HIGH;
    public static final char MAX = MAX_LOW;
    
    private static final String replacementPrefix = "<>aa#";
    private int replacementIndex = 0;

    // Range of UCS-4 values that need surrogates in UTF-16
    //
    public static final int UCS4_MIN = 0x10000;
    public static final int UCS4_MAX = (1 << 20) + UCS4_MIN - 1;

    /**
     * Tells whether or not the given UCS-4 character must be represented as a
     * surrogate pair in UTF-16.
     */
    public static boolean neededFor(int uc) {
        return (uc >= UCS4_MIN) && (uc <= UCS4_MAX);
    }

    /**
     * Returns the high UTF-16 surrogate for the given UCS-4 character.
     */
    public static char surrogateHigh(int uc) {
        assert neededFor(uc);
        return (char)(0xd800 | (((uc - UCS4_MIN) >> 10) & 0x3ff));
    }

    /**
     * Returns the low UTF-16 surrogate for the given UCS-4 character.
     */
    public static char surrogateLow(int uc) {
        assert neededFor(uc);
        return (char)(0xdc00 | ((uc - UCS4_MIN) & 0x3ff));
    }
    
    
    // Constructor! ////////////////////////////////////////////////////////
    // averageCharsPerByte - A positive float value indicating the expected number of characters that will be produced for each input byte
    // maxCharsPerByte - A positive float value indicating the maximum number of characters that will be produced for each input byte
    public ReportingCharsetDecoder(Charset cs) {
        super(cs, 1.0f, 7.0f);  // used to be 1.  For 3, due to replacement string: \uFFFD \uFFFD int
        replaceWith(generateReplacement());
        onMalformedInput(CodingErrorAction.REPLACE);
        onUnmappableCharacter(CodingErrorAction.REPLACE);
        
    }
    
    private String generateReplacement() {
        return replacementPrefix+Integer.toString(replacementIndex++,10);
    }

    static final void updatePositions(Buffer src, int sp, Buffer dst, int dp) {
        src.position(sp - src.arrayOffset());
        dst.position(dp - dst.arrayOffset());
    }
    
    private static boolean isNotContinuation(int b) {
        return (b & 0xc0) != 0x80;
    }

    //  [C2..DF] [80..BF]
    private static boolean isMalformed2(int b1, int b2) {
        return (b1 & 0x1e) == 0x0 || (b2 & 0xc0) != 0x80;
    }

    //  [E0]     [A0..BF] [80..BF]
    //  [E1..EF] [80..BF] [80..BF]
    private static boolean isMalformed3(int b1, int b2, int b3) {
        return (b1 == (byte)0xe0 && (b2 & 0xe0) == 0x80) ||
               (b2 & 0xc0) != 0x80 || (b3 & 0xc0) != 0x80;
    }

    //  [F0]     [90..BF] [80..BF] [80..BF]
    //  [F1..F3] [80..BF] [80..BF] [80..BF]
    //  [F4]     [80..8F] [80..BF] [80..BF]
    //  only check 80-be range here, the [0xf0,0x80...] and [0xf4,0x90-...]
    //  will be checked by Surrogate.neededFor(uc)
    private static boolean isMalformed4(int b2, int b3, int b4) {
        return (b2 & 0xc0) != 0x80 || (b3 & 0xc0) != 0x80 ||
               (b4 & 0xc0) != 0x80;
    }

    private static CoderResult lookupN(ByteBuffer src, int n) {
        for (int i=1;i<n;i++) {
           if (isNotContinuation(src.get()))
               return CoderResult.malformedForLength(i);
        }
        return CoderResult.malformedForLength(n);
    }

    private static CoderResult malformedN(ByteBuffer src, int nb) {
        switch (nb) {
        case 1:
            int b1 = src.get();
            if ((b1 >> 2) == -2) {
                // 5 bytes 111110xx 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx
                if (src.remaining() < 4)
                    return CoderResult.UNDERFLOW;
                return lookupN(src, 5);
            }
            if ((b1 >> 1) == -2) {
                // 6 bytes 1111110x 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx
                if (src.remaining() < 5)
                    return CoderResult.UNDERFLOW;
                return lookupN(src, 6);
            }
            return CoderResult.malformedForLength(1);
        case 2:                    // always 1
            return CoderResult.malformedForLength(1);
        case 3:
            b1 = src.get();
            int b2 = src.get();    // no need to lookup b3
            return CoderResult.malformedForLength(
                ((b1 == (byte)0xe0 && (b2 & 0xe0) == 0x80) ||
                 isNotContinuation(b2))?1:2);
        case 4:  // we don't care the speed here
            b1 = src.get() & 0xff;
            b2 = src.get() & 0xff;
            if (b1 > 0xf4 ||
                (b1 == 0xf0 && (b2 < 0x90 || b2 > 0xbf)) ||
                (b1 == 0xf4 && (b2 & 0xf0) != 0x80) ||
                isNotContinuation(b2))
                return CoderResult.malformedForLength(1);
            if (isNotContinuation(src.get()))
                return CoderResult.malformedForLength(2);
            return CoderResult.malformedForLength(3);
        default:
            assert false;
            return null;
        }
    }
    
    // called from decode array loop
    private static CoderResult malformed(ByteBuffer src, int sp, CharBuffer dst, int dp, int nb) {
        src.position(sp - src.arrayOffset());
        CoderResult cr = malformedN(src, nb);
        updatePositions(src, sp, dst, dp);
        return cr;
    }

    // called from decode buffer loop
    private static CoderResult malformed(ByteBuffer src, int mark, int nb) {
        src.position(mark);
        CoderResult cr = malformedN(src, nb);
        src.position(mark);
        return cr;
    }

    private static CoderResult xflow(Buffer src, int sp, int sl, Buffer dst, int dp, int nb) {
        updatePositions(src, sp, dst, dp);
        return (nb == 0 || sl - sp < nb)
               ?CoderResult.UNDERFLOW:CoderResult.OVERFLOW;
    }

    private static CoderResult xflow(Buffer src, int mark, int nb) {
        CoderResult cr = (nb == 0 || src.remaining() < (nb - 1))
                         ?CoderResult.UNDERFLOW:CoderResult.OVERFLOW;
        src.position(mark);
        return cr;
    }

    private CoderResult decodeArrayLoop(ByteBuffer src, CharBuffer dst) {
        // This method is optimized for ASCII input.
        byte[] sa = src.array();
        int sp = src.arrayOffset() + src.position();
        int sl = src.arrayOffset() + src.limit();

        char[] da = dst.array();
        int dp = dst.arrayOffset() + dst.position();
        int dl = dst.arrayOffset() + dst.limit();
        int dlASCII = dp + Math.min(sl - sp, dl - dp);

        // ASCII only loop
        while (dp < dlASCII && sa[sp] >= 0)
            da[dp++] = (char)sa[sp++];

        while (sp < sl) {
            int b1 = sa[sp];
            if (b1  >= 0) {
                // 1 byte, 7 bits: 0xxxxxxx
                if (dp >= dl)
                    return xflow(src, sp, sl, dst, dp, 1);
                da[dp++] = (char)b1;
                sp++;
            } else if ((b1 >> 5) == -2) {
                // 2 bytes, 11 bits: 110xxxxx 10xxxxxx
                if (sl - sp < 2 || dp >= dl)
                    return xflow(src, sp, sl, dst, dp, 2);
                int b2 = sa[sp + 1];
                if (isMalformed2(b1, b2))
                    return malformed(src, sp, dst, dp, 2);
                da[dp++] = (char) (((b1 << 6) ^ b2) ^ 0x0f80);
                sp += 2;
            } else if ((b1 >> 4) == -2) {
                // 3 bytes, 16 bits: 1110xxxx 10xxxxxx 10xxxxxx
                if (sl - sp < 3 || dp >= dl)
                    return xflow(src, sp, sl, dst, dp, 3);
                int b2 = sa[sp + 1];
                int b3 = sa[sp + 2];
                if (isMalformed3(b1, b2, b3))
                    return malformed(src, sp, dst, dp, 3);
                da[dp++] = (char) (((b1 << 12) ^ (b2 << 6) ^ b3) ^ 0x1f80);
                sp += 3;
            } else if ((b1 >> 3) == -2) {
                // 4 bytes, 21 bits: 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
                if (sl - sp < 4 || dl - dp < 2)
                    return xflow(src, sp, sl, dst, dp, 4);
                int b2 = sa[sp + 1];
                int b3 = sa[sp + 2];
                int b4 = sa[sp + 3];
                int uc = ((b1 & 0x07) << 18) |
                         ((b2 & 0x3f) << 12) |
                         ((b3 & 0x3f) << 06) |
                         (b4 & 0x3f);
                if (isMalformed4(b2, b3, b4) ||
                    !neededFor(uc)) {
                    return malformed(src, sp, dst, dp, 4);
                }
                da[dp++] = surrogateHigh(uc);
                da[dp++] = surrogateLow(uc);
                sp += 4;
            } else
                return malformed(src, sp, dst, dp, 1);
        }
        return xflow(src, sp, sl, dst, dp, 0);
    }

    private CoderResult decodeBufferLoop(ByteBuffer src, CharBuffer dst) {
        int mark = src.position();
        int limit = src.limit();
        while (mark < limit) {
            int b1 = src.get();
            if (b1 >= 0) {
                // 1 byte, 7 bits: 0xxxxxxx
                if (dst.remaining() < 1)
                    return xflow(src,mark,1);  //overflow
                dst.put((char)b1);
                mark++;
            } else if ((b1 >> 5) == -2) {
                // 2 bytes, 11 bits: 110xxxxx 10xxxxxx
                if (limit - mark < 2|| dst.remaining() < 1)
                    return xflow(src, mark, 2);
                int b2 = src.get();
                if (isMalformed2(b1,b2))
                    return malformed(src, mark, 2);
                dst.put((char) (((b1 << 6) ^ b2) ^ 0x0f80));
                mark += 2;
            } else if ((b1 >> 4) == -2) {
                // 3 bytes, 16 bits: 1110xxxx 10xxxxxx 10xxxxxx
                if (limit - mark < 3 || dst.remaining() < 1)
                    return xflow(src,mark,3);
                int b2 = src.get();
                int b3 = src.get();
                if (isMalformed3(b1,b2,b3))
                    return malformed(src, mark, 3);
                dst.put((char) (((b1 << 12) ^ (b2 << 6) ^ b3) ^ 0x1f80));
                mark += 3;
            } else if ((b1 >> 3) == -2) {
                // 4 bytes, 21 bits: 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
                if (limit - mark < 4 || dst.remaining() < 2)
                    return xflow(src,mark,4);
                int b2 = src.get();
                int b3 = src.get();
                int b4 = src.get();
                int uc = ((b1 & 0x07) << 18) |
                         ((b2 & 0x3f) << 12) |
                         ((b3 & 0x3f) << 06) |
                         (b4 & 0x3f);
                if (isMalformed4(b2,b3,b4) ||
                    !neededFor(uc)) { // shortest form check
                    return malformed(src, mark, 4);
                }
                dst.put(surrogateHigh(uc));
                dst.put(surrogateLow(uc));
                mark += 4;
            } else {
                return malformed(src,mark,1);
            }
        }
        return xflow(src,mark,0);
    }

    protected CoderResult decodeLoop(ByteBuffer src, CharBuffer dst) {
        CoderResult cr;
        if (src.hasArray() && dst.hasArray()) {
            cr = decodeArrayLoop(src, dst);
        } else {
            cr = decodeBufferLoop(src, dst);
        }
        if (cr.isError()) {
            replaceWith(generateReplacement());
        }
        return cr;
    }
}
