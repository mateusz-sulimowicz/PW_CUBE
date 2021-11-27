package concurrentcube.inspection;

import concurrentcube.util.AccessManager;
import concurrentcube.structure.CubeState;

public class CubeInspector {

	private final CubeState cube;
	private final Runnable beforeShowing;
	private final Runnable afterShowing;
	private final AccessManager accessManager;

	public CubeInspector(CubeState cube, Runnable beforeShowing, Runnable afterShowing,
			AccessManager accessManager) {
		this.cube = cube;
		this.beforeShowing = beforeShowing;
		this.afterShowing = afterShowing;
		this.accessManager = accessManager;
	}

	public String show() throws InterruptedException {
		String serializedCube;
		accessManager.onInspectorEntry();
		beforeShowing.run();
		serializedCube = cube.toString();
		afterShowing.run();
		accessManager.onInspectorExit();

		return serializedCube;
	}

}
