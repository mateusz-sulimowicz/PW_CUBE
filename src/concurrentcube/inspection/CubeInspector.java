package concurrentcube.inspection;

import concurrentcube.Cube;
import concurrentcube.structure.CubeState;

public class CubeInspector {

	private final CubeState cube;
	private final Runnable beforeShowing;
	private final Runnable afterShowing;
	private final Cube.AccessManager accessManager;

	public CubeInspector(CubeState cube, Runnable beforeShowing, Runnable afterShowing,
			Cube.AccessManager accessManager) {
		this.cube = cube;
		this.beforeShowing = beforeShowing;
		this.afterShowing = afterShowing;
		this.accessManager = accessManager;
	}

	public String show() throws InterruptedException {
		String serializedCube;

		try {
			accessManager.onInspectorEntry();
			beforeShowing.run();
			serializedCube = cube.toString();
			afterShowing.run();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw e;
		} finally {
			accessManager.onInspectorExit();
		}

		return serializedCube;
	}

}
