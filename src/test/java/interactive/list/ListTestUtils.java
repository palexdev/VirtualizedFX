package interactive.list;

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.controls.Label;
import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.virtualizedfx.cells.CellBase;
import io.github.palexdev.virtualizedfx.cells.CellBaseBehavior;
import io.github.palexdev.virtualizedfx.list.VirtualizedList;
import io.github.palexdev.virtualizedfx.list.VirtualizedListHelper;
import io.github.palexdev.virtualizedfx.list.VirtualizedListSkin;
import io.github.palexdev.virtualizedfx.list.VirtualizedListState;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import org.opentest4j.AssertionFailedError;
import org.testfx.api.FxToolkit;

import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ListTestUtils {
	//================================================================================
	// Static Properties
	//================================================================================
	static final Counter counter = new Counter();

	//================================================================================
	// Constructors
	//================================================================================
	private ListTestUtils() {}

	//================================================================================
	// Methods
	//================================================================================
	static StackPane setupStage() {
		StackPane pane = new StackPane();
		try {
			Scene scene = new Scene(pane, 400, 400);
			FxToolkit.setupStage(s -> {
				s.setWidth(400);
				s.setHeight(400);
				s.setScene(scene);
			});
		} catch (TimeoutException e) {
			throw new RuntimeException(e);
		}
		return pane;
	}

	static void assertState(VirtualizedList<Integer, SimpleCell> list, IntegerRange range) {
		VirtualizedListState<Integer, SimpleCell> state = list.getState();
		VirtualizedListHelper<Integer, SimpleCell> helper = list.getHelper();
		if (IntegerRange.of(-1).equals(range)) {
			assertEquals(VirtualizedListState.EMPTY, state);
			return;
		}

		assertEquals(helper.range(), range);
		assertEquals(helper.totalNum(), range.diff() + 1);

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

	static void assertPosition(VirtualizedList<Integer, SimpleCell> list, int iteration, SimpleCell cell) {
		Orientation o = list.getOrientation();
		Function<Bounds, Double> inParentPos = (o == Orientation.VERTICAL) ? Bounds::getMinY : Bounds::getMinX;
		double pos = iteration * list.getHelper().getTotalCellSize();
		assertEquals(pos, inParentPos.apply(cell.toNode().getBoundsInParent()));
	}

	static void assertCounter(int created, int layouts, int ixUpdates, int itUpdates, int deCached, int cached) {
		assertEquals(created, counter.created);
		assertEquals(layouts, counter.layoutCnt);
		assertEquals(ixUpdates, counter.updIndexCnt);
		assertEquals(itUpdates, counter.updItemCnt);
		assertEquals(deCached, counter.fromCache);
		assertEquals(cached, counter.toCache);
		counter.reset();
	}

	//================================================================================
	// Internal Classes
	//================================================================================
	public static class List extends VirtualizedList<Integer, SimpleCell> {
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
			return new VirtualizedListSkin<>(this) {
				@Override
				protected void onLayoutCompleted(boolean done) {
					super.onLayoutCompleted(done);
					if (done) counter.layout();
				}
			};
		}
	}

	public static class SimpleCell extends CellBase<Integer> {
		public SimpleCell() {}

		public SimpleCell(Integer item) {super(item);}

		@Override
		public void onDeCache() {
			counter.fCache();
		}

		@Override
		public void onCache() {
			counter.tCache();
		}

		@Override
		protected SkinBase<?, ?> buildSkin() {
			return new SkinBase<>(this) {
				final Label label = new Label();

				{
					CellBase<Integer> cell = getSkinnable();
					listeners(
						When.onInvalidated(cell.itemProperty())
							.then(this::byItem)
							.executeNow(),
						When.onInvalidated(cell.indexProperty())
							.then(this::byIndex)
							.executeNow()
					);
					label.setStyle("-fx-border-color: red");
					getChildren().add(label);
				}

				private void byItem(Integer it) {
					counter.item();
					int idx = getIndex();
					label.setText("Index: %d Item: %s".formatted(
						idx,
						it != null ? it.toString() : ""
					));
				}

				private void byIndex(Number index) {
					counter.index();
					Integer it = getItem();
					label.setText("Index: %d Item: %s".formatted(
						index.intValue(),
						it != null ? it.toString() : ""
					));
				}

				@Override
				protected void initBehavior(CellBaseBehavior<Integer> behavior) {}

				@Override
				protected void layoutChildren(double x, double y, double w, double h) {
					label.resizeRelocate(x, y, w, h);
				}
			};
		}
	}

	public static class Counter {
		public int created = 0;
		private int layoutCnt = 0;
		private int updIndexCnt = 0;
		private int updItemCnt = 0;
		private int fromCache = 0;
		private int toCache = 0;

		public void created() {created++;}

		public int getCreated() {
			return created;
		}

		public void layout() {layoutCnt++;}

		public int getLayoutCnt() {
			return layoutCnt;
		}

		public void index() {updIndexCnt++;}

		public int getUpdIndexCnt() {
			return updIndexCnt;
		}

		public void item() {updItemCnt++;}

		public int getUpdItemCnt() {
			return updItemCnt;
		}

		public void fCache() {fromCache++;}

		public int getFromCache() {
			return fromCache;
		}

		public void tCache() {toCache++;}

		public int getToCache() {
			return toCache;
		}

		public void reset() {
			created = 0;
			layoutCnt = 0;
			updIndexCnt = 0;
			updItemCnt = 0;
			fromCache = 0;
			toCache = 0;
		}
	}
}

