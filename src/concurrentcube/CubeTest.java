package concurrentcube;

import java.util.ArrayList;
import java.util.List;

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

	private static final int PARALLEL_ROTATORS = 1000;

	@Test
	public void shouldRotateParallelLayersConcurrently() {
		// given
		cube = getSolvedCube(PARALLEL_ROTATORS, 1000);
		List<Thread> parallel = getParallelRotators(0, PARALLEL_ROTATORS);
		List<Thread> parralelCounterClockwise = getParallelRotators(5, PARALLEL_ROTATORS);

		// when
		try {
			startThreads(parallel, parralelCounterClockwise);
			joinThreads(parallel, parralelCounterClockwise);

			// then
			Assertions.assertEquals(cube.show(), getSolvedCube(PARALLEL_ROTATORS, 0).show());
		} catch (InterruptedException e) {
			Assertions.fail();
		}
	}

	private static final int CONCURRENT_INSPECTORS = 500;

	@Test
	public void shouldInspectCubeConcurrently() {
		// given
		cube = getSolvedCube(CONCURRENT_INSPECTORS, 1000);
		List<Thread> inspectors = getInspectors(CONCURRENT_INSPECTORS);

		// when
		try {
			startThreads(inspectors);
			joinThreads(inspectors);

			// then
			Assertions.assertEquals(cube.show(), getSolvedCube(CONCURRENT_INSPECTORS, 0).show());
		} catch (InterruptedException e) {
			Assertions.fail();
		}
	}


	private List<Thread> getInspectors(int count) {
		List<Thread> inspectors = new ArrayList<>();
		for (int i = 0; i < count; ++i) {
			inspectors.add(getInspectorThread());
		}
		return inspectors;
	}

	private Thread getInspectorThread() {
		return new Thread(() -> {
			try {
				cube.show();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
	}


	private List<Thread> getParallelRotators(int side, int count) {
		List<Thread> rotators = new ArrayList<>();
		for (int i = 0; i < count; ++i) {
			rotators.add(getRotatorThread(side, i));
		}
		return rotators;
	}

	private Thread getRotatorThread(int side, int layer) {
		return new Thread(() -> {
			try {
				cube.rotate(side, layer);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
	}

	private void startThreads(List<Thread>... threadLists) {
		for (var threads : threadLists) {
			for (var thread : threads) {
				thread.start();
			}
		}
	}

	private void joinThreads(List<Thread>... threadLists) throws InterruptedException {
		for (var threads : threadLists) {
			for (var thread : threads) {
				thread.join();
			}
		}
	}

	private Cube getSolvedCube(int size, int actionTime) {
		// rotation and inspections take actionTime milliseconds.
		return new Cube(size,
				(x, y) -> {
					try {
						Thread.sleep(actionTime);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				},
				(x, y) -> {},
				() -> {
					try {
						Thread.sleep(actionTime);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				},
				() -> {});
	}

}
