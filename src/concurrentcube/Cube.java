package concurrentcube;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
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

		private final Semaphore guard = new Semaphore(1);

		private final Semaphore waitingRotatorsRepresentatives = new Semaphore(0);
		private int waitingRotatorsTotalCount;
		private final Map<RotatorType, Semaphore> waitingRotators;
		private final Map<RotatorType, Integer> waitingRotatorCounts;

		private int workingRotatorsCount;
		private RotatorType workingRotatorType;

		private final Semaphore waitingInspectors = new Semaphore(0);
		private int waitingInspectorsCount;
		private int workingInspectorsCount;

		private final Semaphore[] rotationLayersGuards;

		public AccessManager() {
			this.waitingRotators = new HashMap<>();
			this.waitingRotatorCounts = new HashMap<>();

			for (var rotatorType : RotatorType.values()) {
				waitingRotators.put(rotatorType, new Semaphore(0));
				waitingRotatorCounts.put(rotatorType, 0);
			}

			rotationLayersGuards = new Semaphore[size];
			for (int i = 0; i < size; ++i) {
				rotationLayersGuards[i] = new Semaphore(1);
			}
		}

		public void onBeforeRotation(int side, int layer) throws InterruptedException {
			RotatorType rotatorType = RotatorType.get(side);

			guard.acquire();
			if (shouldRotatorWait(rotatorType)) {
				addWaitingRotatorInfo(rotatorType);
				guard.release();
				waitBeforeRotationAccess(rotatorType);
				// dziedziczenie ochrony
				removeWaitingRotatorInfo(rotatorType);
			}
			addWorkingRotatorInfo(rotatorType);
			wakeNextWaitingRotator(rotatorType);
			getRotationLayerGuard(side, layer).acquire();
		}


		public void onAfterRotation(int side, int layer) throws InterruptedException {
			RotatorType rotatorType = RotatorType.get(side);

			guard.acquire();
			removeWorkingRotatorInfo(rotatorType);
			if (workingRotatorsCount == 0) {
				if (waitingInspectorsCount > 0) {
					// przekazanie ochrony
					waitingInspectors.release();
				} else if (waitingRotatorsTotalCount > 0) {
					// przekazanie ochrony
					waitingRotatorsRepresentatives.release();
				} else {
					// nie ma kogo budzić
					guard.release();
				}
			} else {
				// nie jest ostatnim obracającym
				guard.release();
			}
		}

		public void onBeforeInspection() throws InterruptedException {
			guard.acquire();

		}

		public void onAfterInspection() {

		}

		private boolean shouldRotatorWait(RotatorType rotatorType) {
			return workingInspectorsCount > 0
					|| waitingInspectorsCount > 0
					|| (workingRotatorType != null && workingRotatorType != rotatorType)
					|| areRotatorsWaiting();
		}

		private boolean areRotatorsWaiting() {
			for (var rotator : RotatorType.values()) {
				if (waitingRotatorCounts.get(rotator) > 0) {
					return true;
				}
			}
			return false;
		}

		private void waitBeforeRotationAccess(RotatorType rotatorType) throws InterruptedException {
			if (waitingRotatorCounts.get(rotatorType) == 1) {
				waitingRotatorsRepresentatives.acquire();
			} else {
				waitingRotators.get(rotatorType).acquire();
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
				// przekazanie ochrony
				waitingRotators.get(rotatorType).release();
			} else {
				guard.release(); // nie ma kogo budzić
			}
		}



		private Semaphore getRotationLayerGuard(int side, int layer) {

		}



	}



}
