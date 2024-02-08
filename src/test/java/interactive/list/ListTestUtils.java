package interactive.list;

import interactive.TestFXUtils.SimpleCell;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.virtualizedfx.list.VFXList;
import io.github.palexdev.virtualizedfx.list.VFXListHelper;
import io.github.palexdev.virtualizedfx.list.VFXListSkin;
import io.github.palexdev.virtualizedfx.list.VFXListState;
import io.github.palexdev.virtualizedfx.list.paginated.VFXPaginatedList;
import io.github.palexdev.virtualizedfx.list.paginated.VFXPaginatedListSkin;
import io.github.palexdev.virtualizedfx.utils.Utils;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import org.opentest4j.AssertionFailedError;

import java.util.Map;
import java.util.function.Function;

import static interactive.TestFXUtils.counter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ListTestUtils {
	//================================================================================
	// Constructors
	//================================================================================
	private ListTestUtils() {}

	//================================================================================
	// Methods
	//================================================================================
	static void assertState(VFXList<Integer, SimpleCell> list, IntegerRange range) {
		VFXListState<Integer, SimpleCell> state = list.getState();
		VFXListHelper<Integer, SimpleCell> helper = list.getHelper();
		if (Utils.INVALID_RANGE.equals(range)) {
			assertEquals(VFXListState.EMPTY, state);
			return;
		}

		assertEquals(helper.range(), range);
		assertEquals(helper.totalNum(), state.size());

		Map<Integer, SimpleCell> cells = state.getCellsByIndexUnmodifiable();
		ObservableList<Integer> items = list.getItems();
		for (Integer index : range) {
			SimpleCell cell = cells.get(index);
			try {
				assertNotNull(cell);
			} catch (AssertionFailedError error) {
				// For debug purposes
				System.err.println("Null cell for index: " + index);
				throw error;
			}
			assertEquals(index, cell.getIndex());
			assertEquals(items.get(index), cell.getItem());
			assertPosition(list, index - range.getMin(), cell);
		}
	}

	static void assertPosition(VFXList<Integer, SimpleCell> list, int iteration, SimpleCell cell) {
		Orientation o = list.getOrientation();
		Function<Bounds, Double> inParentPos = (o == Orientation.VERTICAL) ? Bounds::getMinY : Bounds::getMinX;
		double pos = iteration * list.getHelper().getTotalCellSize();
		assertEquals(pos, inParentPos.apply(cell.toNode().getBoundsInParent()));
	}

	//================================================================================
	// Internal Classes
	//================================================================================
	public static class List extends VFXList<Integer, SimpleCell> {
		public List(ObservableList<Integer> items) {
			this(items, SimpleCell::new);
		}

		public List(ObservableList<Integer> items, Function<Integer, SimpleCell> cellFactory) {
			super(items, cellFactory);
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
			return new VFXListSkin<>(this) {
				@Override
				protected void onLayoutCompleted(boolean done) {
					super.onLayoutCompleted(done);
					if (done) counter.layout();
				}
			};
		}
	}

	public static class PList extends VFXPaginatedList<Integer, SimpleCell> {
		public PList(ObservableList<Integer> items) {
			this(items, SimpleCell::new);
		}

		public PList(ObservableList<Integer> items, Function<Integer, SimpleCell> cellFactory) {
			super(items, cellFactory);
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
			return new VFXPaginatedListSkin<>(this) {
				@Override
				protected void onLayoutCompleted(boolean done) {
					super.onLayoutCompleted(done);
					if (done) counter.layout();
				}
			};
		}
	}

}

