package concurrentcube.rotation;

public enum RotatorType {

	XY,
	YZ,
	XZ;

	public static RotatorType get(int side) {
		switch (side) {
			case 0:
			case 5:
				return YZ;
			case 1:
			case 3:
				return XZ;
			default:
				return XY;
		}

	}


}
