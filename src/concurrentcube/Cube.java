package concurrentcube;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

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

		private final Lock lock = new ReentrantLock();

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

		public void onRotatorEntry(int side, int layer) throws InterruptedException {
			RotatorType rotatorType = RotatorType.get(side);

			lock.lockInterruptibly();
			try {
				if (shouldRotatorWait(rotatorType)) {
					addWaitingRotatorInfo(rotatorType);
					lock.unlock();
					waitBeforeRotationCubeAccess(rotatorType);
					lock.lock();
					removeWaitingRotatorInfo(rotatorType);
				}
				addWorkingRotatorInfo(rotatorType);

				if (Thread.currentThread().isInterrupted()) {
					onRotatorExit(side, layer);
					throw new InterruptedException();
				} else {
					wakeNextWaitingRotator(rotatorType);
				}
			} finally {
				lock.unlock();
			}

			getRotationLayerLock(side, layer).lock();
			if (Thread.currentThread().isInterrupted()) {
				onRotatorExit(side, layer);
			}
		}

		public void onRotatorExit(int side, int layer) throws InterruptedException {
			getRotationLayerLock(side, layer).unlock();
			RotatorType rotatorType = RotatorType.get(side);

			lock.lock();
			try {
				removeWorkingRotatorInfo(rotatorType);
				if (workingRotatorsCount == 0) {
					if (waitingInspectorsCount > 0) {
						waitingInspectors.release();
					} else if (waitingRotatorsTotalCount > 0) {
						waitingRotatorsRepresentatives.release();
					}
				}

				if (Thread.currentThread().isInterrupted()) {
					throw new InterruptedException();
				}
			} finally {
				lock.unlock();
			}
		}

		public void onInspectorEntry() throws InterruptedException {
			lock.lockInterruptibly();
			if (shouldInspectorWait()) {
				++waitingInspectorsCount;
				lock.unlock();
				waitingInspectors.acquireUninterruptibly();
				lock.lock();
				--waitingInspectorsCount;
			}
			++workingInspectorsCount;

			try {
				if (!Thread.currentThread().isInterrupted()) {
					wakeNextWaitingInspector();
				} else {
					onInspectorExit();
					throw new InterruptedException();
				}
			} finally {
				lock.unlock();
			}
		}

		public void onInspectorExit() throws InterruptedException {
			lock.lock();
			try {
				--workingInspectorsCount;
				if (workingInspectorsCount == 0) {
					if (waitingRotatorsTotalCount > 0) {
						waitingRotatorsRepresentatives.release();
					} else if (waitingInspectorsCount > 0) {
						waitingInspectors.release();
					}
				}

				if (Thread.currentThread().isInterrupted()) {
					throw new InterruptedException();
				}
			} finally {
				lock.unlock();
			}
		}

		private boolean shouldRotatorWait(RotatorType rotatorType) {
			return workingInspectorsCount > 0
					|| waitingInspectorsCount > 0
					|| (workingRotatorType != null && workingRotatorType != rotatorType)
					|| waitingRotatorsTotalCount > 0;
		}

		private void waitBeforeRotationCubeAccess(RotatorType rotatorType) throws InterruptedException {
			if (waitingRotatorCounts.get(rotatorType) == 1) {
				waitingRotatorsRepresentatives.acquireUninterruptibly();
			} else {
				waitingRotators.get(rotatorType).acquireUninterruptibly();
			}
		}

		private void addWaitingRotatorInfo(RotatorType rotatorType) {
			waitingRotatorCounts.put(rotatorType, waitingRotatorCounts.get(rotatorType) + 1);
			++waitingRotatorsTotalCount;
		}

		private void removeWaitingRotatorInfo(RotatorType rotatorType) {
			waitingRotatorCounts.put(rotatorType, waitingRotatorCounts.get(rotatorType) - 1);
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

		private void wakeNextWaitingRotator(RotatorType rotatorType) {
			if (waitingRotatorCounts.get(rotatorType) > 0) {
				waitingRotators.get(rotatorType).release();
			}
		}

		private boolean shouldInspectorWait() {
			return workingRotatorsCount > 0 || waitingRotatorsTotalCount > 0;
		}

		private void wakeNextWaitingInspector() {
			if (waitingInspectorsCount > 0) {
				waitingInspectors.release();
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
