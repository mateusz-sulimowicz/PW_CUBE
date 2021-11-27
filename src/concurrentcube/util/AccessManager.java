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

	/**
	 * Menedżer dostępu do kostki. Zapobiega:
	 * - równoczesnym obrotom kolidujących warstw,
	 * - równoczesnemu oglądaniu i obracaniu kostki
	 * - zagłodzeniu wątków czekających na swoją kolej.
	 * Zajmuje się również obsługą przerwań wątków pracujących na kostce.
	 */
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

	// ---------- OBRACANIE KOSTKI ------------

	public void onRotatorEntry(int side, int layer) throws InterruptedException {
		lock.lockInterruptibly();
		RotatorType rotator = RotatorType.get(side);
		addWaitingRotatorInfo(rotator);
		try {
			if (shouldRotatorWait(rotator)) {
				// Jeśli wątek nie powinien wejść do kostki to czeka.
				// Zapobiega braku bezpieczeństwa i zagłodzeniu
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
			onAfterRotation(side, layer);
		}
	}

	private void onAfterRotation(int side, int layer) throws InterruptedException {
		getRotationLayerLock(side, layer).unlock();
	}

	public void onRotatorExit(int side, int layer) throws InterruptedException {
		onAfterRotation(side, layer);

		lock.lock();
		removeWorkingRotatorInfo();
		notifyAllIfCubeIsUnoccupied();
		lock.unlock();
		if (Thread.interrupted()) {
			throw new InterruptedException("Rotator " + Thread.currentThread().getName() + "interrupted.");
		}
	}

	// -------- OGLĄDANIE KOSTKI ---------

	public void onInspectorEntry() throws InterruptedException {
		lock.lock();
		++waitingInspectorsCount;
		try {
			if (shouldInspectorWait()) {
				// Jeśli wątek nie powinien wejść do kostki to czeka.
				// Zapobiega braku bezpieczeństwa i zagłodzeniu
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

	// ------ metody pomocnicze ------

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
		// Czeka jeśli ktoś ogląda kostkę lub chce oglądać
		// lub obracajce wątki z nim kolidują
		// lub czekają wątki obracające w kolidujący z nim sposób.
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
			// ostatni obracający
			workingRotatorType = null;
		}
	}

	private boolean shouldInspectorWait() {
		// Czeka jeśli ktoś obraca lub chce obracać.
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
