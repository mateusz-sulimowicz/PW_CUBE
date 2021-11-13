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
	private static final String SOLVED_EXPECTED =
			"0000"
					+ "0000"
					+ "0000"
					+ "0000"

					+ "1111"
					+ "1111"
					+ "1111"
					+ "1111"

					+ "2222"
					+ "2222"
					+ "2222"
					+ "2222"

					+ "3333"
					+ "3333"
					+ "3333"
					+ "3333"

					+ "4444"
					+ "4444"
					+ "4444"
					+ "4444"

					+ "5555"
					+ "5555"
					+ "5555"
					+ "5555";

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

	private static final String LEFT1_EXPECTED =
			"0400"
					+ "0400"
					+ "0400"
					+ "0400"

					+ "1111"
					+ "1111"
					+ "1111"
					+ "1111"

					+ "2022"
					+ "2022"
					+ "2022"
					+ "2022"

					+ "3333"
					+ "3333"
					+ "3333"
					+ "3333"

					+ "4454"
					+ "4454"
					+ "4454"
					+ "4454"

					+ "5255"
					+ "5255"
					+ "5255"
					+ "5255";

	@Test
	public void shouldRotateLeft1Sequential() {
		try {
			cube.rotate(1, 1);
			Assertions.assertEquals(cube.show(), LEFT1_EXPECTED);
		} catch (InterruptedException ignored) {
		}
	}

	private static final String BACK2_EXPECTED =
			"0000"
					+ "0000"
					+ "3333"
					+ "0000"

					+ "1101"
					+ "1101"
					+ "1101"
					+ "1101"

					+ "2222"
					+ "2222"
					+ "2222"
					+ "2222"

					+ "3533"
					+ "3533"
					+ "3533"
					+ "3533"

					+ "4444"
					+ "4444"
					+ "4444"
					+ "4444"

					+ "5555"
					+ "1111"
					+ "5555"
					+ "5555";


	@Test
	public void shouldRotateBack2Sequential() {
		try {
			cube.rotate(4, 2);
			Assertions.assertEquals(cube.show(), BACK2_EXPECTED);
		} catch (InterruptedException ignored) {
		}
	}

	private static final String LEFT1_BACK2_EXPECTED =
			"0400"
					+ "0400"
					+ "3333"
					+ "0400"

					+ "1101"
					+ "1101"
					+ "1141"
					+ "1101"

					+ "2022"
					+ "2022"
					+ "2022"
					+ "2022"

					+ "3533"
					+ "3533"
					+ "3233"
					+ "3533"

					+ "4454"
					+ "4454"
					+ "4454"
					+ "4454"

					+ "5255"
					+ "1111"
					+ "5255"
					+ "5255";

	@Test
	public void shouldRotateLeft1Back2Sequential() {
		try {
			cube.rotate(1, 1);
			cube.rotate(4, 2);
			Assertions.assertEquals(cube.show(), LEFT1_BACK2_EXPECTED);
		} catch (InterruptedException ignored) {
		}
	}

}
