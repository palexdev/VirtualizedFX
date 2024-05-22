package interactive.table;

import interactive.table.TableTestUtils.EmptyColumn;
import interactive.table.TableTestUtils.Table;
import io.github.palexdev.mfxcore.base.beans.range.DoubleRange;
import io.github.palexdev.mfxcore.utils.RandomUtils;
import io.github.palexdev.virtualizedfx.cells.base.VFXTableCell;
import io.github.palexdev.virtualizedfx.table.ColumnsLayoutCache;
import io.github.palexdev.virtualizedfx.table.VFXTable;
import io.github.palexdev.virtualizedfx.table.VFXTableColumn;
import javafx.collections.ObservableList;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.Set;

import static interactive.table.TableTestUtils.setColumnWidth;
import static model.User.users;
import static org.junit.jupiter.api.Assertions.*;
import static utils.TestFXUtils.setupStage;
import static utils.Utils.setWindowSize;

@ExtendWith(ApplicationExtension.class)
public class ColumnsLayoutCacheTests {

	@Start
	void start(Stage stage) {
		stage.show();
	}

	@Test
	void testColumnsWidthCache(FxRobot robot) {
		StackPane pane = setupStage();
		Table table = new Table(users(50));
		DebuggableCache cache = new DebuggableCache(table);
		robot.interact(() -> pane.getChildren().add(table));

		// Assert cache init
		assertEquals(7, cache.size());
		assertFalse(cache.isValid());
		cache.assertAllInvalid();

		// Validate the cache
		assertEquals(180 * 7, cache.get());
		assertTrue(cache.isValid());
		cache.assertAllValid();

		// Invalidate by increasing table's width
		robot.interact(() -> setWindowSize(table, 1440, -1));
		assertFalse(cache.isValid());
		cache.assertInvalid(6);

		// Validate
		assertEquals(1440, cache.get());
		assertEquals(1440 - 180 * 6, cache.getLastColumnWidth());
		assertTrue(cache.isValid());
		cache.assertAllValid();

		// Change the min size
		robot.interact(() -> table.setColumnsWidth(200));
		assertFalse(cache.isValid());
		cache.assertAllInvalid();

		// Validate
		assertEquals(1440, cache.get());
		assertEquals(1440 - 200 * 6, cache.getLastColumnWidth());
		assertTrue(cache.isValid());
		cache.assertAllValid();

		// Now change the width of a random column (before last)
		int idx = RandomUtils.random.nextInt(0, table.getColumns().size() - 1);
		robot.interact(() -> {
			VFXTableColumn<User, ?> c = table.getColumns().get(idx);
			c.resize(210);
		});
		assertFalse(cache.isValid());
		cache.assertInvalid(idx, 6);

		// Validate
		assertEquals(1440, cache.get());
		assertEquals(1440 - 200 * 6 - 10, cache.getLastColumnWidth());
		assertTrue(cache.isValid());
		cache.assertAllValid();

		// Now change the width of the last column
		robot.interact(() -> {
			VFXTableColumn<User, ?> c = table.getColumns().getLast();
			c.resize(400);
		});
		assertFalse(cache.isValid());
		cache.assertInvalid(6);

		// Validate
		assertEquals((200 * 6 + 10) + 400, cache.get());
		assertEquals(400, cache.getLastColumnWidth());
		assertTrue(cache.isValid());
		cache.assertAllValid();
	}

	@Test
	void testColumnsWidthCacheLCL(FxRobot robot) { // LCL -> ListChangeListener
		StackPane pane = setupStage();
		Table table = new Table(users(50));
		DebuggableCache cache = new DebuggableCache(table);
		robot.interact(() -> {
			setWindowSize(pane, 1440, -1);
			pane.getChildren().add(table);
		});

		// Assert cache init
		assertEquals(7, cache.size());
		assertFalse(cache.isValid());
		cache.assertAllInvalid();

		// Validate before testing list changes
		assertEquals(1440, cache.get());
		assertEquals(1440 - 180 * 6, cache.getLastColumnWidth());
		assertTrue(cache.isValid());
		cache.assertAllValid();

		// Remove random column (except last)
		int rIdx = RandomUtils.random.nextInt(0, table.getColumns().size() - 1);
		robot.interact(() -> table.getColumns().remove(rIdx));
		assertEquals(6, cache.size());
		assertFalse(cache.isValid());
		cache.assertInvalid(5);

		// Validate
		assertEquals(1440, cache.get());
		assertEquals(1440 - 180 * 5, cache.getLastColumnWidth());
		assertTrue(cache.isValid());
		cache.assertAllValid();

		// Add column at random (except last)
		int aIdx = RandomUtils.random.nextInt(0, table.getColumns().size());
		robot.interact(() -> table.getColumns().add(aIdx, new EmptyColumn("Add " + aIdx, 0)));
		assertEquals(7, cache.size());
		assertFalse(cache.isValid());
		cache.assertInvalid(aIdx, 6);

		// Validate
		assertEquals(1440, cache.get());
		assertEquals(1440 - 180 * 6, cache.getLastColumnWidth());
		assertTrue(cache.isValid());
		cache.assertAllValid();

		// Remove last
		robot.interact(() -> table.getColumns().removeLast());
		assertEquals(6, cache.size());
		assertFalse(cache.isValid());
		cache.assertInvalid(5);
		assertEquals(table.getColumns().getLast(), cache.getLastColumn());

		// Validate
		assertEquals(1440, cache.get());
		assertEquals(1440 - 180 * 5, cache.getLastColumnWidth());
		assertTrue(cache.isValid());
		cache.assertAllValid();

		// Add last
		robot.interact(() -> table.getColumns().addLast(new EmptyColumn("New Last", 999)));
		assertEquals(7, cache.size());
		assertFalse(cache.isValid());
		cache.assertInvalid(5, 6);
		assertEquals(table.getColumns().getLast(), cache.getLastColumn());

		// Validate
		assertEquals(1440, cache.get());
		assertEquals(1440 - 180 * 6, cache.getLastColumnWidth());
		assertTrue(cache.isValid());
		cache.assertAllValid();

		// Clear
		robot.interact(() -> table.getColumns().clear());
		assertEquals(0, cache.size());
		assertFalse(cache.isValid());
		assertNull(cache.getLastColumn());

		// Validate
		assertEquals(0, cache.get());
		assertTrue(cache.isValid());

		// Add one
		robot.interact(() -> table.getColumns().add(new EmptyColumn("First", 0)));
		assertEquals(1, cache.size());
		assertFalse(cache.isValid());
		cache.assertInvalid(0);
		assertEquals(table.getColumns().getLast(), cache.getLastColumn());

		// Validate
		assertEquals(1440, cache.get());
		assertEquals(1440, cache.getLastColumnWidth());
		assertTrue(cache.isValid());
		cache.assertAllValid();

		// Add another one
		robot.interact(() -> table.getColumns().addLast(new EmptyColumn("Second", 1)));
		assertEquals(2, cache.size());
		assertFalse(cache.isValid());
		cache.assertAllInvalid();
		assertEquals(table.getColumns().getLast(), cache.getLastColumn());

		// Validate
		assertEquals(1440, cache.get());
		assertEquals(1440 - 180, cache.getLastColumnWidth());
		assertTrue(cache.isValid());
		cache.assertAllValid();

		// Remove first one
		robot.interact(() -> table.getColumns().removeFirst());
		assertEquals(1, cache.size());
		assertFalse(cache.isValid());
		cache.assertAllInvalid();
		assertEquals(table.getColumns().getLast(), cache.getLastColumn());

		// Validate
		assertEquals(1440, cache.get());
		assertEquals(1440, cache.getLastColumnWidth());
		assertTrue(cache.isValid());
		cache.assertAllValid();

		// This time add at 0
		robot.interact(() -> table.getColumns().addFirst(new EmptyColumn("Brrrr", -1)));
		assertEquals(2, cache.size());
		assertFalse(cache.isValid());
		cache.assertAllInvalid();
		assertEquals(table.getColumns().getLast(), cache.getLastColumn());

		// Validate
		assertEquals(1440, cache.get());
		assertEquals(1440 - 180, cache.getLastColumnWidth());
		assertTrue(cache.isValid());
		cache.assertAllValid();
	}

	@Test
	void testColumnsPositionCache(FxRobot robot) {
		StackPane pane = setupStage();
		Table table = new Table(users(50));
		DebuggableCache cache = new DebuggableCache(table);
		robot.interact(() -> {
			setWindowSize(pane, 1440, -1);
			pane.getChildren().add(table);
		});

		// Try getting one in the middle
		assertEquals(540, cache.getColumnPos(3));
		// Since the pos cache is built incrementally, check that previous columns have the right pos too
		assertEquals(0, cache.getColumnPos(0));
		assertEquals(180, cache.getColumnPos(1));
		assertEquals(360, cache.getColumnPos(2));

		// Change the width of column 1
		robot.interact(() -> setColumnWidth(table, 1, 200));
		cache.assertPosInvalid(2, 3);
		assertEquals(0, cache.getColumnPos(0));
		assertEquals(180, cache.getColumnPos(1));
		// Notice how the assertions are scrambled
		assertEquals(560, cache.getColumnPos(3));
		assertEquals(380, cache.getColumnPos(2));

		// Change the width of column 2
		robot.interact(() -> setColumnWidth(table, 2, 205));
		cache.assertPosInvalid(3, 3);
		assertEquals(0, cache.getColumnPos(0));
		assertEquals(180, cache.getColumnPos(1));
		assertEquals(380, cache.getColumnPos(2));
		assertEquals(585, cache.getColumnPos(3));

		// Change the width of an already invalid column
		robot.interact(() -> setColumnWidth(table, 4, 190));
		cache.assertPosInvalid(5, 6);
		assertEquals(1135, cache.getColumnPos(6));
		assertEquals(0, cache.getColumnPos(0));
		assertEquals(180, cache.getColumnPos(1));
		assertEquals(380, cache.getColumnPos(2));
		assertEquals(585, cache.getColumnPos(3));
		assertEquals(765, cache.getColumnPos(4));
		assertEquals(955, cache.getColumnPos(5));

		// Change the columns' minimum width
		robot.interact(() -> table.setColumnsWidth(200.0));
		cache.assertPositionCount(0);
		cache.assertInvalid(0, 3, 4, 5, 6);
		// Validate all
		assertEquals(0, cache.getColumnPos(0));
		assertEquals(200, cache.getColumnPos(1));
		assertEquals(400, cache.getColumnPos(2));
		assertEquals(605, cache.getColumnPos(3));
		assertEquals(805, cache.getColumnPos(4));
		assertEquals(1005, cache.getColumnPos(5));
		assertEquals(1205, cache.getColumnPos(6));

		// Add a column
		robot.interact(() -> table.getColumns().add(3, new EmptyColumn("Add", 999)));
		cache.assertPositionCount(0);
		assertFalse(cache.isValid());
		cache.assertInvalid(3, 7);
		// Validate all
		assertEquals(1405, cache.getColumnPos(7));
		assertEquals(1205, cache.getColumnPos(6));
		assertEquals(1005, cache.getColumnPos(5));
		assertEquals(805, cache.getColumnPos(4));
		assertEquals(605, cache.getColumnPos(3));
		assertEquals(400, cache.getColumnPos(2));
		assertEquals(200, cache.getColumnPos(1));
		assertEquals(0, cache.getColumnPos(0));

		// Remove column
		robot.interact(() -> table.getColumns().remove(3));
		cache.assertPositionCount(0);
		assertFalse(cache.isValid());
		cache.assertInvalid(6);
		// Validate all
		assertEquals(1205, cache.getColumnPos(6));
		assertEquals(1005, cache.getColumnPos(5));
		assertEquals(805, cache.getColumnPos(4));
		assertEquals(605, cache.getColumnPos(3));
		assertEquals(400, cache.getColumnPos(2));
		assertEquals(200, cache.getColumnPos(1));
		assertEquals(0, cache.getColumnPos(0));

		// Clear pos cache and validate partially
		cache.clearPositionCache();
		cache.getColumnPos(3);
		cache.assertPositionCount(4);
		// Now change the column width of an invalid one
		robot.interact(() -> setColumnWidth(table, 5, 240));
		cache.assertPositionCount(4); // Nothing happens as [4, 6] are not present in the cache
	}

	@Test
	void testColumnsVisibilityCache(FxRobot robot) {
		StackPane pane = setupStage();
		Table table = new Table(users(50));
		DebuggableCache cache = new DebuggableCache(table);
		robot.interact(() -> pane.getChildren().add(table));

		// Initial check
		cache.assertVisible(0, 1, 2);
		cache.assertNotVisible(3, 4, 5, 6);

		// Make the table bigger
		robot.interact(() -> setWindowSize(pane, 1024, -1));
		cache.assertVisible(0, 1, 2, 3, 4, 5);
		cache.assertNotVisible(6);

		// Increase the minimum width
		robot.interact(() -> table.setColumnsWidth(240));
		cache.assertVisibilityCount(0);
		cache.assertVisible(0, 1, 2, 3, 4);
		cache.assertNotVisible(5, 6);

		// Increase a column's width by a lot
		robot.interact(() -> setColumnWidth(table, 2, 400));
		cache.assertVisibilityCount(3);
		cache.assertVisible(0, 1, 2, 3);
		cache.assertNotVisible(4, 5, 6);

		// Now scroll to max
		robot.interact(() -> table.setHPos(Double.MAX_VALUE));
		cache.assertVisibilityCount(0);
		cache.assertVisible(2, 3, 4, 5, 6);
		cache.assertNotVisible(0, 1);

		// Decrease table's width
		robot.interact(() -> setWindowSize(pane, 500, -1));
		cache.assertVisible(2, 3, 4);
		cache.assertNotVisible(0, 1, 5, 6);
	}

	@Test
	void testColumnsLayoutCacheDisposal(FxRobot robot) {
		StackPane pane = setupStage();
		Table table = new Table(users(50));
		DebuggableCache cache = new DebuggableCache(table);
		robot.interact(() -> {
			setWindowSize(pane, 1440, -1);
			pane.getChildren().add(table);
		});

		// Validate and Dispose
		cache.get();
		cache.dispose();

		// Try making some change
		robot.interact(() -> table.getColumns().removeLast());
		assertTrue(cache.isValid());
		robot.interact(() -> table.setColumnsWidth(200));
		assertTrue(cache.isValid());
		robot.interact(() -> table.getColumns().getFirst().resize(210));
		assertTrue(cache.isValid());
		robot.interact(() -> setWindowSize(table, 800, -1));
		assertTrue(cache.isValid());

		Reference<DebuggableCache> ref = new WeakReference<>(cache);
		cache = null;
		System.gc();
		assertNull(ref.get());
	}

	//================================================================================
	// Internal Classes
	//================================================================================
	static class DebuggableCache extends ColumnsLayoutCache<User> {

		public DebuggableCache(VFXTable<User> table) {
			super(table);
			setWidthFunction(this::computeColumnWidth);
			setPositionFunction(this::computeColumnPos);
			setVisibilityFunction(this::computeVisibility);
			init();
			sortToString = true;
		}

		protected double computeColumnWidth(VFXTableColumn<User, ?> column, boolean isLast) {
			VFXTable<User> table = getTable();
			double minW = table.getColumnsSize().getWidth();
			double prefW = Math.max(column.prefWidth(-1), minW);
			if (table.getColumns().size() == 1) return Math.max(prefW, table.getWidth());
			if (!isLast) return prefW;

			double partialW = getPartialWidth();
			return Math.max(prefW, table.getWidth() - partialW);
		}

		protected double computeColumnPos(int index, double prevPos) {
			VFXTable<User> table = getTable();
			VFXTableColumn<User, ? extends VFXTableCell<User>> column = table.getColumns().get(index);
			return prevPos + getColumnWidth(column);
		}

		protected boolean computeVisibility(VFXTableColumn<User, ?> column) {
			VFXTable<User> table = column.getTable();
			int index = column.getIndex();
			if (table == null ||
				index < 0 ||
				column.getScene() == null ||
				column.getParent() == null
			) return false;
			try {
				double tableW = table.getWidth();
				double hPos = table.getHPos();
				DoubleRange viewBounds = DoubleRange.of(hPos, hPos + tableW);
				double columnX = getColumnPos(index);
				double columnW = getColumnWidth(column);
				return (columnX + columnW >= viewBounds.getMin()) && (columnX <= viewBounds.getMax());
			} catch (Exception ex) {
				return false;
			}
		}

		void assertAllValid() {
			assertTrue(getCacheMap().values().stream().allMatch(LayoutInfo::isWidthValid));
		}

		void assertAllInvalid() {
			assertTrue(getCacheMap().values().stream().noneMatch(LayoutInfo::isWidthValid));
		}

		void assertInvalid(Integer... idxs) {
			Set<Integer> set = Set.of(idxs);
			ObservableList<VFXTableColumn<User, ? extends VFXTableCell<User>>> columns = getTable().getColumns();
			for (int i = 0; i < columns.size(); i++) {
				VFXTableColumn<User, ?> column = columns.get(i);
				ColumnsLayoutCache<User>.LayoutInfo li = getCacheMap().get(column);
				try {
					assertEquals(set.contains(i), !li.isWidthValid());
				} catch (AssertionError e) {
					System.err.printf("Assertion failed for %s/%d%n", column.getText(), i);
					fail(e);
				}
			}
		}

		void assertPosInvalid(int min, int max) {
			ObservableList<VFXTableColumn<User, ? extends VFXTableCell<User>>> columns = getTable().getColumns();
			for (int i = min; i <= max; i++) {
				VFXTableColumn<User, ? extends VFXTableCell<User>> c = columns.get(i);
				assertEquals(-1.0, getCacheMap().get(c).getPos());
			}
		}

		void assertVisible(int... idxs) {
			VFXTable<User> table = getTable();
			for (int idx : idxs) {
				assertTrue(isInViewport(table.getColumns().get(idx)));
			}
		}

		void assertNotVisible(int... idxs) {
			VFXTable<User> table = getTable();
			for (int idx : idxs) {
				assertFalse(isInViewport(table.getColumns().get(idx)));
			}
		}

		void assertPositionCount(int expected) {
			long cnt = getCacheMap().values().stream()
				.map(LayoutInfo::getPos)
				.filter(x -> x >= 0.0)
				.count();
			assertEquals(expected, cnt);
		}

		void assertVisibilityCount(long expected) {
			long cnt = getCacheMap().values().stream()
				.map(LayoutInfo::isVisible)
				.filter(Objects::nonNull)
				.count();
			assertEquals(expected, cnt);
		}

		public void clearPositionCache() {
			((LayoutInfoCache) getCacheMap()).clearPositionCache();
		}

		@Override
		public VFXTableColumn<User, ?> getLastColumn() {
			return super.getLastColumn();
		}
	}
}
