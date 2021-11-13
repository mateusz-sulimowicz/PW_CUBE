package concurrentcube.structure;

public class CubeSideState {

	private final int[][] squares;
	private final int size;

	public CubeSideState(int side, int size) {
		this.size = size;
		squares = new int[size][size];
		for (int i = 0; i < size; ++i) {
			for (int j = 0; j < size; ++j) {
				squares[i][j] = side;
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder serializedSide = new StringBuilder();
		for (int i = 0; i < size; ++i) {
			for (int j = 0; j < size; ++j) {
				serializedSide.append(squares[i][j]);
			}
		}
		return serializedSide.toString();
	}

	public int[] getRow(int rowNumber) {
		int[] row = new int[size];
		for (int i = 0; i < size; ++i) {
			row[i] = squares[rowNumber][i];
		}
		return row;
	}

	public int[] getColumn(int columnNumber) {
		int[] column = new int[size];
		for (int i = 0; i < size; ++i) {
			column[i] = squares[i][columnNumber];
		}
		return column;
	}

	public void setRow(int[] row, int rowNumber) {
		for (int i = 0; i < size; ++i) {
			squares[rowNumber][i] = row[i];
		}
	}

	public void setColumn(int[] column, int columnNumber) {
		for (int i = 0; i < size; ++i) {
			squares[i][columnNumber] = column[i];
		}
	}


}
