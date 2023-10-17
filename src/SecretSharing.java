import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;

public class SecretSharing {
	private static final BigInteger field = new BigInteger("8CF83642A709A097B447997640129DA299B1A47D1EB3750BA308B0FE64F5FBD3", 16);
	private static final SecureRandom rndGenerator = new SecureRandom();

	public static void main(String[] args) {
		int polyDegree = 1;		// = threshold - 1
		int nShareholders = 4;	// must be > than polyDegree

		// random secret for testing purposes; secret should be less than field
		BigInteger secret = new BigInteger(field.bitLength() - 1, rndGenerator);
		System.out.printf("Secret: %s\n", secret);

		// calculating shares
		Share[] shares = share(polyDegree, nShareholders, secret);

		System.out.println("Shares (shareholder, share):");
		for (Share share : shares) {
			System.out.printf("\t(%s, %s)\n", share.getShareholder(), share.getShare());
		}

		// combining shares
		BigInteger recoveredSecret1 = combine(shares); // reconstruction with all shares
		BigInteger recoveredSecret2 = combine(Arrays.copyOfRange(shares, 1, 3) ); // reconstruction with a threshold nº of shares

		System.out.printf("Recovered secret1: %s\n", recoveredSecret1);
		System.out.printf("Recovered secret2: %s\n", recoveredSecret2);

		if (!secret.equals(recoveredSecret1) || !secret.equals(recoveredSecret2))
			throw new IllegalStateException("Something went wrong... the recovered secrets are different!!!");
	}

	/**
	 * This method shares a secret using Shamir's scheme.
	 * @param polyDegree Degree of the polynomial.
	 * @param nShareholders Number of shareholders.
	 * @param secret Secret to share.
	 * @return Shares of the secret.
	 */
	private static Share[] share(int polyDegree, int nShareholders, BigInteger secret) {
		// creating polynomial: P(x) = a_d * x^d + ... + a_1 * x^1 + secret
		BigInteger[] polynomial = new BigInteger[polyDegree + 1];

		// todo
		for (int i=0; i < polyDegree; i++)
			polynomial[i] = new BigInteger(field.bitLength() - 1 , rndGenerator);
		polynomial[polyDegree] = secret;


		// calculating shares
		Share[] shares = new Share[nShareholders];
		for (int i = 0; i < nShareholders; i++) {
			BigInteger shareholder = BigInteger.valueOf(i + 1); // shareholder id can be any positive number, except 0
			BigInteger share = calculatePoint(shareholder, polynomial);
			shares[i] = new Share(shareholder, share);
		}
		return shares;
	}

	/**
	 * This method combines shares, using Lagrange polynomials, to recover the secret.
	 * 	Lagrange polynomials: https://en.wikipedia.org/wiki/Lagrange_polynomial.
	 * @param shares Shares of the secret.
	 * @return Recovered secret.
	 */
	private static BigInteger combine(Share[] shares) {
		// todo
		BigInteger x = BigInteger.ZERO;
		BigInteger y = BigInteger.ZERO;
		BigInteger y1, x1, x2, diff1, diff2;

		for (Share share1 : shares) {
			x1 = share1.getShareholder();
			y1 = share1.getShare();

			for (Share share2 : shares) {
				x2 = share2.getShareholder();

				if (x1.equals(x2)) continue;

				diff1 = x.subtract(x2);
				diff2 = x1.subtract(x2);
				y1 = y1.multiply(diff1).divide(diff2);
			}
			y = y.add(y1);
		}
		return y;
	}


	/**
	 * This method calculates a point on a polynomial using the Horner's method:
	 * 	https://en.wikipedia.org/wiki/Horner%27s_method.
	 * @param x X value.
	 * @param polynomial Polynomial P(x).
	 * @return Y value.
	 */
	private static BigInteger calculatePoint(BigInteger x, BigInteger[] polynomial) {
		// todo
		BigInteger result = polynomial[0];
		for (int i=1; i<polynomial.length; i++)
			result = result.multiply(x).add(polynomial[i]);
		return result;
	}

	private static class Share {
		private final BigInteger shareholder;
		private final BigInteger share;

		private Share(BigInteger shareholder, BigInteger share) {
			this.shareholder = shareholder;
			this.share = share;
		}

		public BigInteger getShare() {
			return share;
		}

		public BigInteger getShareholder() {
			return shareholder;
		}
	}
}