package com.keenwrite.util;

/**
 * The MurmurHash3 algorithm was created by Austin Appleby and placed in the
 * public domain. This Java port was authored by Yonik Seeley and also placed
 * into the public domain. The author hereby disclaims copyright to this
 * source code.
 * <p>
 * This produces exactly the same hash values as the final C++ version and is
 * thus suitable for producing the same hash values across platforms.
 * <p>
 * The 32-bit x86 version of this hash should be the fastest variant for
 * relatively short keys like ids. Using {@link #hash32} is a
 * good choice for longer strings or returning more than 32 hashed bits.
 * <p>
 * The x86 and x64 versions do not produce the same results because
 * algorithms are optimized for their respective platforms.
 * <p>
 * Code clean-up by White Magic Software, Ltd.
 * </p>
 */
public final class MurmurHash {
  /**
   * Returns the 32-bit x86-optimized hash of the UTF-8 bytes of the String
   * without actually encoding the string to a temporary buffer. This is over
   * twice as fast as hashing the result of {@link String#getBytes()}.
   */
  @SuppressWarnings( "unused" )
  public static int hash32( CharSequence data, int offset, int len, int seed ) {
    final int c1 = 0xcc9e2d51;
    final int c2 = 0x1b873593;

    int h1 = seed;

    int pos = offset;
    int end = offset + len;
    int k1 = 0;
    int k2;
    int shift = 0;
    int bits;
    int nBytes = 0;   // length in UTF8 bytes

    while( pos < end ) {
      int code = data.charAt( pos++ );
      if( code < 0x80 ) {
        k2 = code;
        bits = 8;
      }
      else if( code < 0x800 ) {
        k2 = (0xC0 | (code >> 6))
          | ((0x80 | (code & 0x3F)) << 8);
        bits = 16;
      }
      else if( code < 0xD800 || code > 0xDFFF || pos >= end ) {
        // we check for pos>=end to encode an unpaired surrogate as 3 bytes.
        k2 = (0xE0 | (code >> 12))
          | ((0x80 | ((code >> 6) & 0x3F)) << 8)
          | ((0x80 | (code & 0x3F)) << 16);
        bits = 24;
      }
      else {
        // surrogate pair
        // int utf32 = pos < end ? (int) data.charAt(pos++) : 0;
        int utf32 = data.charAt( pos++ );
        utf32 = ((code - 0xD7C0) << 10) + (utf32 & 0x3FF);
        k2 = (0xff & (0xF0 | (utf32 >> 18)))
          | ((0x80 | ((utf32 >> 12) & 0x3F))) << 8
          | ((0x80 | ((utf32 >> 6) & 0x3F))) << 16
          | (0x80 | (utf32 & 0x3F)) << 24;
        bits = 32;
      }

      k1 |= k2 << shift;

      // int used_bits = 32 - shift;  // how many bits of k2 were used in k1.
      // int unused_bits = bits - used_bits; //  (bits-(32-shift)) ==
      // bits+shift-32  == bits-newshift

      shift += bits;
      if( shift >= 32 ) {
        // mix after we have a complete word

        k1 *= c1;
        k1 = (k1 << 15) | (k1 >>> 17);  // ROTL32(k1,15);
        k1 *= c2;

        h1 ^= k1;
        h1 = (h1 << 13) | (h1 >>> 19);  // ROTL32(h1,13);
        h1 = h1 * 5 + 0xe6546b64;

        shift -= 32;
        // unfortunately, java won't let you shift 32 bits off, so we need to
        // check for 0
        if( shift != 0 ) {
          k1 = k2 >>> (bits - shift);   // bits used == bits - newshift
        }
        else {
          k1 = 0;
        }
        nBytes += 4;
      }

    } // inner

    // handle tail
    if( shift > 0 ) {
      nBytes += shift >> 3;
      k1 *= c1;
      k1 = (k1 << 15) | (k1 >>> 17);  // ROTL32(k1,15);
      k1 *= c2;
      h1 ^= k1;
    }

    // finalization
    h1 ^= nBytes;

    // fmix(h1);
    h1 ^= h1 >>> 16;
    h1 *= 0x85ebca6b;
    h1 ^= h1 >>> 13;
    h1 *= 0xc2b2ae35;
    h1 ^= h1 >>> 16;

    return h1;
  }
}
