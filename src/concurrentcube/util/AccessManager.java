package concurrentcube.util;

import java.util.HashMap;
import java.util.Map;
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
		if (shouldRotatorWait(rotator)) {
			addWaitingRotatorInfo(rotator);
			logger.info(Thread.currentThread().getName() + ":  "
					+ "Rotator " + rotator + " waiting. Waiting rotators: " + waitingRotatorsTotalCount + " "
					+ waitingRotatorCounts + "waiting inspectors: " + waitingInspectorsCount + "working inspectors: "
					+ workingInspectorsCount + "working rotators: " + workingRotatorsCount + "working rotator type: " + workingRotatorType);

			try {
				do {
					condition.await();
				} while (workingInspectorsCount > 0 || (workingRotatorType != null && workingRotatorType != rotator));
			} catch (InterruptedException e) {
				removeWaitingRotatorInfo(rotator);
				logger.info(Thread.currentThread().getName() + ":  "
						+  "JESTEM PRZERWANY! " + Thread.currentThread().getName()  + " Waiting rotators: " + waitingRotatorsTotalCount + " "
						+ waitingRotatorCounts + "waiting inspectors: " + waitingInspectorsCount + "working inspectors: "
						+ workingInspectorsCount + "working rotators: " + workingRotatorsCount + "working rotator type: " + workingRotatorType);

				if (workingRotatorsCount == 0 && workingInspectorsCount == 0) {
					condition.signalAll();
				}

				lock.unlock();
				throw e;
			}

			removeWaitingRotatorInfo(rotator);
			logger.info(Thread.currentThread().getName() + ": "
					+ "Rotator " + rotator + " awaken. Waiting rotators: " + waitingRotatorsTotalCount + " "
					+ waitingRotatorCounts);
		}

		logger.info(Thread.currentThread().getName() + " WCHODZE OBRACAACZ!: " + rotator);
		addWorkingRotatorInfo(rotator);
		lock.unlock();

		getRotationLayerLock(side, layer).lock();
	}

	public void onRotatorExit(int side) throws InterruptedException {
		RotatorType rotatorType = RotatorType.get(side);

		lock.lock();
		removeWorkingRotatorInfo(rotatorType);
		if (workingRotatorsCount == 0) {
			logger.info("JESTEM obracaczem!!: budze was!" + rotatorType);
			condition.signalAll();
		}
		logger.info("WYCHODZE OBRACAACZ!: " + rotatorType);
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
		if (shouldInspectorWait()) {
			++waitingInspectorsCount;
			logger.info(Thread.currentThread().getName() + ": "
					+ "Inspector " + " waiting. "
					+ "Waiting inspectors: " + waitingInspectorsCount);

			try {
				do  {
					condition.await();
				} while (workingRotatorsCount > 0);
			} catch (InterruptedException e) {
				--waitingInspectorsCount;
				if (workingRotatorsCount == 0 && workingInspectorsCount == 0) {
					condition.signalAll();
				}
				lock.unlock();
				throw e;
			}

			--waitingInspectorsCount;
			logger.info(Thread.currentThread().getName() + ": "
					+ "Inspector " + " awaken. "
					+ "Waiting inspectors: " + waitingInspectorsCount);
		}
		++workingInspectorsCount;
		lock.unlock();
	}

	public void onInspectorExit() throws InterruptedException {
		lock.lock();
		--workingInspectorsCount;
		if (workingInspectorsCount == 0) {
			logger.info("JESTEM ogladaczem budze was!!: ");
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

	private boolean shouldRotatorWait(RotatorType rotatorType) {
		return workingInspectorsCount > 0
				|| waitingInspectorsCount > 0
				|| (workingRotatorType != null && workingRotatorType != rotatorType)
				|| waitingRotatorsTotalCount > 0;
	}

	private boolean shouldInspectorWait() {
		// Czeka jeśli ktoś obraca lub chce obracać.
		return workingRotatorsCount > 0 || waitingRotatorsTotalCount > 0;
	}

	private Lock getRotationLayerLock(int side, int layer) {
		if (side == 0 || side == 1 || side == 2) {
			return rotationLayersLocks[layer];
		} else {
			return rotationLayersLocks[size - 1 - layer];
		}
	}

}
