package interactive.grid;

import cells.TestGridCell;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.mfxcore.utils.GridUtils;
import io.github.palexdev.virtualizedfx.cells.VFXCellBase;
import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import io.github.palexdev.virtualizedfx.enums.BufferSize;
import io.github.palexdev.virtualizedfx.grid.VFXGrid;
import io.github.palexdev.virtualizedfx.grid.VFXGridHelper;
import io.github.palexdev.virtualizedfx.grid.VFXGridSkin;
import io.github.palexdev.virtualizedfx.grid.VFXGridState;
import io.github.palexdev.virtualizedfx.utils.Utils;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import org.opentest4j.AssertionFailedError;

import java.util.SequencedMap;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static utils.TestFXUtils.counter;

public class GridTestUtils {

	//================================================================================
	// Constructors
	//================================================================================
	private GridTestUtils() {}

	//================================================================================
	// Methods
	//================================================================================
	static void assertState(VFXGrid<Integer, VFXCell<Integer>> grid, IntegerRange rowsRange, IntegerRange columnsRange) {
		VFXGridState<Integer, VFXCell<Integer>> state = grid.getState();
		VFXGridHelper<Integer, VFXCell<Integer>> helper = grid.getHelper();
		int nColumns = helper.maxColumns();
		if (Utils.INVALID_RANGE.equals(rowsRange) || Utils.INVALID_RANGE.equals(columnsRange)) {
			assertEquals(VFXGridState.INVALID, state);
			return;
		}

		assertEquals(helper.rowsRange(), rowsRange);
		assertEquals(helper.columnsRange(), columnsRange);
		assertEquals(helper.totalCells(), state.size());

		SequencedMap<Integer, VFXCell<Integer>> cells = state.getCellsByIndexUnmodifiable();
		ObservableList<Integer> items = grid.getItems();
		int i = 0, j = 0;
		for (Integer rIdx : rowsRange) {
			for (Integer cIdx : columnsRange) {
				int linear = GridUtils.subToInd(nColumns, rIdx, cIdx);
				VFXCell<Integer> cell = cells.get(linear);
				if (linear >= items.size()) {
					assertNull(cell);
				} else {
					try {
						assertNotNull(cell);
					} catch (AssertionFailedError error) {
						System.err.printf("Null cell for indexes L/R/C: %d/%d/%d%n", linear, rIdx, cIdx);
						throw error;
					}

					if (cell instanceof VFXCellBase<Integer> cb) {
						assertEquals(linear, cb.getIndex());
						assertEquals(items.get(linear), cb.getItem());
					}
					assertPosition(grid, i, j, cell);
				}
				j++;
			}
			i++;
			j = 0;
		}
	}

	static void assertLength(VFXGrid<Integer, VFXCell<Integer>> grid, double vLength, double hLength) {
		VFXGridHelper<Integer, VFXCell<Integer>> helper = grid.getHelper();
		assertEquals(hLength, helper.getVirtualMaxX());
		assertEquals(vLength, helper.getVirtualMaxY());
	}

	static void assertPosition(VFXGrid<Integer, VFXCell<Integer>> grid, int rIdxIt, int cIdxIt, VFXCell<Integer> cell) {
		VFXGridHelper<Integer, VFXCell<Integer>> helper = grid.getHelper();
		Bounds bounds = cell.toNode().getBoundsInParent();
		double cw = helper.getTotalCellSize().getWidth();
		double ch = helper.getTotalCellSize().getHeight();
		double x = cw * cIdxIt;
		double y = ch * rIdxIt;
		try {
			assertEquals(x, bounds.getMinX());
			assertEquals(y, bounds.getMinY());
		} catch (AssertionFailedError err) {
			Integer item = (cell instanceof VFXCellBase<Integer> cb) ? cb.getItem() : null;
			System.err.printf("Failed position assertion for cell at [%d:%d] with item %d%n", rIdxIt, cIdxIt, item);
			throw err;
		}
	}

	//================================================================================
	// Internal Classes
	//================================================================================
	public static class Grid extends VFXGrid<Integer, VFXCell<Integer>> {
		public Grid(ObservableList<Integer> items) {
			this(items, TestGridCell::new);
		}

		public Grid(ObservableList<Integer> items, Function<Integer, VFXCell<Integer>> cellFactory) {
			super(items, cellFactory);
			setBufferSize(BufferSize.SMALL);
		}

		@Override
		public void setCellFactory(Function<Integer, VFXCell<Integer>> cellFactory) {
			super.setCellFactory(cellFactory.andThen(c -> {
				counter.created();
				return c;
			}));
		}

		@Override
		protected SkinBase<?, ?> buildSkin() {
			return new VFXGridSkin<>(this) {
				@Override
				protected void onLayoutCompleted(boolean done) {
					super.onLayoutCompleted(done);
					if (done) counter.layout();
				}
			};
		}
	}
}
