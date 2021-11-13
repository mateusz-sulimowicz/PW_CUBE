package concurrentcube;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CubeTest {

	private Cube cube;

	@BeforeEach
	public void resetCube() {
		cube = new Cube(4, (x, y) -> {},  (x, y) -> {}, () -> {}, () -> {});
	}

	private static final String VALIDATION_EXPECTED =
			"0000"
					+ "0000"
					+ "0000"
					+ "1111"

					+ "1115"
					+ "1115"
					+ "4444"
					+ "1115"

					+ "2222"
					+ "2222"
					+ "1115"
					+ "2222"

					+ "0333"
					+ "0333"
					+ "2222"
					+ "0333"

					+ "4444"
					+ "4444"
					+ "0333"
					+ "4444"

					+ "3333"
					+ "5555"
					+ "5555"
					+ "5555";


	@Test
	public void shouldPassValidation() {
		try {
			cube.rotate(2, 0);
			cube.rotate(5, 1);
			Assertions.assertEquals(cube.show(), VALIDATION_EXPECTED);
		} catch (InterruptedException ignored) {

		}
	}

	public static void main(String[] args) throws InterruptedException {
		Cube cube = new Cube(3, (x, y) -> {},  (x, y) -> {}, () -> {}, () -> {});
		System.out.println(cube.show());
	}
}
