package concurrentcube.inspection;

import concurrentcube.Cube;
import concurrentcube.structure.CubeState;

public class CubeInspector {

	private final CubeState state;
	private final Runnable beforeShowing;
	private final Runnable afterShowing;
	private final Cube.AccessManager accessManager;

	public CubeInspector(CubeState state, Runnable beforeShowing, Runnable afterShowing,
			Cube.AccessManager accessManager) {
		this.state = state;
		this.beforeShowing = beforeShowing;
		this.afterShowing = afterShowing;
		this.accessManager = accessManager;
	}

	public String show() throws InterruptedException {
		String serializedCube = "";

		accessManager.onInspectorEntry();
		beforeShowing.run();
		serializedCube = state.toString();
		afterShowing.run();
		accessManager.onInspectorExit();

		return serializedCube;
	}

}
