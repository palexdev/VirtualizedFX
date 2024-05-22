package interactive;

import assets.User;
import io.github.palexdev.mfxcore.controls.Label;
import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.virtualizedfx.cells.CellBaseBehavior;
import io.github.palexdev.virtualizedfx.cells.VFXCellBase;
import io.github.palexdev.virtualizedfx.events.VFXContainerEvent;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import org.testfx.api.FxToolkit;

import java.util.concurrent.TimeoutException;

import static interactive.table.TableTestUtils.rowsCounter;
import static io.github.palexdev.mfxcore.events.WhenEvent.intercept;
import static io.github.palexdev.mfxcore.observables.When.onInvalidated;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestFXUtils {
	//================================================================================
	// Static Properties
	//================================================================================
	public static final Counter counter = new Counter();
	public static final double FP_ASSERTIONS_DELTA = 2.0;

	//================================================================================
	// Constructors
	//================================================================================
	private TestFXUtils() {}

	//================================================================================
	// Methods
	//================================================================================
	public static void assertCounter(int created, int layouts, int ixUpdates, int itUpdates, int deCached, int cached, int disposed) {
		assertEquals(created, counter.created);
		assertEquals(layouts, counter.getLayoutCnt());
		assertEquals(ixUpdates, counter.getUpdIndexCnt());
		assertEquals(itUpdates, counter.getUpdItemCnt());
		assertEquals(deCached, counter.getFromCache());
		assertEquals(cached, counter.getToCache());
		assertEquals(disposed, counter.getDisposed());
		counter.reset();
	}

	public static void resetCounters() {
		counter.reset();
		rowsCounter.reset();
	}

	public static StackPane setupStage() {
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

	//================================================================================
	// Inner Classes
	//================================================================================
	public static class SimpleCell extends VFXCellBase<Integer> {
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
		public void dispose() {
			counter.disposed();
		}

		@Override
		protected SkinBase<?, ?> buildSkin() {
			return new CellSkin<>(this);
		}
	}

	public static class GridCell extends SimpleCell {
		public GridCell(Integer item) {super(item);}


		@Override
		protected SkinBase<?, ?> buildSkin() {
			return new CellSkin<>(this) {
				{
					label.setAlignment(Pos.CENTER);
				}
			};
		}
	}

	public static class CellSkin<T> extends SkinBase<VFXCellBase<T>, CellBaseBehavior<T>> {
		protected final Label label;

		public CellSkin(VFXCellBase<T> cell) {
			super(cell);

			label = new Label();
			label.setStyle("-fx-border-color: red");

			update();
			// First update counts as both
			counter.index();
			counter.item();

			addListeners();
			getChildren().add(label);
		}

		protected void addListeners() {
			VFXCellBase<T> cell = getSkinnable();
			listeners(
				onInvalidated(cell.indexProperty())
					.then(v -> {
						counter.index();
						update();
					}),
				onInvalidated(cell.itemProperty())
					.then(v -> {
						counter.item();
						update();
					})
			);
			events(
				intercept(cell, VFXContainerEvent.UPDATE)
					.process(e -> {
						update();
						e.consume();
					})
			);

		}

		protected void update() {
			VFXCellBase<T> cell = getSkinnable();
			int index = cell.getIndex();
			T item = cell.getItem();
			label.setText(toString(index, item));
		}

		protected String toString(int idx, T item) {
			return "Index: %d Item: %s".formatted(
				idx,
				item != null ? item.toString() : ""
			);
		}

		@Override
		protected void initBehavior(CellBaseBehavior<T> behavior) {}

		@Override
		protected void layoutChildren(double x, double y, double w, double h) {
			label.resizeRelocate(x, y, w, h);
		}
	}

	public static class UserCell extends VFXCellBase<User> {
		public UserCell(User item) {super(item);}

		@Override
		public void onDeCache() {
			counter.fCache();
		}

		@Override
		public void onCache() {
			counter.tCache();
		}

		@Override
		public void dispose() {
			counter.disposed();
		}

		@Override
		protected SkinBase<?, ?> buildSkin() {
			return new CellSkin<>(this);
		}
	}

	public static class Counter {
		public int created = 0;
		private int layoutCnt = 0;
		private int updIndexCnt = 0;
		private int updItemCnt = 0;
		private int fromCache = 0;
		private int toCache = 0;
		private int disposed = 0;

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

		public void disposed() {disposed++;}

		public int getDisposed() {
			return disposed;
		}

		public void reset() {
			created = 0;
			layoutCnt = 0;
			updIndexCnt = 0;
			updItemCnt = 0;
			fromCache = 0;
			toCache = 0;
			disposed = 0;
		}
	}
}
