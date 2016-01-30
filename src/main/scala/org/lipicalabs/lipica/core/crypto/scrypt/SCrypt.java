package org.lipicalabs.lipica.core.crypto.scrypt;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.System.arraycopy;

/**
 * この Scrypt.java の実装は、https://github.com/wg/scrypt プロジェクトから借用しています。
 * This class is borrowed from https://github.com/wg/scrypt Project.
 */

/**
 * Modified version of scrypt implementation at https://github.com/wg/scrypt v1.4.0 release
 *  modifications:
 *  * Consolidated into a single class
 *  * Replaced base64 implementation with Base64Util
 *  * Removed jni implementation/references
 *
 * An implementation of the <a href="http://www.tarsnap.com/scrypt/scrypt.pdf"/>scrypt</a>
 * key derivation function. This class will attempt to load a native library
 * containing the optimized C implementation from
 * <a href="http://www.tarsnap.com/scrypt.html">http://www.tarsnap.com/scrypt.html<a> and
 * fall back to the pure Java version if that fails.
 *
 * @author  Will Glozer
 */
public class SCrypt {
    /**
     * Implementation of the <a href="http://www.tarsnap.com/scrypt/scrypt.pdf"/>scrypt KDF</a>.
     * Calls the native implementation  when the native library was successfully
     * loaded, otherwise calls {@link #scryptJ}.
     *
     * @param passwd    Password.
     * @param salt      Salt.
     * @param N         CPU cost parameter.
     * @param r         Memory cost parameter.
     * @param p         Parallelization parameter.
     * @param dkLen     Intended length of the derived key.
     *
     * @return The derived key.
     *
     * @throws GeneralSecurityException when HMAC_SHA256 is not available.
     */
    public static byte[] scrypt(byte[] passwd, byte[] salt, int N, int r, int p, int dkLen) throws GeneralSecurityException {
        return scryptJ(passwd, salt, N, r, p, dkLen);
    }

    /**
     * Pure Java implementation of the <a href="http://www.tarsnap.com/scrypt/scrypt.pdf"/>scrypt KDF</a>.
     *
     * @param passwd    Password.
     * @param salt      Salt.
     * @param N         CPU cost parameter.
     * @param r         Memory cost parameter.
     * @param p         Parallelization parameter.
     * @param dkLen     Intended length of the derived key.
     *
     * @return The derived key.
     *
     * @throws GeneralSecurityException when HMAC_SHA256 is not available.
     */
    public static byte[] scryptJ(byte[] passwd, byte[] salt, int N, int r, int p, int dkLen) throws GeneralSecurityException {
        if (N < 2 || (N & (N - 1)) != 0) throw new IllegalArgumentException("N must be a power of 2 greater than 1");

        if (N > MAX_VALUE / 128 / r) throw new IllegalArgumentException("Parameter N is too large");
        if (r > MAX_VALUE / 128 / p) throw new IllegalArgumentException("Parameter r is too large");

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(passwd, "HmacSHA256"));

        byte[] DK = new byte[dkLen];

        byte[] B  = new byte[128 * r * p];
        byte[] XY = new byte[256 * r];
        byte[] V  = new byte[128 * r * N];
        int i;

        pbkdf2(mac, salt, 1, B, p * 128 * r);

        for (i = 0; i < p; i++) {
            smix(B, i * 128 * r, r, N, V, XY);
        }

        pbkdf2(mac, B, 1, DK, dkLen);

        return DK;
    }

    public static void smix(byte[] B, int Bi, int r, int N, byte[] V, byte[] XY) {
        int Xi = 0;
        int Yi = 128 * r;
        int i;

        arraycopy(B, Bi, XY, Xi, 128 * r);

        for (i = 0; i < N; i++) {
            arraycopy(XY, Xi, V, i * (128 * r), 128 * r);
            blockmix_salsa8(XY, Xi, Yi, r);
        }

        for (i = 0; i < N; i++) {
            int j = integerify(XY, Xi, r) & (N - 1);
            blockxor(V, j * (128 * r), XY, Xi, 128 * r);
            blockmix_salsa8(XY, Xi, Yi, r);
        }

        arraycopy(XY, Xi, B, Bi, 128 * r);
    }

    public static void blockmix_salsa8(byte[] BY, int Bi, int Yi, int r) {
        byte[] X = new byte[64];
        int i;

        arraycopy(BY, Bi + (2 * r - 1) * 64, X, 0, 64);

        for (i = 0; i < 2 * r; i++) {
            blockxor(BY, i * 64, X, 0, 64);
            salsa20_8(X);
            arraycopy(X, 0, BY, Yi + (i * 64), 64);
        }

        for (i = 0; i < r; i++) {
            arraycopy(BY, Yi + (i * 2) * 64, BY, Bi + (i * 64), 64);
        }

        for (i = 0; i < r; i++) {
            arraycopy(BY, Yi + (i * 2 + 1) * 64, BY, Bi + (i + r) * 64, 64);
        }
    }

    public static int R(int a, int b) {
        return (a << b) | (a >>> (32 - b));
    }

    public static void salsa20_8(byte[] B) {
        int[] B32 = new int[16];
        int[] x   = new int[16];
        int i;

        for (i = 0; i < 16; i++) {
            B32[i]  = (B[i * 4 + 0] & 0xff) << 0;
            B32[i] |= (B[i * 4 + 1] & 0xff) << 8;
            B32[i] |= (B[i * 4 + 2] & 0xff) << 16;
            B32[i] |= (B[i * 4 + 3] & 0xff) << 24;
        }

        arraycopy(B32, 0, x, 0, 16);

        for (i = 8; i > 0; i -= 2) {
            x[ 4] ^= R(x[ 0]+x[12], 7);  x[ 8] ^= R(x[ 4]+x[ 0], 9);
            x[12] ^= R(x[ 8]+x[ 4],13);  x[ 0] ^= R(x[12]+x[ 8],18);
            x[ 9] ^= R(x[ 5]+x[ 1], 7);  x[13] ^= R(x[ 9]+x[ 5], 9);
            x[ 1] ^= R(x[13]+x[ 9],13);  x[ 5] ^= R(x[ 1]+x[13],18);
            x[14] ^= R(x[10]+x[ 6], 7);  x[ 2] ^= R(x[14]+x[10], 9);
            x[ 6] ^= R(x[ 2]+x[14],13);  x[10] ^= R(x[ 6]+x[ 2],18);
            x[ 3] ^= R(x[15]+x[11], 7);  x[ 7] ^= R(x[ 3]+x[15], 9);
            x[11] ^= R(x[ 7]+x[ 3],13);  x[15] ^= R(x[11]+x[ 7],18);
            x[ 1] ^= R(x[ 0]+x[ 3], 7);  x[ 2] ^= R(x[ 1]+x[ 0], 9);
            x[ 3] ^= R(x[ 2]+x[ 1],13);  x[ 0] ^= R(x[ 3]+x[ 2],18);
            x[ 6] ^= R(x[ 5]+x[ 4], 7);  x[ 7] ^= R(x[ 6]+x[ 5], 9);
            x[ 4] ^= R(x[ 7]+x[ 6],13);  x[ 5] ^= R(x[ 4]+x[ 7],18);
            x[11] ^= R(x[10]+x[ 9], 7);  x[ 8] ^= R(x[11]+x[10], 9);
            x[ 9] ^= R(x[ 8]+x[11],13);  x[10] ^= R(x[ 9]+x[ 8],18);
            x[12] ^= R(x[15]+x[14], 7);  x[13] ^= R(x[12]+x[15], 9);
            x[14] ^= R(x[13]+x[12],13);  x[15] ^= R(x[14]+x[13],18);
        }

        for (i = 0; i < 16; ++i) B32[i] = x[i] + B32[i];

        for (i = 0; i < 16; i++) {
            B[i * 4 + 0] = (byte) (B32[i] >> 0  & 0xff);
            B[i * 4 + 1] = (byte) (B32[i] >> 8  & 0xff);
            B[i * 4 + 2] = (byte) (B32[i] >> 16 & 0xff);
            B[i * 4 + 3] = (byte) (B32[i] >> 24 & 0xff);
        }
    }

    public static void blockxor(byte[] S, int Si, byte[] D, int Di, int len) {
        for (int i = 0; i < len; i++) {
            D[Di + i] ^= S[Si + i];
        }
    }

    public static int integerify(byte[] B, int Bi, int r) {
        int n;

        Bi += (2 * r - 1) * 64;

        n  = (B[Bi + 0] & 0xff) << 0;
        n |= (B[Bi + 1] & 0xff) << 8;
        n |= (B[Bi + 2] & 0xff) << 16;
        n |= (B[Bi + 3] & 0xff) << 24;

        return n;
    }

    /**
     * Implementation of PBKDF2 (RFC2898).
     *
     * @param   mac     Pre-initialized {@link javax.crypto.Mac} instance to use.
     * @param   S       Salt.
     * @param   c       Iteration count.
     * @param   DK      Byte array that derived key will be placed in.
     * @param   dkLen   Intended length, in octets, of the derived key.
     *
     * @throws  java.security.GeneralSecurityException
     */
    public static void pbkdf2(Mac mac, byte[] S, int c, byte[] DK, int dkLen) throws GeneralSecurityException {
        int hLen = mac.getMacLength();

        if (dkLen > (Math.pow(2, 32) - 1) * hLen) {
            throw new GeneralSecurityException("Requested key length too long");
        }

        byte[] U      = new byte[hLen];
        byte[] T      = new byte[hLen];
        byte[] block1 = new byte[S.length + 4];

        int l = (int) Math.ceil((double) dkLen / hLen);
        int r = dkLen - (l - 1) * hLen;

        arraycopy(S, 0, block1, 0, S.length);

        for (int i = 1; i <= l; i++) {
            block1[S.length + 0] = (byte) (i >> 24 & 0xff);
            block1[S.length + 1] = (byte) (i >> 16 & 0xff);
            block1[S.length + 2] = (byte) (i >> 8  & 0xff);
            block1[S.length + 3] = (byte) (i >> 0  & 0xff);

            mac.update(block1);
            mac.doFinal(U, 0);
            arraycopy(U, 0, T, 0, hLen);

            for (int j = 1; j < c; j++) {
                mac.update(U);
                mac.doFinal(U, 0);

                for (int k = 0; k < hLen; k++) {
                    T[k] ^= U[k];
                }
            }

            arraycopy(T, 0, DK, (i - 1) * hLen, (i == l ? r : hLen));
        }
    }

//    public static String scrypt(String passwd) {
//        return scrypt(passwd, 16, 16, 16);
//    }

//    /**
//     * Hash the supplied plaintext password and generate output in the format described
//     * Simple {@link SCrypt} interface for hashing passwords using the
//     * <a href="http://www.tarsnap.com/scrypt.html">scrypt</a> key derivation function
//     * and comparing a plain text password to a hashed one. The hashed output is an
//     * extended implementation of the Modular Crypt Format that also includes the scrypt
//     * algorithm parameters.
//     *
//     * Format: <code>$s0$PARAMS$SALT$KEY</code>.
//     *
//     * <dl>
//     * <dd>PARAMS</dd><dt>32-bit hex integer containing log2(N) (16 bits), r (8 bits), and p (8 bits)</dt>
//     * <dd>SALT</dd><dt>base64-encoded salt</dt>
//     * <dd>KEY</dd><dt>base64-encoded derived key</dt>
//     * </dl>
//     *
//     * <code>s0</code> identifies version 0 of the scrypt format, using a 128-bit salt and 256-bit derived key.
//     *
//     * @author  Will Glozer
//     *
//     * @param passwd    Password.
//     * @param N         CPU cost parameter.
//     * @param r         Memory cost parameter.
//     * @param p         Parallelization parameter.
//     *
//     * @return The hashed password.
//     */
//    public static String scrypt(String passwd, int N, int r, int p) {
//        try {
//            byte[] salt = new byte[16];
//            SecureRandom.getInstance("SHA1PRNG").nextBytes(salt);
//
//            byte[] derived = SCrypt.scrypt(passwd.getBytes("UTF-8"), salt, N, r, p, 32);
//
//            String params = Long.toString(log2(N) << 16L | r << 8 | p, 16);
//
//            StringBuilder sb = new StringBuilder((salt.length + derived.length) * 2);
//            sb.append("$s0$").append(params).append('$');
//            sb.append(Base64Util.encodeBytes(salt)).append('$');
//            sb.append(Base64Util.encodeBytes(derived));
//
//            return sb.toString();
//        } catch (UnsupportedEncodingException e) {
//            throw new IllegalStateException("JVM doesn't support UTF-8?");
//        } catch (GeneralSecurityException e) {
//            throw new IllegalStateException("JVM doesn't support SHA1PRNG or HMAC_SHA256?");
//        }
//    }
//
//    /**
//     * Compare the supplied plaintext password to a hashed password.
//     *
//     * @param   passwd  Plaintext password.
//     * @param   hashed  scrypt hashed password.
//     *
//     * @return true if passwd matches hashed value.
//     */
//    public static boolean check(String passwd, String hashed) {
//        try {
//            String[] parts = hashed.split("\\$");
//
//            if (parts.length != 5 || !parts[1].equals("s0")) {
//                throw new IllegalArgumentException("Invalid hashed value");
//            }
//
//            long params = Long.parseLong(parts[2], 16);
//            byte[] salt = Base64Util.decode(parts[3]);
//            byte[] derived0 = Base64Util.decode(parts[4]);
//
//            int N = (int) Math.pow(2, params >> 16 & 0xffff);
//            int r = (int) params >> 8 & 0xff;
//            int p = (int) params      & 0xff;
//
//            byte[] derived1 = SCrypt.scrypt(passwd.getBytes("UTF-8"), salt, N, r, p, 32);
//
//            if (derived0.length != derived1.length) return false;
//
//            int result = 0;
//            for (int i = 0; i < derived0.length; i++) {
//                result |= derived0[i] ^ derived1[i];
//            }
//            return result == 0;
//        } catch (UnsupportedEncodingException e) {
//            throw new IllegalStateException("JVM doesn't support UTF-8?");
//        } catch (GeneralSecurityException e) {
//            throw new IllegalStateException("JVM doesn't support SHA1PRNG or HMAC_SHA256?");
//        } catch (IOException e) {
//            throw new IllegalStateException("Issue decoding base64 hash: " + e.getMessage());
//        }
//    }

    private static int log2(int n) {
        int log = 0;
        if ((n & 0xffff0000 ) != 0) { n >>>= 16; log = 16; }
        if (n >= 256) { n >>>= 8; log += 8; }
        if (n >= 16 ) { n >>>= 4; log += 4; }
        if (n >= 4  ) { n >>>= 2; log += 2; }
        return log + (n >>> 1);
    }
}