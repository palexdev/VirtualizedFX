package jmh;

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.utils.GridUtils;
import io.github.palexdev.virtualizedfx.cells.base.Cell;
import javafx.scene.Node;

import java.util.SequencedMap;
import java.util.TreeMap;

public class JMHCommons {

	//================================================================================
	// Internal Classes
	//================================================================================
	public static class MockState {
		final IntegerRange range;
		final SequencedMap<Integer, MockCell> cells = new TreeMap<>();

		public MockState(IntegerRange range) {this.range = range;}

		public static MockState generate(IntegerRange range) {
			MockState s = new MockState(range);
			range.forEach(i -> s.addCell(i, new MockCell(i)));
			return s;
		}

		public void addCell(Integer index, MockCell cell) {
			cells.put(index, cell);
		}

		public MockCell removeCell(Integer index) {
			return cells.remove(index);
		}

		public boolean isEmpty() {
			return cells.isEmpty();
		}
	}

	public static class MockState2D {
		final int nColumns;
		final IntegerRange rRange;
		final IntegerRange cRange;
		final SequencedMap<Integer, MockCell> cells = new TreeMap<>();

		public MockState2D(int nColumns, IntegerRange rRange, IntegerRange cRange) {
			this.nColumns = nColumns;
			this.rRange = rRange;
			this.cRange = cRange;
		}

		public static MockState2D generate2D(int nColumns, IntegerRange rRange, IntegerRange cRange) {
			MockState2D s = new MockState2D(nColumns, rRange, cRange);
			for (Integer rIdx : rRange) {
				for (Integer cIdx : cRange) {
					int linear = GridUtils.subToInd(nColumns, rIdx, cIdx);
					s.addCell(linear, new MockCell(linear));
				}
			}
			return s;
		}

		public void addCell(Integer index, MockCell cell) {
			cells.put(index, cell);
		}

		public MockCell removeCell(Integer index) {
			return cells.remove(index);
		}

		public boolean isEmpty() {
			return cells.isEmpty();
		}
	}

	public static class MockCell implements Cell<Integer> {
		private Integer index = -1;
		private Integer item;
		private String text = "";

		public MockCell(Integer item) {
			updateItem(item);
		}

		void render() {
			text = "Index/Item: %s/%s".formatted(index, item);
		}

		@Override
		public Node toNode() {
			return null;
		}

		@Override
		public void updateIndex(int index) {
			this.index = index;
			render();
		}

		@Override
		public void updateItem(Integer item) {
			this.item = item;
			render();
		}
	}
}
