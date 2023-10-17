import java.math.BigInteger;
import java.security.SecureRandom;

public class ElGamal {
    private static final SecureRandom rndGenerator = new SecureRandom();

    //public parameters:

    //secure group for production purposes - see https://www.rfc-editor.org/rfc/rfc5114#section-2.3
  /*  private static final BigInteger q = new BigInteger("8CF83642A709A097B447997640129DA299B1A47D1EB3750BA308B0FE64F5FBD3", 16);
    private static final BigInteger p = new BigInteger("87A8E61DB4B6663CFFBBD19C651959998CEEF608660DD0F25D2CEED4435E3B00E00DF8F1D61957D4FAF7DF4561B2AA3016C3D91134096FAA3BF4296D830E9A7C209E0C6497517ABD5A8A9D306BCF67ED91F9E6725B4758C022E0B1EF4275BF7B6C5BFC11D45F9088B941F54EB1E59BB8BC39A0BF12307F5C4FDB70C581B23F76B63ACAE1CAA6B7902D52526735488A0EF13C6D9A51BFA4AB3AD8347796524D8EF6A167B5A41825D967E144E5140564251CCACB83E6B486F6B3CA3F7971506026C0B857F689962856DED4010ABD0BE621C3A3960A54E710C375F26375D7014103A4B54330C198AF126116D2276E11715F693877FAD7EF09CADB094AE91E1A1597", 16);
    private static final BigInteger g = new BigInteger("3FB32C9B73134D0B2E77506660EDBD484CA7B18F21EF205407F4793A1A0BA12510DBC15077BE463FFF4FED4AAC0BB555BE3A6C1B0C6B47B1BC3773BF7E8C6F62901228F8C28CBB18A55AE31341000A650196F931C77A57F2DDF463E5E9EC144B777DE62AAAB8A8628AC376D282D6ED3864E67982428EBC831D14348F6F2F9193B5045AF2767164E1DFC967C1FB3F2E55A4BD1BFFE83B9C80D052B985D182EA0ADB2A3B7313D3FE14C8484B1E052588B9B7D2BBD2DF016199ECD06E1557CD0915B3353BBB64E0EC377FD028370DF92B52C7891428CDC67EB6184B523D1DB246C32F63078490F00EF8D647D148D47954515E2327CFEF98C582664B4C0F6CC41659", 16);
*/
    //small group for testing purposes only
	private static final BigInteger q = new BigInteger("83");
	private static final BigInteger p = q.multiply(BigInteger.TWO).add(BigInteger.ONE);
	private static final BigInteger g = new BigInteger("4");
	private static final BigInteger x = new BigInteger("37");

    //public and private keys:


  //  private static final BigInteger x = new BigInteger(q.bitLength() - 1, rndGenerator);// the private key sk = random value < q
    private static final BigInteger h = g.modPow(x, p);    // the public key pk = g^x mod p


    public static void main(String[] args) {
        testEncDec();
        testHom();
    }

    private static void testEncDec() {
        System.out.println("========");
        System.out.println("Test Encryption and Decryption");
        System.out.println("========");

        BigInteger m1 = new BigInteger(q.bitLength() - 1, rndGenerator);

        Ciphertext c = encrypt(m1);
        BigInteger m2 = decrypt(c);

        System.out.println("original message: " + m1);
        System.out.println("ciphertext1: " + c.c1);
        System.out.println("ciphertext2: " + c.c2);
        System.out.println("decrypted message: " + m2);
        System.out.println();
    }

    private static void testHom() {
        System.out.println("========");
        System.out.println("Test Homomorphic Operation");
        System.out.println("========");
        BigInteger m1 = BigInteger.valueOf(9);
        BigInteger m2 = BigInteger.valueOf(8);

        Ciphertext cipher1 = encrypt(m1);
        Ciphertext cipher2 = encrypt(m2);
        Ciphertext cipher_times = eval(cipher1, cipher2);

        BigInteger m_times = decrypt(cipher_times);

        System.out.println("plaintext message 1: " + m1);
        System.out.println("plaintext message 2: " + m2);
        System.out.println("plaintext multiplication: " + m1.multiply(m2));
        System.out.println("decryption of the multiplied ciphertexts: " + m_times);
        System.out.println();
    }

    /**
     * Encrypts a message m with the public key h, using a random value y.
     *
     * @param m the message
     * @return ciphertexts c1 = g^y mod p and c2 = ( ( x^h mod p ) . m ) mod p
     */
    private static Ciphertext encrypt(BigInteger m) {
        BigInteger y = new BigInteger(q.bitLength() - 1, rndGenerator);
        BigInteger c1 = g.modPow(y, p);
        BigInteger c2 = h.modPow(y, p).multiply(m);
        return new Ciphertext(c1, c2);
    }

    /**
     * Decrypts ciphertext c with the private key x.
     *
     * @param c the ciphertext
     * @return plaintext m = c2 / c1^x = ( c2 . (c1^x mod p)^-1 mod p ) mod p
     */
    private static BigInteger decrypt(Ciphertext c) {
        BigInteger c1 = c.c1;
        BigInteger c2 = c.c2;
        return c2.divide(c1.modPow(x, p));
    }

    /**
     * Performs the homomorphic operation of this scheme, i.e., multiplies the
     * ciphertexts which results in the multiplication of the plaintexts.
     *
     * @param cipher1
     * @param cipher2
     * @return ciphertext c = ( cipher1 . cipher2 ) mod p
     */
    private static Ciphertext eval(Ciphertext cipher1, Ciphertext cipher2) {
        return new Ciphertext(cipher1.c1.multiply(cipher2.c1).mod(p), cipher1.c2.multiply(cipher2.c2).mod(p));
    }


    private static class Ciphertext {
        private final BigInteger c1;
        private final BigInteger c2;

        private Ciphertext(BigInteger c1, BigInteger c2) {
            this.c1 = c1;
            this.c2 = c2;
        }
    }

}
