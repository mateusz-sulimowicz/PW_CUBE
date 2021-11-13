package concurrentcube.structure;

public enum SideType {

	TOP,
	LEFT,
	FRONT,
	RIGHT,
	BACK,
	BOTTOM;

	public static SideType from(int side) {
		switch (side) {
			case 0:
				return TOP;
			case 1:
				return LEFT;
			case 2:
				return FRONT;
			case 3:
				return RIGHT;
			case 4:
				return BACK;
			case 5:
				return BOTTOM;
			default:
				throw new EnumConstantNotPresentException(SideType.class, "ERROR");
		}
	}

	public static int getOpposite(int side) {
		switch (side) {
			case 0:
				return 5;
			case 1:
				return 3;
			case 2:
				return 4;
			case 3:
				return 1;
			case 4:
				return 2;
			case 5:
				return 0;
			default:
				throw new IllegalArgumentException();
		}
	}

}
