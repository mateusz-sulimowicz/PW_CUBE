package concurrentcube.structure;

public class CubeState {

	private final static int CUBE_SIDES = 6;

	CubeSideState[] sideStates;
	int size;

	public CubeState(int size) {
		this.size = size;
		sideStates = new CubeSideState[CUBE_SIDES];
		for (int i = 0; i < CUBE_SIDES; ++i) {
			sideStates[i] = new CubeSideState(i, size);
		}
	}

	@Override
	public String toString() {
		StringBuilder serializedCube = new StringBuilder();
		for (var side : sideStates) {
			serializedCube.append(side.toString());
		}
		return serializedCube.toString();
	}

	public int[] getRow(SideType side, int rowNumber) {
		return sideStates[side.ordinal()].getRow(rowNumber);
	}

	public int[] getColumn(SideType side, int columnNumber) {
		return sideStates[side.ordinal()].getColumn(columnNumber);
	}

	public void setRow(SideType side, int[] newRow, int rowNumber) {
		sideStates[side.ordinal()].setRow(newRow, rowNumber);
	}

	public void setColumn(SideType side, int[] newColumn, int columnNumber) {
		sideStates[side.ordinal()].setColumn(newColumn, columnNumber);
	}

	public int size() {
		return size;
	}


}
