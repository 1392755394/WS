

public class testAction {

	int z = 1;

	testAction(int z) {
		assert false;
		z = 2;
		System.out.println(z);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		int z = 0;
		testAction testActions = new testAction(z);


		System.out.println("3");

	}

}
