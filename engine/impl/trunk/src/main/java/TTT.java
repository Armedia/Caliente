import java.io.File;

public class TTT {

	private TTT() {
	}

	public static void main(String[] args) throws Exception {
		for (String a : args) {
			File fA = new File(a);
			for (String b : args) {
				File fB = new File(b);

				System.out.printf("A=%s%n", fA);
				System.out.printf("B=%s%n", fB);

				System.out.printf("\tA == B: %s%n", fA.equals(fB));
				System.out.printf("\tA ?= B: %s%n", fA.compareTo(fB));
				System.out.printf("\thA == %08X%n", fA.hashCode());
				System.out.printf("\thB == %08X%n", fB.hashCode());
				System.out.printf("\tCA == %s%n", fA.getCanonicalFile());
				System.out.printf("\tCB == %s%n", fB.getCanonicalFile());
				System.out.printf("\thCA == %08X%n", fA.getCanonicalFile().hashCode());
				System.out.printf("\thCB == %08X%n", fB.getCanonicalFile().hashCode());
			}
		}
	}
}