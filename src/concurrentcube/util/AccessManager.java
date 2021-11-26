package concurrentcube.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import concurrentcube.rotation.RotatorType;

public class AccessManager {

	private final Logger logger = Logger.getLogger(AccessManager.class.getName());

	private final Lock lock = new ReentrantLock();

	private final Condition condition = lock.newCondition();

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

	public void onBeforeRotation(int side, int layer) throws InterruptedException {
		RotatorType rotator = RotatorType.get(side);
		lock.lock();
		while (workingInspectorsCount > 0 || (workingRotatorType != null && workingRotatorType != rotator)) {
			addWaitingRotatorInfo(rotator);
			logger.info(Thread.currentThread().getName() + ": "
					+ "Rotator " + rotator + " waiting. Waiting rotators: " + waitingRotatorsTotalCount + " "
					+ waitingRotatorCounts);

			condition.await();

			removeWaitingRotatorInfo(rotator);
			logger.info(Thread.currentThread().getName() + ": "
					+ "Rotator " + rotator + " awaken. Waiting rotators: " + waitingRotatorsTotalCount + " "
					+ waitingRotatorCounts);
		}
		addWorkingRotatorInfo(rotator);
		lock.unlock();

		try {
			getRotationLayerLock(side, layer).lockInterruptibly();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			onRotatorExit(side);
		}
	}

	public void onRotatorExit(int side) throws InterruptedException {
		RotatorType rotatorType = RotatorType.get(side);

		lock.lock();
		removeWorkingRotatorInfo(rotatorType);
		if (workingRotatorsCount == 0) {
			condition.signalAll();
		}
		lock.unlock();

		if (Thread.interrupted()) {
			throw new InterruptedException("Rotator " + Thread.currentThread().getName() + "interrupted.");
		}
	}

	public void onAfterRotation(int side, int layer) throws InterruptedException {
		getRotationLayerLock(side, layer).unlock();
		onRotatorExit(side);
	}

	public void onInspectorEntry() throws InterruptedException {
		lock.lock();
		while (workingRotatorsCount > 0) {
			++waitingInspectorsCount;
			logger.info(Thread.currentThread().getName() + ": "
					+ "Inspector " + " waiting. "
					+ "Waiting inspectors: " + waitingInspectorsCount);

			condition.await();

			--waitingInspectorsCount;
			logger.info(Thread.currentThread().getName() + ": "
					+ "Inspector " + " awaken. "
					+ "Waiting inspectors: " + waitingInspectorsCount);
		}
		++workingInspectorsCount;
		lock.unlock();

		if (Thread.interrupted()) {
			Thread.currentThread().interrupt();
			onInspectorExit();
		}
	}

	public void onInspectorExit() throws InterruptedException {
		lock.lock();
		--workingInspectorsCount;
		if (workingInspectorsCount == 0) {
			condition.signalAll();
		}
		lock.unlock();

		if (Thread.interrupted()) {
			throw new InterruptedException("Inspector " + Thread.currentThread().getName() + "interrupted.");
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

	private Lock getRotationLayerLock(int side, int layer) {
		if (side == 0 || side == 1 || side == 2) {
			return rotationLayersLocks[layer];
		} else {
			return rotationLayersLocks[size - 1 - layer];
		}
	}

}
