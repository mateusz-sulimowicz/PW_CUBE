package concurrentcube;

import java.util.function.BiConsumer;

import concurrentcube.inspection.CubeInspector;
import concurrentcube.rotation.CubeRotator;
import concurrentcube.structure.CubeState;
import concurrentcube.util.AccessManager;

/**
 * Represents Rubik's cube that
 * enables executing non-colliding 
 * rotations concurrently.
 */
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
		AccessManager accessManager = new AccessManager(size);
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

}
