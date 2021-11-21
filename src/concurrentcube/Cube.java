package concurrentcube;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

import concurrentcube.inspection.CubeInspector;
import concurrentcube.rotation.CubeRotator;
import concurrentcube.rotation.RotatorType;
import concurrentcube.structure.CubeState;

public class Cube {

	private final CubeRotator rotator;
	private final CubeInspector inspector;
	private final int size;

	public Cube(int size,
			BiConsumer<Integer, Integer> beforeRotation,
			BiConsumer<Integer, Integer> afterRotation,
			Runnable beforeShowing,
			Runnable afterShowing) {
		this.size = size;
		AccessManager accessManager = new AccessManager();
		CubeState state = new CubeState(size);
		rotator = new CubeRotator(state, beforeRotation, afterRotation, accessManager);
		inspector = new CubeInspector(state, beforeShowing, afterShowing, accessManager);
	}

	public void rotate(int side, int layer) throws InterruptedException {
		rotator.rotate(side, layer);
	}

	public String show() throws InterruptedException {
		return inspector.show();
	}

	public class AccessManager {

		private final Logger logger = Logger.getLogger(AccessManager.class.getName());

		private final Semaphore mutex = new Semaphore(1);

		private final Semaphore waitingRotatorsRepresentatives = new Semaphore(0);
		private int waitingRotatorsTotalCount;
		private final Map<RotatorType, Semaphore> waitingRotators;
		private final Map<RotatorType, Integer> waitingRotatorCounts;

		private int workingRotatorsCount;
		private RotatorType workingRotatorType;

		private final Semaphore waitingInspectors = new Semaphore(0);
		private int waitingInspectorsCount;
		private int workingInspectorsCount;

		private final Lock[] rotationLayersLocks;

		public AccessManager() {
			this.waitingRotators = new HashMap<>();
			this.waitingRotatorCounts = new HashMap<>();

			for (var rotatorType : RotatorType.values()) {
				waitingRotators.put(rotatorType, new Semaphore(0));
				waitingRotatorCounts.put(rotatorType, 0);
			}

			rotationLayersLocks = new ReentrantLock[size];
			for (int i = 0; i < size; ++i) {
				rotationLayersLocks[i] = new ReentrantLock();
			}
		}

		public void onBeforeRotation(int side, int layer) throws InterruptedException {
			RotatorType rotator = RotatorType.get(side);
			mutex.acquire();
			if (shouldRotatorWait(rotator)) {
				addWaitingRotatorInfo(rotator);
				logger.info(Thread.currentThread().getName() + ": "
						+ "Rotator " + rotator + " waiting. Waiting rotators: " + waitingRotatorsTotalCount + " "
						+ waitingRotatorCounts);

				waitBeforeRotationCubeAccess(rotator);
				// dziedziczenie ochrony

				removeWaitingRotatorInfo(rotator);
				logger.info(Thread.currentThread().getName() + ": "
						+ "Rotator " + rotator + " awaken. Waiting rotators: " + waitingRotatorsTotalCount + " "
						+ waitingRotatorCounts);
			}
			addWorkingRotatorInfo(rotator);
			wakeNextWaitingRotator(rotator);

			if (Thread.interrupted()) {
				onRotatorExit(side);
				throw new InterruptedException("Rotator " + Thread.currentThread().getName() + "interrupted.");
			}

			getRotationLayerLock(side, layer).lock();

			if (Thread.interrupted()) {
				onRotatorExit(side);
				throw new InterruptedException("Rotator " + Thread.currentThread().getName() + "interrupted.");
			}
		}

		public void onRotatorExit(int side) throws InterruptedException {
			RotatorType rotatorType = RotatorType.get(side);

			mutex.acquireUninterruptibly();
			removeWorkingRotatorInfo(rotatorType);
			if (workingRotatorsCount == 0 && waitingInspectorsCount > 0) {
				// Przekazanie ochrony
				waitingInspectors.release();
			} else if (workingRotatorsCount == 0 && waitingRotatorsTotalCount > 0) {
				// Przekazanie ochrony
				waitingRotatorsRepresentatives.release();
			} else {
				mutex.release();
			}

			if (Thread.interrupted()) {
				throw new InterruptedException("Rotator " + Thread.currentThread().getName() + "interrupted.");
			}
		}

		public void onAfterRotation(int side, int layer) throws InterruptedException {
			getRotationLayerLock(side, layer).unlock();
			onRotatorExit(side);
		}

		public void onInspectorEntry() throws InterruptedException {
			mutex.acquire();
			if (shouldInspectorWait()) {
				++waitingInspectorsCount;
				logger.info(Thread.currentThread().getName() + ": "
						+ "Inspector " + " waiting. "
						+ "Waiting inspectors: " + waitingInspectorsCount);

				mutex.release();
				waitingInspectors.acquireUninterruptibly();
				// dziedziczenie ochrony

				--waitingInspectorsCount;
				logger.info(Thread.currentThread().getName() + ": "
						+ "Inspector " + " awaken. "
						+ "Waiting inspectors: " + waitingInspectorsCount);
			}
			++workingInspectorsCount;
			wakeNextWaitingInspector();

			if (Thread.interrupted()) {
				onInspectorExit();
				throw new InterruptedException("Inspector " + Thread.currentThread().getName() + "interrupted.");
			}
		}

		public void onInspectorExit() throws InterruptedException {
			mutex.acquireUninterruptibly();
			--workingInspectorsCount;
			if (workingInspectorsCount == 0 && waitingRotatorsTotalCount > 0) {
				// przekazanie ochrony
				waitingRotatorsRepresentatives.release();
			} else if (workingInspectorsCount == 0 && waitingInspectorsCount > 0) {
				// przekazanie ochrony
				waitingInspectors.release();
			} else {
				mutex.release();
			}

			if (Thread.interrupted()) {
				throw new InterruptedException("Inspector " + Thread.currentThread().getName() + "interrupted.");
			}
		}

		private boolean shouldRotatorWait(RotatorType rotatorType) {
			return workingInspectorsCount > 0
					|| waitingInspectorsCount > 0
					|| (workingRotatorType != null && workingRotatorType != rotatorType)
					|| waitingRotatorsTotalCount > 0;
		}

		private void waitBeforeRotationCubeAccess(RotatorType rotatorType) {
			// Jeśli jest pierwszym czekającym z grupy,
			// to czeka na semaforze dla reprezentantów grup.
			if (waitingRotatorCounts.get(rotatorType) == 1) {
				mutex.release();
				waitingRotatorsRepresentatives.acquireUninterruptibly();
			} else {
				mutex.release();
				waitingRotators.get(rotatorType).acquireUninterruptibly();
			}
		}

		private void addWaitingRotatorInfo(RotatorType rotatorType) {
			waitingRotatorCounts.merge(rotatorType, 1, Integer::sum);
			++waitingRotatorsTotalCount;
		}

		private void removeWaitingRotatorInfo(RotatorType rotatorType) {
			waitingRotatorCounts.merge(rotatorType, -1, Integer::sum);
			--waitingRotatorsTotalCount;
		}

		public void addWorkingRotatorInfo(RotatorType rotatorType) {
			++workingRotatorsCount;
			workingRotatorType = rotatorType;
		}

		public void removeWorkingRotatorInfo(RotatorType rotatorType) {
			--workingRotatorsCount;
			if (workingRotatorsCount == 0) {
				// ostatni obracający
				workingRotatorType = null;
			}
		}

		// Kaskadowe budzenie czekających obracaczy
		private void wakeNextWaitingRotator(RotatorType rotatorType) {
			if (waitingRotatorCounts.get(rotatorType) > 0) {
				// przekazanie ochrony
				waitingRotators.get(rotatorType).release();
			} else {
				mutex.release();
			}
		}

		private boolean shouldInspectorWait() {
			// Czeka jeśli ktoś obraca lub chce obracać.
			return workingRotatorsCount > 0 || waitingRotatorsTotalCount > 0;
		}

		// Kaskadowe budzenie czekających oglądaczy
		private void wakeNextWaitingInspector() {
			if (waitingInspectorsCount > 0) {
				waitingInspectors.release();
				// przekazanie ochrony
			} else {
				mutex.release();
			}
		}

		private Lock getRotationLayerLock(int side, int layer) {
			if (side == 0 || side == 1 || side == 2) {
				return rotationLayersLocks[layer];
			} else {
				return rotationLayersLocks[size - 1 - layer];
			}
		}

	}

}
