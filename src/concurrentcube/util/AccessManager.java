package concurrentcube.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import concurrentcube.rotation.RotatorType;

public class AccessManager {

	private final Lock lock = new ReentrantLock(true);

	private final Condition isCubeAvailable = lock.newCondition();

	private int waitingRotatorsTotalCount;
	private final Map<RotatorType, Integer> waitingRotatorCounts;

	private int workingRotatorsCount;
	private RotatorType workingRotatorType;

	private int waitingInspectorsCount;
	private int workingInspectorsCount;

	private final Lock[] rotationLayersLocks;

	private final int size;

	public AccessManager(int size) {
		this.size = size;
		this.waitingRotatorCounts = new HashMap<>();

		for (var rotatorType : RotatorType.values()) {
			waitingRotatorCounts.put(rotatorType, 0);
		}

		rotationLayersLocks = new ReentrantLock[size];
		for (int i = 0; i < size; ++i) {
			rotationLayersLocks[i] = new ReentrantLock();
		}
	}

	// ---------- CUBE ROTATION ------------

	public void onRotatorEntry(int side, int layer) throws InterruptedException {
		lock.lockInterruptibly();
		RotatorType rotator = RotatorType.get(side);
		addWaitingRotatorInfo(rotator);
		try {
			if (shouldRotatorWait(rotator)) {
				// If thread should not enter the cube right now,
				// it is supposed to wait
				// until being notified that cube is unoccupied.
				waitBeforeRotationAccess(rotator);
			}
			addWorkingRotatorInfo(rotator);
		} catch (InterruptedException e) {
			notifyAllIfCubeIsUnoccupied();
			throw e;
		} finally {
			removeWaitingRotatorInfo(rotator);
			lock.unlock();
		}

		onBeforeRotation(side, layer);
	}

	private void onBeforeRotation(int side, int layer) throws InterruptedException {
		try {
			getRotationLayerLock(side, layer).lockInterruptibly();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			onRotatorExit();
		}
	}

	public void onAfterRotation(int side, int layer) throws InterruptedException {
		getRotationLayerLock(side, layer).unlock();
		onRotatorExit();
	}

	public void onRotatorExit() throws InterruptedException {
		lock.lock();
		removeWorkingRotatorInfo();
		notifyAllIfCubeIsUnoccupied();
		lock.unlock();
		if (Thread.interrupted()) {
			throw new InterruptedException("Rotator " + Thread.currentThread().getName() + "interrupted.");
		}
	}

	// -------- CUBE INSPECTION ---------

	public void onInspectorEntry() throws InterruptedException {
		lock.lock();
		++waitingInspectorsCount;
		try {
			if (shouldInspectorWait()) {
				// If thread should not enter the cube right now,
				// it is supposed to wait
				// until being notified that cube is unoccupied.
				waitBeforeInspectionAccess();
			}
			++workingInspectorsCount;
		} catch (InterruptedException e) {
			notifyAllIfCubeIsUnoccupied();
			throw e;
		} finally {
			--waitingInspectorsCount;
			lock.unlock();
		}
	}

	public void onInspectorExit() throws InterruptedException {
		lock.lock();
		--workingInspectorsCount;
		notifyAllIfCubeIsUnoccupied();
		lock.unlock();

		if (Thread.interrupted()) {
			throw new InterruptedException("Inspector " + Thread.currentThread().getName() + "interrupted.");
		}
	}

	// ------ helper methods ------

	private void addWaitingRotatorInfo(RotatorType rotatorType) {
		waitingRotatorCounts.merge(rotatorType, 1, Integer::sum);
		++waitingRotatorsTotalCount;
	}

	private boolean areOtherRotatorTypesWaiting(RotatorType rotatorType) {
		for (RotatorType rotator : waitingRotatorCounts.keySet()) {
			if (rotator != rotatorType && waitingRotatorCounts.get(rotator) > 0) {
				return true;
			}
		}
		return false;
	}

	private boolean shouldRotatorWait(RotatorType rotatorType) {
		// Waits if someone is inspecting the cube
		// or wants to inspect it
		// or working threads are rotating the cube in colliding way
		// or there are colliding waiting rotators.
		return workingInspectorsCount > 0
				|| waitingInspectorsCount > 0
				|| (workingRotatorType != null && workingRotatorType != rotatorType)
				|| areOtherRotatorTypesWaiting(rotatorType);
	}

	private void waitBeforeRotationAccess(RotatorType rotator) throws InterruptedException {
		do {
			isCubeAvailable.await();
		} while (workingInspectorsCount > 0 || (workingRotatorType != null && workingRotatorType != rotator));
	}

	private void removeWaitingRotatorInfo(RotatorType rotatorType) {
		waitingRotatorCounts.merge(rotatorType, -1, Integer::sum);
		--waitingRotatorsTotalCount;
	}

	public void addWorkingRotatorInfo(RotatorType rotatorType) {
		++workingRotatorsCount;
		workingRotatorType = rotatorType;
	}

	private Lock getRotationLayerLock(int side, int layer) {
		if (side == 0 || side == 1 || side == 2) {
			return rotationLayersLocks[layer];
		} else {
			return rotationLayersLocks[size - 1 - layer];
		}
	}

	public void removeWorkingRotatorInfo() {
		--workingRotatorsCount;
		if (workingRotatorsCount == 0) {
			// the last working rotator
			workingRotatorType = null;
		}
	}

	private boolean shouldInspectorWait() {
		// Waits if someone is rotating or wants to rotate.
		return workingRotatorsCount > 0 || waitingRotatorsTotalCount > 0;
	}

	private void waitBeforeInspectionAccess() throws InterruptedException {
		do {
			isCubeAvailable.await();
		} while (workingRotatorsCount > 0);
	}

	private void notifyAllIfCubeIsUnoccupied() {
		if (workingRotatorsCount == 0 && workingInspectorsCount == 0) {
			isCubeAvailable.signalAll();
		}
	}

}
