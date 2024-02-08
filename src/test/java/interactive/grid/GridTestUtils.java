package interactive.grid;

import interactive.TestFXUtils.GridCell;
import interactive.TestFXUtils.SimpleCell;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.mfxcore.utils.GridUtils;
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

import static interactive.TestFXUtils.counter;
import static org.junit.jupiter.api.Assertions.*;

public class GridTestUtils {

	//================================================================================
	// Constructors
	//================================================================================
	private GridTestUtils() {}

	//================================================================================
	// Methods
	//================================================================================
	static void assertState(VFXGrid<Integer, SimpleCell> grid, IntegerRange rowsRange, IntegerRange columnsRange) {
		VFXGridState<Integer, SimpleCell> state = grid.getState();
		VFXGridHelper<Integer, SimpleCell> helper = grid.getHelper();
		int nColumns = helper.maxColumns();
		if (Utils.INVALID_RANGE.equals(rowsRange) || Utils.INVALID_RANGE.equals(columnsRange)) {
			assertEquals(VFXGridState.EMPTY, state);
			return;
		}

		assertEquals(helper.rowsRange(), rowsRange);
		assertEquals(helper.columnsRange(), columnsRange);
		assertEquals(helper.totalCells(), state.size());

		SequencedMap<Integer, SimpleCell> cells = state.getCellsByIndexUnmodifiable();
		ObservableList<Integer> items = grid.getItems();
		int i = 0, j = 0;
		for (Integer rIdx : rowsRange) {
			for (Integer cIdx : columnsRange) {
				int linear = GridUtils.subToInd(nColumns, rIdx, cIdx);
				SimpleCell cell = cells.get(linear);
				if (linear >= items.size()) {
					assertNull(cell);
				} else {
					try {
						assertNotNull(cell);
					} catch (AssertionFailedError error) {
						System.err.printf("Null cell for indexes L/R/C: %d/%d/%d%n", linear, rIdx, cIdx);
						throw error;
					}

					assertEquals(linear, cell.getIndex());
					assertEquals(items.get(linear), cell.getItem());
					assertPosition(grid, i, j, cell);
				}
				j++;
			}
			i++;
			j = 0;
		}
	}

	static void assertLength(VFXGrid<Integer, SimpleCell> grid, double vLength, double hLength) {
		VFXGridHelper<Integer, SimpleCell> helper = grid.getHelper();
		assertEquals(vLength, helper.getEstimateHeight());
		assertEquals(hLength, helper.getEstimateWidth());
	}

	static void assertPosition(VFXGrid<Integer, SimpleCell> grid, int rIdxIt, int cIdxIt, SimpleCell cell) {
		VFXGridHelper<Integer, SimpleCell> helper = grid.getHelper();
		Bounds bounds = cell.toNode().getBoundsInParent();
		double cw = helper.getTotalCellSize().getWidth();
		double ch = helper.getTotalCellSize().getHeight();
		double x = cw * cIdxIt;
		double y = ch * rIdxIt;
		try {
			assertEquals(x, bounds.getMinX());
			assertEquals(y, bounds.getMinY());
		} catch (AssertionFailedError err) {
			System.err.printf("Failed position assertion for cell at [%d:%d] with item %d%n", rIdxIt, cIdxIt, cell.getItem());
			throw err;
		}
	}

	//================================================================================
	// Internal Classes
	//================================================================================
	public static class Grid extends VFXGrid<Integer, SimpleCell> {
		public Grid(ObservableList<Integer> items) {
			this(items, GridCell::new);
		}

		public Grid(ObservableList<Integer> items, Function<Integer, SimpleCell> cellFactory) {
			super(items, cellFactory);
			setBufferSize(BufferSize.SMALL);
		}

		@Override
		public void setCellFactory(Function<Integer, SimpleCell> cellFactory) {
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
