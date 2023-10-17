import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class Main {
    public static void main(String[] args) {
        // The plaintext message
        runDec("Hello, World!");
        runDec("Hello, World1");
        runDec("Hello, World2");

    }

    private static void runDec(String plaintext){
        byte[] plaintextBytes = plaintext.getBytes();
        byte[] seed = getTrulyRandomSeed();
        byte[] pad = generatePad(seed, plaintextBytes.length);
        byte[] ciphertext = xor(plaintextBytes, pad);

        // Display the results
        System.out.println("Plaintext: " + plaintext);
        System.out.println("Seed: " + Arrays.toString(seed));
        System.out.println("Generated Pad: " + Arrays.toString(pad));
        System.out.println("Ciphertext: " + Arrays.toString(ciphertext));
    }

    private static byte[] getTrulyRandomSeed() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] seed = new byte[16];
        secureRandom.nextBytes(seed);
        return seed;
    }

    private static byte[] generatePad(byte[] seed, int length) {
        SecureRandom prng = new SecureRandom(seed);
        byte[] pad = new byte[length];
        prng.nextBytes(pad);
        return pad;
    }

    private static byte[] xor(byte[] a, byte[] b) {
        byte[] result = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = (byte) (a[i] ^ b[i]);
        }
        return result;
    }
}