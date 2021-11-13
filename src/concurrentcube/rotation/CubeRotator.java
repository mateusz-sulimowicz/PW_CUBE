package concurrentcube.rotation;

import java.util.function.BiConsumer;

import concurrentcube.Cube;
import concurrentcube.structure.CubeState;

public class CubeRotator {

	private final CubeState state;
	private final BiConsumer<Integer, Integer> beforeRotation;
	private final BiConsumer<Integer, Integer> afterRotation;
	private final Cube.AccessManager accessManager;

	public CubeRotator(CubeState state, BiConsumer<Integer, Integer> beforeRotation,
			BiConsumer<Integer, Integer> afterRotation, Cube.AccessManager accessManager) {
		this.state = state;
		this.beforeRotation = beforeRotation;
		this.afterRotation = afterRotation;
		this.accessManager = accessManager;
	}

	public void rotate(int side, int layer) throws InterruptedException {
		accessManager.onBeforeRotation(side, layer);
		beforeRotation.accept(side, layer);
		rotateCube(side, layer);
		afterRotation.accept(side, layer);
		accessManager.onAfterRotation(side, layer);
	}

	private void rotateCube(int side, int layer) throws InterruptedException {

	}


}
