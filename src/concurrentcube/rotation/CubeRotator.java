package concurrentcube.rotation;

import static concurrentcube.structure.SideType.BACK;
import static concurrentcube.structure.SideType.BOTTOM;
import static concurrentcube.structure.SideType.FRONT;
import static concurrentcube.structure.SideType.LEFT;
import static concurrentcube.structure.SideType.RIGHT;
import static concurrentcube.structure.SideType.TOP;

import java.util.function.BiConsumer;

import concurrentcube.util.AccessManager;
import concurrentcube.structure.CubeState;
import concurrentcube.structure.SideType;

/**
 * Zajmuje się obracaniem warstwy kostki.
 * Czeka na swoją kolej, jeśli ktoś ogląda kostkę lub chce oglądać
 * lub ktoś obraca w kolidujący sposób lub ktoś czeka na możliwość obrotu.
 * */
public class CubeRotator {

	private final CubeState state;
	private final BiConsumer<Integer, Integer> beforeRotation;
	private final BiConsumer<Integer, Integer> afterRotation;
	private final AccessManager accessManager;

	public CubeRotator(CubeState state, BiConsumer<Integer, Integer> beforeRotation,
			BiConsumer<Integer, Integer> afterRotation, AccessManager accessManager) {
		this.state = state;
		this.beforeRotation = beforeRotation;
		this.afterRotation = afterRotation;
		this.accessManager = accessManager;
	}

	public void rotate(int side, int layer) throws InterruptedException {
		accessManager.onRotatorEntry(side, layer);
		beforeRotation.accept(side, layer);
		rotateCube(side, layer);
		afterRotation.accept(side, layer);
		accessManager.onRotatorExit(side, layer);
	}

	private void rotateCube(int side, int layer) {
		SideType sideType = SideType.from(side);
		switch (sideType) {
			case TOP:
				rotateFromTop(layer);
				break;
			case LEFT:
				rotateFromLeft(layer);
				break;
			case FRONT:
				rotateFromFront(layer);
				break;
			case RIGHT:
				rotateFromRight(layer);
				break;
			case BACK:
				rotateFromBack(layer);
				break;
			case BOTTOM:
				rotateFromBottom(layer);
				break;
		}

		if (layer == 0) {
			rotateSideClockwise(side);
		} else if (layer == state.size() - 1) {
			rotateSideCounterClockwise(SideType.getOpposite(side));
		}
	}

	private void rotateFromFront(int layer) {
		int size = state.size();
		int[] topRow = state.getRow(TOP, size - 1 - layer);
		int[] leftColumn = state.getColumn(LEFT, size - 1- layer);
		int[] bottomRow = state.getRow(BOTTOM, layer);
		int[] rightColumn = state.getColumn(RIGHT, layer);

		state.setRow(TOP, reverse(leftColumn), size - 1 - layer);
		state.setColumn(LEFT, bottomRow, size - 1- layer);
		state.setRow(BOTTOM, reverse(rightColumn), layer);
		state.setColumn(RIGHT, topRow, layer);
	}

	private void rotateFromBack(int layer) {
		int size = state.size();
		int[] topRow = state.getRow(TOP, layer);
		int[] rightColumn = state.getColumn(RIGHT, size - 1 - layer);
		int[] bottomRow = state.getRow(BOTTOM, size - 1 - layer);
		int[] leftColumn = state.getColumn(LEFT, layer);

		state.setRow(TOP, rightColumn, layer);
		state.setColumn(LEFT, reverse(topRow), layer);
		state.setRow(BOTTOM, leftColumn, size - 1 - layer);
		state.setColumn(RIGHT, reverse(bottomRow), size - 1 - layer);
	}

	private void rotateFromTop(int layer) {
		int size = state.size();
		int[] backRow = state.getRow(BACK, layer);
		int[] rightRow = state.getRow(RIGHT, layer);
		int[] frontRow = state.getRow(FRONT, layer);
		int[] leftRow = state.getRow(LEFT, layer);

		state.setRow(BACK, leftRow, layer);
		state.setRow(RIGHT, backRow, layer);
		state.setRow(FRONT, rightRow, layer);
		state.setRow(LEFT, frontRow, layer);
	}

	private void rotateFromBottom(int layer) {
		int size = state.size();
		int[] frontRow = state.getRow(FRONT, size - 1 - layer);
		int[] rightRow = state.getRow(RIGHT, size - 1 - layer);
		int[] backRow = state.getRow(BACK, size - 1 - layer);
		int[] leftRow = state.getRow(LEFT, size - 1 - layer);

		state.setRow(FRONT, leftRow, size - 1 - layer);
		state.setRow(RIGHT, frontRow, size - 1 - layer);
		state.setRow(BACK, rightRow, size - 1 - layer);
		state.setRow(LEFT, backRow, size - 1 - layer);
	}

	private void rotateFromLeft(int layer) {
		int size = state.size();
		int[] topColumn = state.getColumn(TOP, layer);
		int[] frontColumn = state.getColumn(FRONT, layer);
		int[] bottomColumn = state.getColumn(BOTTOM, layer);
		int[] backColumn = state.getColumn(BACK, size - 1 - layer);

		state.setColumn(TOP, reverse(backColumn), layer);
		state.setColumn(FRONT, topColumn, layer);
		state.setColumn(BOTTOM, frontColumn, layer);
		state.setColumn(BACK, reverse(bottomColumn), size - 1 - layer);
	}

	private void rotateFromRight(int layer) {
		int size = state.size();
		int[] topColumn = state.getColumn(TOP, size - 1 - layer);
		int[] backColumn = state.getColumn(BACK, layer);
		int[] bottomColumn = state.getColumn(BOTTOM, size - 1 - layer);
		int[] frontColumn = state.getColumn(FRONT, size - 1 - layer);

		state.setColumn(TOP, frontColumn, size - 1 - layer);
		state.setColumn(BACK, reverse(topColumn), layer);
		state.setColumn(BOTTOM, backColumn, size - 1- layer);
		state.setColumn(FRONT, reverse(bottomColumn), size - 1 - layer);
	}

	private void rotateSideClockwise(int side) {
		int size = state.size();
		SideType sideType = SideType.from(side);

		int[][] rows = new int[size][size];
		for (int i = 0; i < size; ++i) {
			rows[i] = state.getRow(sideType, i);
		}

		for (int i = 0; i < size; ++i) {
			state.setColumn(sideType, rows[i], size - 1 - i);
		}
	}

	private void rotateSideCounterClockwise(int side) {
		int size = state.size();
		SideType sideType = SideType.from(side);

		int[][] columns = new int[size][size];
		for (int i = 0; i < size; ++i) {
			columns[i] = state.getColumn(sideType, i);
		}

		for (int i = 0; i < size; ++i) {
			state.setRow(sideType, columns[i], size - 1 - i);
		}
	}

	private int[] reverse(int[] arr) {
		int[] reversed = new int[arr.length];
		for (int i = 0; i < arr.length; ++i) {
			reversed[i] = arr[arr.length - 1 - i];
		}
		return reversed;
	}

}
