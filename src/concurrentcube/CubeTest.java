package concurrentcube;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
		} catch (InterruptedException ignored) {}
	}

	private static final String FRONT2_LEFT2_BOTTOM1_EXPECTED =
			 		  "0040"
					+ "1141"
					+ "0040"
					+ "0040"

					+ "1511"
					+ "1511"
					+ "4544"
					+ "1511"

					+ "2202"
					+ "2212"
					+ "1511"
					+ "2202"

					+ "3303"
					+ "3303"
					+ "2202"
					+ "3303"

					+ "4544"
					+ "4344"
					+ "3303"
					+ "4544"

					+ "5525"
					+ "5525"
					+ "3323"
					+ "5525";

	@Test
	public void shouldRotateFront2Left2Bottom1() {
		try {
			cube.rotate(2, 2);
			cube.rotate(1, 2);
			cube.rotate(5, 1);
			Assertions.assertEquals(cube.show(), FRONT2_LEFT2_BOTTOM1_EXPECTED);
		} catch (InterruptedException ignored) {}
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
			joinThreads(0, parallel, parralelCounterClockwise);

			// then
			Assertions.assertEquals(cube.show(), getSolvedCube(PARALLEL_ROTATORS, 0).show());
		} catch (InterruptedException e) {
			Assertions.fail();
		}
	}

	private static final int CONCURRENT_INSPECTORS = 1000;

	@Test
	public void shouldInspectCubeConcurrently() {
		// given
		cube = getSolvedCube(4, 1000);
		List<Thread> inspectors = getInspectors(CONCURRENT_INSPECTORS);

		// when
		try {
			startThreads(inspectors);
			joinThreads(0, inspectors);

			// then
			Assertions.assertEquals(cube.show(), SOLVED_EXPECTED);
		} catch (InterruptedException e) {
			Assertions.fail();
		}
	}

	private static final int DEADLOCK_TEST_ATTEMPTS = 1000;

	@Test
	public void shouldNotDeadlock() {
		AtomicInteger waitingThreadsCount = new AtomicInteger(0);

		cube = new Cube(10,
				(x, y) -> waitingThreadsCount.incrementAndGet(),
				(x, y) -> waitingThreadsCount.decrementAndGet(),
				waitingThreadsCount::incrementAndGet,
				waitingThreadsCount::decrementAndGet);
		for (int i = 0; i < DEADLOCK_TEST_ATTEMPTS; ++i) {
			// given
			List<Thread> inspectors = getInspectors(10);
			List<Thread> rotatorsXY = getParallelRotators(2, 10);
			rotatorsXY.addAll(getParallelRotators(4, 10));

			List<Thread> rotatorsXZ = getParallelRotators(0, 10);
			rotatorsXZ.addAll(getParallelRotators(5, 10));

			List<Thread> rotatorsYZ = getParallelRotators(1, 10);
			rotatorsYZ.addAll(getParallelRotators(3, 10));

			// when
			try {
				startThreads(inspectors, rotatorsXZ, rotatorsXY, rotatorsYZ);
				joinThreads(400, inspectors, rotatorsXZ, rotatorsXY, rotatorsYZ);

				// then
				Assertions.assertEquals(waitingThreadsCount.intValue(), 0);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				System.out.println("Main thread interrupted!");
			}
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

	private void joinThreads(int timeout, List<Thread>... threadLists) throws InterruptedException {
		for (var threads : threadLists) {
			for (var thread : threads) {
				thread.join(timeout);
			}
		}
	}

	private Cube getSolvedCube(int size, int actionTime) {
		// rotation and inspection take actionTime milliseconds.
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
