package concurrentcube;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import concurrentcube.rotation.RotatorType;
import concurrentcube.structure.SideType;

public class CubeTest {

	private Cube cube;

	// Counter that enables checking whether all waiting threads
	// finally accessed cube and finished their work.
	AtomicInteger waitingThreadsCount = new AtomicInteger(0);

	@BeforeEach
	public void resetCube() {
		cube = new Cube(4, (x, y) -> {}, (x, y) -> {}, () -> {}, () -> {});
		waitingThreadsCount.set(0);
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

	@Test
	public void shouldShowSolvedCube() {
		try {
			Assertions.assertEquals(SOLVED_EXPECTED, cube.show());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
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
	public void shouldPassValidationSequential() {
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
	public void shouldRotateLeft1LayerSequential() {
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
	public void shouldRotateBack2LayerSequential() {
		try {
			cube.rotate(4, 2);
			Assertions.assertEquals(cube.show(), BACK2_EXPECTED);
		} catch (InterruptedException ignored) {
		}
	}

	@Test
	public void shouldShowSolvedCubeAfterCyclicRotations() {
		try {
			for (int i = 0; i < 1260; i++) {
				cube.rotate(3, 0);
				cube.rotate(0, 0);
				cube.rotate(0, 0);
				cube.rotate(5, 0);
				cube.rotate(5, 0);
				cube.rotate(5, 0);
				cube.rotate(4, 0);
				cube.rotate(5, 0);
				cube.rotate(5, 0);
				cube.rotate(5, 0);
			}
			Assertions.assertEquals(cube.show(), SOLVED_EXPECTED);
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
	public void shouldRotateLeft1Back2LayersSequential() {
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
	public void shouldRotateFront2Left2Bottom1LayersSequential() {
		try {
			cube.rotate(2, 2);
			cube.rotate(1, 2);
			cube.rotate(5, 1);
			Assertions.assertEquals(cube.show(), FRONT2_LEFT2_BOTTOM1_EXPECTED);
		} catch (InterruptedException ignored) {
		}
	}

	private static final int PARALLEL_ROTATORS = 420;

	@Test
	public void shouldRotateParallelLayersConcurrently() {
		// given
		cube = getSolvedCube(PARALLEL_ROTATORS, 1000);

		List<Thread> parallel = getParallelRotators(0, PARALLEL_ROTATORS);
		List<Thread> parallelCounterClockwise = getParallelRotators(5, PARALLEL_ROTATORS);

		// when
		try {
			startThreads(parallel, parallelCounterClockwise);
			joinThreads(5000, parallel, parallelCounterClockwise);

			// then
			Assertions.assertEquals(cube.show(), getSolvedCube(PARALLEL_ROTATORS, 0).show());
		} catch (InterruptedException e) {
			Assertions.fail();
		}
	}

	private static final int CONCURRENT_INSPECTORS = 420;

	@Test
	public void shouldInspectCubeConcurrently() {
		// given
		cube = getSolvedCube(4, 1000);
		waitingThreadsCount.set(CONCURRENT_INSPECTORS);
		List<Thread> inspectors = getInspectors(CONCURRENT_INSPECTORS);

		// when
		try {
			startThreads(inspectors);
			joinThreads(5000, inspectors);

			// then
			Assertions.assertEquals(cube.show(), SOLVED_EXPECTED);
		} catch (InterruptedException e) {
			Assertions.fail();
		}
	}

	private static final int DEADLOCK_TEST_ATTEMPTS = 420;
	private static final int CUBE_SIZE = 42;
	private static final int WORKER_TYPES = 7;

	@RepeatedTest(DEADLOCK_TEST_ATTEMPTS)
	public void shouldNotDeadlock() {
		cube = new Cube(CUBE_SIZE,
				(x, y) -> {},
				(x, y) -> waitingThreadsCount.decrementAndGet(),
				() -> {},
				waitingThreadsCount::decrementAndGet);

		// given
			List<Thread> inspectors = getInspectors(CUBE_SIZE);
			List<Thread> rotatorsXY = getParallelRotators(2, CUBE_SIZE);
			rotatorsXY.addAll(getParallelRotators(4, CUBE_SIZE));

			List<Thread> rotatorsXZ = getParallelRotators(0, CUBE_SIZE);
			rotatorsXZ.addAll(getParallelRotators(5, CUBE_SIZE));

			List<Thread> rotatorsYZ = getParallelRotators(1, CUBE_SIZE);
			rotatorsYZ.addAll(getParallelRotators(3, CUBE_SIZE));

			waitingThreadsCount.set(CUBE_SIZE * WORKER_TYPES);

			// when
			try {
				startThreads(inspectors, rotatorsXZ, rotatorsXY, rotatorsYZ);
				joinThreads(1000, inspectors, rotatorsXZ, rotatorsXY, rotatorsYZ);

				// then
				Assertions.assertEquals(0, waitingThreadsCount.intValue());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				System.out.println("Main thread interrupted!");
			}
	}

	@RepeatedTest(DEADLOCK_TEST_ATTEMPTS)
	public void shouldNotDeadlockWhenInterruptedRotators() {
		cube = new Cube(CUBE_SIZE,
				(x, y) -> {},
				(x, y) -> {},
				() -> {},
				() -> waitingThreadsCount.decrementAndGet());
			// given
			List<Thread> inspectors = getInspectors(CUBE_SIZE);

			List<Thread> rotatorsXY = getParallelRotators(2, CUBE_SIZE);
			rotatorsXY.addAll(getParallelRotators(4, CUBE_SIZE));

			List<Thread> rotatorsXZ = getParallelRotators(0, CUBE_SIZE);
			rotatorsXZ.addAll(getParallelRotators(5, CUBE_SIZE));

			List<Thread> rotatorsYZ = getParallelRotators(1, CUBE_SIZE);
			rotatorsYZ.addAll(getParallelRotators(3, CUBE_SIZE));

			List<Thread> toInterrupt = getParallelRotators(2, CUBE_SIZE);

			waitingThreadsCount.set(CUBE_SIZE);

			// when
			try {
				startThreads(toInterrupt, inspectors, rotatorsXZ, rotatorsXY, rotatorsYZ);
				interruptAll(toInterrupt);
				joinThreads(1000, inspectors, rotatorsXZ, rotatorsXY, rotatorsYZ);

				// then
				Assertions.assertEquals(0, waitingThreadsCount.intValue());
			} catch (InterruptedException ignored) {
				Thread.currentThread().interrupt();
			}

	}

	@RepeatedTest(DEADLOCK_TEST_ATTEMPTS)
	public void shouldNotDeadlockWhenInterruptedInspectors() {
		cube = new Cube(CUBE_SIZE,
				(x, y) -> {},
				(x, y) -> waitingThreadsCount.decrementAndGet(),
				() -> {},
				() -> {});

		// given
			List<Thread> inspectors = getInspectors(CUBE_SIZE);

			List<Thread> rotatorsXY = getParallelRotators(2, CUBE_SIZE);
			rotatorsXY.addAll(getParallelRotators(4, CUBE_SIZE));

			List<Thread> rotatorsXZ = getParallelRotators(0, CUBE_SIZE);
			rotatorsXZ.addAll(getParallelRotators(5, CUBE_SIZE));

			List<Thread> rotatorsYZ = getParallelRotators(1, CUBE_SIZE);
			rotatorsYZ.addAll(getParallelRotators(3, CUBE_SIZE));

			List<Thread> toInterrupt = getInspectors(CUBE_SIZE);

			waitingThreadsCount.set(CUBE_SIZE * (WORKER_TYPES - 1));

			// when
			try {
				startThreads(toInterrupt, inspectors, rotatorsXZ, rotatorsXY, rotatorsYZ);
				interruptAll(toInterrupt);
				joinThreads(1000, inspectors, rotatorsXZ, rotatorsXY, rotatorsYZ);

				// then
				Assertions.assertEquals(0, waitingThreadsCount.intValue());
			} catch (InterruptedException ignored) {
				Thread.currentThread().interrupt();
			}

	}

	private static final int SECURITY_TEST_ATTEMPTS = 420;

	@RepeatedTest(SECURITY_TEST_ATTEMPTS)
	public void shouldNotViolateCubeAccess() {
		CubeAccessData accessData = new CubeAccessData();

		cube = new Cube(CUBE_SIZE,
				(x, y) -> accessData.notifyRotatorEntrance(RotatorType.get(x)),
				(x, y) -> accessData.notifyRotatorExit(RotatorType.get(x)),
				accessData::notifyInspectorEntrance,
				accessData::notifyInspectorExit);

		// given
			List<Thread> inspectors = getInspectors(CUBE_SIZE);

			List<Thread> rotatorsXY = getParallelRotators(2, CUBE_SIZE);
			rotatorsXY.addAll(getParallelRotators(4, CUBE_SIZE));

			List<Thread> rotatorsXZ = getParallelRotators(0, CUBE_SIZE);
			rotatorsXZ.addAll(getParallelRotators(5, CUBE_SIZE));

			List<Thread> rotatorsYZ = getParallelRotators(1, CUBE_SIZE);
			rotatorsYZ.addAll(getParallelRotators(3, CUBE_SIZE));

			List<Thread> toInterrupt = getInspectors(CUBE_SIZE);

			// when
			try {
				startThreads(toInterrupt, inspectors, rotatorsXZ, rotatorsXY, rotatorsYZ);
				interruptAll(toInterrupt);
				joinThreads(1000, inspectors, rotatorsXZ, rotatorsXY, rotatorsYZ);

				// then
				Assertions.assertFalse(accessData.isSecurityViolated());
			} catch (InterruptedException ignored) {
			}
	}

	private static final int ROTATION_TRIES = 420;

	// Checks, whether after many rotation
	// cube still contains correct numbers of squares
	// of every color.
	@RepeatedTest(ROTATION_TRIES)
	public void shouldCubeHaveSameNumbersOfTilesAsSolved() {
		// given
		Map<Integer, Integer> tileCounts = new HashMap<>();
		cube = getSolvedCube(CUBE_SIZE, 0);

		// when
		try {
				List<Thread> inspectors = getInspectors(CUBE_SIZE);

				List<Thread> rotatorsXY = getParallelRotators(2, CUBE_SIZE);
				rotatorsXY.addAll(getParallelRotators(4, CUBE_SIZE));

				List<Thread> rotatorsXZ = getParallelRotators(0, CUBE_SIZE);
				rotatorsXZ.addAll(getParallelRotators(5, CUBE_SIZE));

				List<Thread> rotatorsYZ = getParallelRotators(1, CUBE_SIZE);
				rotatorsYZ.addAll(getParallelRotators(3, CUBE_SIZE));

				startThreads(inspectors, rotatorsXZ, rotatorsXY, rotatorsYZ);
				joinThreads(1000, inspectors, rotatorsXZ, rotatorsXY, rotatorsYZ);

			// then
			String cubeRepresentation = cube.show();
			for (char c : cubeRepresentation.toCharArray()) {
				tileCounts.merge(c - '0', 1, Integer::sum);
			}

			for (var side : SideType.values()) {
				Assertions.assertEquals(CUBE_SIZE * CUBE_SIZE, tileCounts.get(side.ordinal()));
			}
		} catch (InterruptedException ignored) {
			Thread.currentThread().interrupt();
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

	private void interruptAll(List<Thread> threads) {
		for (Thread t : threads) {
			t.interrupt();
		}
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
				(x, y) -> {waitingThreadsCount.decrementAndGet();},
				() -> {
					try {
						Thread.sleep(actionTime);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				},
				() -> {waitingThreadsCount.decrementAndGet();});
	}

	/**
	 * Helper class to enable monitoring
	 * what types of workers have concurrent access
	 * to the cube.
	 */
	private static class CubeAccessData {

		Lock lock = new ReentrantLock();

		int rotatorsInsideXY = 0;
		int rotatorsInsideYZ = 0;
		int rotatorsInsideXZ = 0;
		int inspectorsInside = 0;

		boolean securityViolated = false;

		public void notifyRotatorEntrance(RotatorType rotatorType) {
			lock.lock();
			switch (rotatorType) {
				case XY:
					++rotatorsInsideXY;
					securityViolated |= (inspectorsInside > 0 || rotatorsInsideYZ + rotatorsInsideXZ > 0);
					break;
				case YZ:
					++rotatorsInsideYZ;
					securityViolated |= (inspectorsInside > 0 || rotatorsInsideXY + rotatorsInsideXZ > 0);
					break;
				default:
					++rotatorsInsideXZ;
					securityViolated |= (inspectorsInside > 0 || rotatorsInsideXY + rotatorsInsideYZ > 0);
					break;
			}
			lock.unlock();
		}

		public void notifyRotatorExit(RotatorType rotatorType) {
			lock.lock();
			switch (rotatorType) {
				case XY:
					--rotatorsInsideXY;
					break;
				case YZ:
					--rotatorsInsideYZ;
					break;
				default:
					--rotatorsInsideXZ;
					break;
			}
			lock.unlock();
		}

		public void notifyInspectorEntrance() {
			lock.lock();
			++inspectorsInside;
			securityViolated |= rotatorsInsideXZ + rotatorsInsideXY + rotatorsInsideXY > 0;
			lock.unlock();
		}

		public void notifyInspectorExit() {
			lock.lock();
			--inspectorsInside;
			lock.unlock();
		}

		public boolean isSecurityViolated() {
			return securityViolated;
		}

	}

}
