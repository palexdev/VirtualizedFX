package interactive.list;

import interactive.TestFXUtils;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.controls.Label;
import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.mfxcore.utils.RandomUtils;
import io.github.palexdev.virtualizedfx.cells.CellBase;
import io.github.palexdev.virtualizedfx.cells.CellBaseBehavior;
import io.github.palexdev.virtualizedfx.enums.BufferSize;
import io.github.palexdev.virtualizedfx.list.VFXListHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import utils.Utils;

import java.util.Comparator;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static interactive.TestFXUtils.assertCounter;
import static interactive.TestFXUtils.counter;
import static interactive.list.ListTestUtils.PList;
import static interactive.list.ListTestUtils.assertState;
import static io.github.palexdev.virtualizedfx.utils.Utils.INVALID_RANGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static utils.Utils.items;
import static utils.Utils.setWindowSize;

@ExtendWith(ApplicationExtension.class)
public class PaginatedListTests {

	@Start
	void start(Stage stage) {stage.show();}

	@BeforeEach
	void setup() {counter.reset();}

	@Test
	void testInitAndGeometry(FxRobot robot) {
		StackPane pane = TestFXUtils.setupStage();
		PList list = new PList(items(20), TestFXUtils.SimpleCell::new);
		robot.interact(() -> pane.getChildren().add(list));

		assertState(list, IntegerRange.of(0, 13));
		assertCounter(14, 1, 14, 14, 0, 0, 0);

		// Following tests do not affect the number of cells
		robot.interact(() -> setWindowSize(pane, -1, 600));
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(0, 0, 0, 0, 0, 0, 0);

		robot.interact(() -> setWindowSize(pane, -1, 300));
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(0, 0, 0, 0, 0, 0, 0);

		// Expand width and test again
		// Should be the same as before but with one extra layout
		robot.interact(() -> setWindowSize(pane, 500, -1));
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(0, 1, 0, 0, 0, 0, 0);

		// Edge case set height to 0
		robot.interact(() -> {
			list.setMinHeight(0);
			list.setPrefHeight(0);
			list.setMaxHeight(0);
			setWindowSize(pane, -1, 0);
		});
		assertState(list, INVALID_RANGE);
		assertCounter(0, 0, 0, 0, 0, 14, 4);
	}

	@Test
	void testInitAndGeometryAtBottom(FxRobot robot) {
		StackPane pane = TestFXUtils.setupStage();
		PList list = new PList(items(50), TestFXUtils.SimpleCell::new);
		robot.interact(() -> {
			pane.getChildren().add(list);
			list.scrollToLast();
		});

		// Check vPos!!
		assertEquals(1280.0, list.getVPos());
		assertEquals(4, list.getPage());

		assertState(list, IntegerRange.of(36, 49));
		assertCounter(14, 1, 14, 14, 0, 0, 0);

		// Expand and check counter, state and pos
		robot.interact(() -> list.setCellsPerPage(12));
		assertEquals(1536.0, list.getVPos());
		assertEquals(4, list.getPage());
		assertState(list, IntegerRange.of(34, 49));
		assertCounter(2, 1, 2, 2, 0, 0, 0);

		// Shrink and check again
		robot.interact(() -> list.setCellsPerPage(8));
		assertEquals(1024.0, list.getVPos());
		assertState(list, IntegerRange.of(30, 41));
		assertCounter(0, 1, 4, 4, 0, 4, 0);
	}

	@Test
	void testPopulateCache(FxRobot robot) {
		StackPane pane = TestFXUtils.setupStage();
		PList list = new PList(items(20), TestFXUtils.SimpleCell::new);
		list.populateCache(); // 10 by default
		counter.reset(); // Cells created by cache do not count
		robot.interact(() -> pane.getChildren().add(list));

		assertState(list, IntegerRange.of(0, 13));
		assertCounter(4, 1, 14, 14, 10, 0, 0);

		robot.interact(() -> list.setCellsPerPage(13));
		assertState(list, IntegerRange.of(0, 16));
		assertCounter(3, 1, 3, 3, 0, 0, 0);

		robot.interact(() -> list.setCellsPerPage(7));
		assertState(list, IntegerRange.of(0, 10));
		assertCounter(0, 1, 0, 0, 0, 6, 0);

		// Re-expand and cache should put in some work
		robot.interact(() -> list.setCellsPerPage(13));
		assertState(list, IntegerRange.of(0, 16));
		assertCounter(0, 1, 0, 0, 6, 0, 0); // No index nor item updates!
	}

	@Test
	void testChangePage(FxRobot robot) {
		StackPane pane = TestFXUtils.setupStage();
		PList list = new PList(items(100));
		list.populateCache(); // 10 by default
		counter.reset(); // Cells created by cache do not count
		robot.interact(() -> pane.getChildren().add(list));

		// Test init, why not
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(4, 1, 14, 14, 10, 0, 0);

		robot.interact(() -> list.setPage(1));
		assertState(list, IntegerRange.of(8, 21)); // [0, 13] -> [8, 21] = 8 update
		assertCounter(0, 1, 8, 8, 0, 0, 0);
		assertEquals(32 * 10, list.getVPos());

		robot.interact(list::scrollToLast);
		assertState(list, IntegerRange.of(86, 99)); // [8, 21] -> [86, 99] = big jump, all cells updated +14
		assertCounter(0, 1, 14, 14, 0, 0, 0);
		assertEquals(32 * 10 * 9, list.getVPos());
	}

	@Test
	void testMaxPageIncrement(FxRobot robot) {
		StackPane pane = TestFXUtils.setupStage();
		PList list = new PList(items((50)));
		robot.interact(() -> pane.getChildren().add(list));

		// Scroll to last first
		robot.interact(list::scrollToLast);
		assertEquals(4, list.getMaxPage());
		assertState(list, IntegerRange.of(36, 49));
		counter.reset(); // Don't care for these stats

		// Increment CPP
		robot.interact(() -> list.setCellsPerPage(15));
		assertEquals(3, list.getMaxPage());
		assertEquals(3, list.getPage());
		assertState(list, IntegerRange.of(31, 49));
		assertCounter(5, 1, 5, 5, 0, 0, 0);
		assertEquals(5, list.getVisibleCellsByIndex().size());
	}

	@Test
	void testMaxPageDecrement(FxRobot robot) {
		StackPane pane = TestFXUtils.setupStage();
		PList list = new PList(items((50)));
		robot.interact(() -> pane.getChildren().add(list));

		// Scroll to last first
		robot.interact(list::scrollToLast);
		assertEquals(4, list.getMaxPage());
		assertState(list, IntegerRange.of(36, 49));
		counter.reset(); // Don't care for these stats

		// Decrement CPP
		robot.interact(() -> list.setCellsPerPage(5));
		assertEquals(9, list.getMaxPage());
		assertEquals(4, list.getPage());
		assertState(list, IntegerRange.of(18, 26));
		assertCounter(0, 1, 9, 9, 0, 5, 0);
		assertEquals(5, list.getVisibleCellsByIndex().size());
	}

	@Test
	void testBufferChangeTop(FxRobot robot) {
		StackPane pane = TestFXUtils.setupStage();
		PList list = new PList(items((20)));
		VFXListHelper<Integer, TestFXUtils.SimpleCell> helper = list.getHelper();
		robot.interact(() -> pane.getChildren().add(list));

		// Standard buffer -> 14 cells (4 buffer)
		assertState(list, IntegerRange.of(0, 13));
		assertEquals(10, helper.visibleNum());
		assertEquals(14, helper.totalNum());
		assertCounter(14, 1, 14, 14, 0, 0, 0);

		// Small buffer -> 12 cells (2 buffer)
		robot.interact(() -> list.setBufferSize(BufferSize.SMALL));
		assertState(list, IntegerRange.of(0, 11));
		assertEquals(10, helper.visibleNum());
		assertEquals(12, helper.totalNum());
		assertCounter(0, 1, 0, 0, 0, 2, 0);

		// Big buffer -> 16 cells (6 buffer)
		robot.interact(() -> list.setBufferSize(BufferSize.BIG));
		assertState(list, IntegerRange.of(0, 15));
		assertEquals(10, helper.visibleNum());
		assertEquals(16, helper.totalNum());
		assertCounter(2, 1, 2, 2, 2, 0, 0);
	}

	@Test
	void testBufferChangeBottom(FxRobot robot) {
		StackPane pane = TestFXUtils.setupStage();
		PList list = new PList(items(20));
		VFXListHelper<Integer, TestFXUtils.SimpleCell> helper = list.getHelper();
		robot.interact(() -> {
			pane.getChildren().add(list);
			list.scrollToLast();
		});

		// Check vPos!!!
		assertEquals(32 * 10, list.getVPos());

		// Standard buffer -> 14 cells (4 buffer)
		assertState(list, IntegerRange.of(6, 19));
		assertEquals(10, helper.visibleNum());
		assertEquals(14, helper.totalNum());
		assertCounter(14, 1, 14, 14, 0, 0, 0);

		// Small buffer -> 12 cells (2 buffer)
		robot.interact(() -> list.setBufferSize(BufferSize.SMALL));
		assertState(list, IntegerRange.of(8, 19));
		assertEquals(10, helper.visibleNum());
		assertEquals(12, helper.totalNum());
		assertCounter(0, 1, 0, 0, 0, 2, 0);

		// Big buffer -> 16 cells (6 buffer)
		robot.interact(() -> list.setBufferSize(BufferSize.BIG));
		assertState(list, IntegerRange.of(4, 19));
		assertEquals(10, helper.visibleNum());
		assertEquals(16, helper.totalNum());
		assertCounter(2, 1, 4, 4, 2, 0, 0);

		/*
		 * Updates are 4 because the implemented cache is simple but quick. Cells are not stored by index!
		 */
	}

	@Test
	void testBufferChangeMiddle(FxRobot robot) {
		StackPane pane = TestFXUtils.setupStage();
		PList list = new PList(items(50));
		VFXListHelper<Integer, TestFXUtils.SimpleCell> helper = list.getHelper();
		robot.interact(() -> {
			pane.getChildren().add(list);
			list.setPage(2);
		});

		// Check vPos!!!
		assertEquals(32 * 10 * 2, list.getVPos());

		// Standard buffer -> 14 cells (4 buffer)
		assertState(list, IntegerRange.of(18, 31));
		assertEquals(10, helper.visibleNum());
		assertEquals(14, helper.totalNum());
		assertCounter(14, 1, 14, 14, 0, 0, 0);

		// Small buffer -> 12 cells (2 buffer)
		robot.interact(() -> list.setBufferSize(BufferSize.SMALL));
		assertState(list, IntegerRange.of(19, 30));
		assertEquals(10, helper.visibleNum());
		assertEquals(12, helper.totalNum());
		assertCounter(0, 1, 0, 0, 0, 2, 0);

		// Big buffer -> 16 cells (6 buffer)
		robot.interact(() -> list.setBufferSize(BufferSize.BIG));
		assertState(list, IntegerRange.of(17, 32));
		assertEquals(10, helper.visibleNum());
		assertEquals(16, helper.totalNum());
		assertCounter(2, 1, 4, 4, 2, 0, 0);
	}

	@Test
	void testChangeFactory(FxRobot robot) {
		StackPane pane = TestFXUtils.setupStage();
		PList list = new PList(items((50)));
		VFXListHelper<Integer, TestFXUtils.SimpleCell> helper = list.getHelper();
		robot.interact(() -> pane.getChildren().add(list));

		// Test init
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(14, 1, 14, 14, 0, 0, 0);

		robot.interact(() -> list.setPage(2));
		// Check vPos!!!
		assertEquals(32 * 10 * 2, list.getVPos());

		// Test change page
		assertState(list, IntegerRange.of(18, 31));
		assertCounter(0, 1, 14, 14, 0, 0, 0);

		// Change factory and test
		robot.interact(() ->
			list.setCellFactory(i -> new TestFXUtils.SimpleCell(i) {
				@Override
				protected SkinBase<?, ?> buildSkin() {
					return new SkinBase<>(this) {
						final Label label = new Label();

						{
							CellBase<Integer> cell = getSkinnable();
							listeners(
								When.onInvalidated(cell.indexProperty())
									.then(i -> {
										counter.index();
										update();
									})
									.executeNow(),
								When.onInvalidated(cell.itemProperty())
									.then(i -> {
										counter.item();
										update();
									})
									.executeNow()
							);
							label.setStyle("-fx-border-color: red");
							getChildren().add(label);
						}

						private void update() {
							int idx = getIndex();
							Integer it = getItem();
							label.setText("New Factory! Index: %d Item: %s".formatted(
								idx,
								it != null ? it.toString() : ""
							));
						}

						@Override
						protected void layoutChildren(double x, double y, double w, double h) {
							label.resizeRelocate(x, y, w, h);
						}

						@Override
						protected void initBehavior(CellBaseBehavior<Integer> behavior) {}
					};
				}
			}));

		assertState(list, IntegerRange.of(18, 31));
		assertEquals(10, helper.visibleNum());
		assertEquals(14, helper.totalNum());
		assertCounter(14, 1, 14, 14, 0, 14, 14);
		// The cache comes from the old state disposal, but those cells are then immediately
		// disposed and dropped by the cache, in fact there is no de-cache afterward
	}

	@Test
	void testFitToViewport(FxRobot robot) {
		StackPane pane = TestFXUtils.setupStage();
		PList list = new PList(items(30), i -> new TestFXUtils.SimpleCell(i) {
			@Override
			protected double computePrefWidth(double height) {
				if (getItem() != null && getItem() == 0) return 800.0;
				int val;
				do {
					val = ThreadLocalRandom.current().nextInt(350, 450);
				} while (val == 400.0);
				return val;
			}
		});
		robot.interact(() -> pane.getChildren().add(list));

		// Test init
		list.getState().getCellsByIndexUnmodifiable().values().stream()
			.map(CellBase::toNode)
			.forEach(n -> assertEquals(400.0, n.getLayoutBounds().getWidth()));

		// Disable and test again
		robot.interact(() -> list.setFitToViewport(false));
		assertEquals(800.0, list.getHelper().getVirtualMaxX());
		list.getState().getCellsByIndexUnmodifiable().values().stream()
			.map(CellBase::toNode)
			.forEach(n -> assertNotEquals(400.0, n.getLayoutBounds().getWidth()));

		// Scroll to max and then disable again
		robot.interact(() -> list.setHPos(Double.MAX_VALUE));
		assertEquals(400.0, list.getHPos()); // virtualMaxX(800) - viewportWidth(400)

		robot.interact(() -> list.setFitToViewport(true));
		assertEquals(0.0, list.getHPos());
		list.getState().getCellsByIndexUnmodifiable().values().stream()
			.map(CellBase::toNode)
			.forEach(n -> assertEquals(400.0, n.getLayoutBounds().getWidth()));
	}

	@Test
	void testChangeCellSizeTop(FxRobot robot) {
		StackPane pane = TestFXUtils.setupStage();
		PList list = new PList(items(50));
		VFXListHelper<Integer, TestFXUtils.SimpleCell> helper = list.getHelper();
		robot.interact(() -> pane.getChildren().add(list));

		// Test init, why not
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(14, 1, 14, 14, 0, 0, 0);
		assertEquals(32 * 50, helper.getVirtualMaxY());

		// Decrease and test again
		robot.interact(() -> list.setCellSize(20));
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(0, 1, 0, 0, 0, 0, 0);
		assertEquals(20 * 50, helper.getVirtualMaxY());

		// Increase and test again
		robot.interact(() -> list.setCellSize(36));
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(0, 1, 0, 0, 0, 0, 0);
		assertEquals(36 * 50, helper.getVirtualMaxY());
	}

	@Test
	void testChangeCellSizeMiddle(FxRobot robot) {
		StackPane pane = TestFXUtils.setupStage();
		PList list = new PList(items(50), TestFXUtils.SimpleCell::new);
		VFXListHelper<Integer, TestFXUtils.SimpleCell> helper = list.getHelper();
		robot.interact(() -> {
			pane.getChildren().add(list);
			list.setPage(2);
		});

		// Check vPos!!!
		assertEquals(32 * 10 * 2, list.getVPos());

		// Test init, why not
		assertState(list, IntegerRange.of(18, 31));
		assertCounter(14, 1, 14, 14, 0, 0, 0);
		assertEquals(32 * 50, helper.getVirtualMaxY());

		// Decrease and test again
		robot.interact(() -> list.setCellSize(10));
		assertState(list, IntegerRange.of(18, 31));
		assertCounter(0, 1, 0, 0, 0, 0, 0);
		assertEquals(10 * 50, helper.getVirtualMaxY());
		assertEquals(200, list.getVPos());

		// Increase and test again
		// vPos is now at 100!
		robot.interact(() -> list.setCellSize(50));
		assertState(list, IntegerRange.of(18, 31));
		assertCounter(0, 1, 0, 0, 0, 0, 0);
		assertEquals(50 * 50, helper.getVirtualMaxY());
		assertEquals(1000, list.getVPos());
	}

	@Test
	void testChangeCellSizeBottom(FxRobot robot) {
		StackPane pane = TestFXUtils.setupStage();
		PList list = new PList(items(50), TestFXUtils.SimpleCell::new);
		VFXListHelper<Integer, TestFXUtils.SimpleCell> helper = list.getHelper();
		robot.interact(() -> {
			pane.getChildren().add(list);
			list.scrollToLast();
		});

		// Check vPos!!!
		assertEquals(1280.0, list.getVPos());

		// Test init, why not
		assertState(list, IntegerRange.of(36, 49));
		assertCounter(14, 1, 14, 14, 0, 0, 0);
		assertEquals(32 * 50, helper.getVirtualMaxY());

		// Decrease and test again
		robot.interact(() -> list.setCellSize(24));
		assertState(list, IntegerRange.of(36, 49));
		assertCounter(0, 1, 0, 0, 0, 0, 0);
		assertEquals(24 * 50, helper.getVirtualMaxY());
		assertEquals(960, list.getVPos());

		// Increase and test again
		// vPos is now at 800!
		robot.interact(() -> list.setCellSize(44));
		assertState(list, IntegerRange.of(36, 49));
		assertCounter(0, 1, 0, 0, 0, 0, 0);
		assertEquals(44 * 50, helper.getVirtualMaxY());
		assertEquals(1760, list.getVPos());
	}

	@Test
	void testChangeList(FxRobot robot) {
		StackPane pane = TestFXUtils.setupStage();
		PList list = new PList(items(50));
		robot.interact(() -> pane.getChildren().add(list));

		assertState(list, IntegerRange.of(0, 13));
		assertCounter(14, 1, 14, 14, 0, 0, 0);

		// Change items property
		robot.interact(() -> list.setItems(items(50, 50)));
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(0, 1, 0, 14, 0, 0, 0);

		// Change items property (fewer elements but same values)
		robot.interact(() -> list.setItems(items(50, 10)));
		assertState(list, IntegerRange.of(0, 9));
		assertCounter(0, 1, 0, 0, 0, 4, 0);

		// Change items property (more elements and different values)
		robot.interact(() -> list.setItems(items(100, 50)));
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(0, 1, 0, 14, 4, 0, 0); // No index updates because of cached cells

		// Scroll(to bottom) and change items property (fewer elements)
		robot.interact(() -> {
			list.scrollToLast();
			list.setItems(items(50, 50));
		});
		assertState(list, IntegerRange.of(36, 49));
		assertCounter(0, 2, 14, 28, 0, 0, 0); // 2 layouts because of scroll

		// Change items property again (more elements)
		robot.interact(() -> list.setItems(items(50, 100)));
		assertState(list, IntegerRange.of(38, 51));
		assertCounter(0, 1, 2, 2, 0, 0, 0);
		assertEquals(4, list.getPage());
		assertEquals(9, list.getMaxPage());

		// Change items to empty
		robot.interact(() -> list.setItems(null));
		assertState(list, INVALID_RANGE);
		assertCounter(0, 0, 0, 0, 0, 14, 4);
		assertEquals(0, list.getPage());
		assertEquals(0, list.getMaxPage());
	}

	@Test
	void testPermutation(FxRobot robot) {
		StackPane pane = TestFXUtils.setupStage();
		PList list = new PList(items(50));
		robot.interact(() -> pane.getChildren().add(list));

		// Assert init
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(14, 1, 14, 14, 0, 0, 0);

		// Permutation change
		robot.interact(() -> FXCollections.sort(list.getItems(), Comparator.reverseOrder()));
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(0, 1, 0, 14, 0, 0, 0);
	}

	@Test
	void testSet(FxRobot robot) {
		StackPane pane = TestFXUtils.setupStage();
		PList list = new PList(items(50));
		robot.interact(() -> pane.getChildren().add(list));

		// Assert init
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(14, 1, 14, 14, 0, 0, 0);

		// Set all (more)
		robot.interact(() -> list.getItems().setAll(items(100, 100)));
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(0, 1, 0, 14, 0, 0, 0);

		// Set all (less)
		robot.interact(() -> list.getItems().setAll(items(100, 10)));
		assertState(list, IntegerRange.of(0, 9));
		assertCounter(0, 1, 0, 0, 0, 4, 0);

		// Restore old items count, scroll and set all (more)
		// Also check re-usability (by identity!!!)
		ObservableList<Integer> tmp = items(100, 100);
		robot.interact(() -> {
			list.getItems().setAll(tmp.subList(0, 50));
			list.scrollToLast();
			list.getItems().setAll(tmp);
		});
		assertState(list, IntegerRange.of(38, 51));
		assertCounter(0, 3, 16, 16, 4, 0, 0); // 3 layouts, one scroll, two updates

		// Set random items
		robot.interact(() -> {
			for (int i = 0; i < 5; i++) {
				int index = RandomUtils.random.nextInt(38, 52);
				list.getItems().set(index, 999 - i);
			}
		});
		assertState(list, IntegerRange.of(38, 51));
		assertCounter(0, 5, 0, 5, 0, 0, 0);
	}

	@Test
	void testAddAt0(FxRobot robot) {
		StackPane pane = TestFXUtils.setupStage();
		PList list = new PList(items(50));
		robot.interact(() -> pane.getChildren().add(list));

		// Assert init
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(14, 1, 14, 14, 0, 0, 0);

		// Add all at 0
		java.util.List<Integer> elements = IntStream.range(0, 4)
			.map(i -> 999 - i)
			.boxed()
			.toList();
		robot.interact(() -> list.getItems().addAll(0, elements));
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(0, 1, 14, 4, 0, 0, 0); // 14 by index, 13 are shifted, 4 are updated

		// Add at 0 (for)
		robot.interact(() -> {
			for (int i = 0; i < 4; i++) list.getItems().addFirst(1999 - i);
		});
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(0, 4, 56, 4, 0, 0, 0); // A lot of index updates because of for cycle
	}

	@Test
	void testAddMiddle(FxRobot robot) {
		StackPane pane = TestFXUtils.setupStage();
		PList list = new PList(items(50));
		robot.interact(() -> pane.getChildren().add(list));

		// Assert init
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(14, 1, 14, 14, 0, 0, 0);

		// Scroll to middle
		robot.interact(() -> list.setPage(2));
		counter.reset(); // Reset otherwise wrong statistics

		// Add before no intersect
		robot.interact(() -> list.getItems().addAll(0, java.util.List.of(-1, -2, -3)));
		assertState(list, IntegerRange.of(18, 31));
		assertCounter(0, 1, 14, 3, 0, 0, 0);

		// Add before intersect
		robot.interact(() -> list.getItems().addAll(16, java.util.List.of(-4, -5, -6, -7)));
		assertState(list, IntegerRange.of(18, 31));
		assertCounter(0, 1, 14, 4, 0, 0, 0);

		// Add after intersect
		robot.interact(() -> list.getItems().addAll(28, java.util.List.of(-8, -9, -10, -11, -12)));
		assertState(list, IntegerRange.of(18, 31));
		assertCounter(0, 1, 0, 4, 0, 0, 0);

		// Add after no intersect
		robot.interact(() -> list.getItems().addAll(40, java.util.List.of(483, 9822, 75486, 1240, 1151)));
		assertState(list, IntegerRange.of(18, 31));
		assertCounter(0, 1, 0, 0, 0, 0, 0);
	}

	@Test
	void testAddAtEnd(FxRobot robot) {
		StackPane pane = TestFXUtils.setupStage();
		PList list = new PList(items(50));
		robot.interact(() -> pane.getChildren().add(list));

		// Assert init
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(14, 1, 14, 14, 0, 0, 0);

		robot.interact(list::scrollToLast);
		counter.reset(); // Reset otherwise wrong statistics

		// Add all at end
		java.util.List<Integer> elements = IntStream.range(0, 4)
			.map(i -> 999 - i)
			.boxed()
			.toList();
		robot.interact(() -> list.getItems().addAll(elements));
		assertState(list, IntegerRange.of(38, 51));
		assertCounter(0, 1, 2, 2, 0, 0, 0); // These 2 updates are because the new elements allow the buffer to be at the bottom

		// Add at end (for)
		robot.interact(() -> {
			for (int i = 0; i < 4; i++) list.getItems().add(1999 - i);
		});
		assertState(list, IntegerRange.of(38, 51));
		assertCounter(0, 4, 0, 0, 0, 0, 0);

		// Add before no intersect
		robot.interact(() -> list.getItems().addAll(0, java.util.List.of(99, 98, 97, 96, 95)));
		assertState(list, IntegerRange.of(38, 51));
		assertCounter(0, 1, 14, 5, 0, 0, 0);

		// Add before intersect
		robot.interact(() -> list.getItems().addAll(34, java.util.List.of(-1, -2, -3, -4, -5)));
		assertState(list, IntegerRange.of(38, 51));
		assertCounter(0, 1, 14, 5, 0, 0, 0);
	}

	@Test
	void testRemoveAt0(FxRobot robot) {
		StackPane pane = TestFXUtils.setupStage();
		PList list = new PList(items(50));
		robot.interact(() -> pane.getChildren().add(list));

		// Assert init
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(14, 1, 14, 14, 0, 0, 0);

		// Remove all at 0
		robot.interact(() -> Utils.removeAll(list, 0, 1, 2, 3));
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(0, 1, 14, 4, 0, 0, 0);

		// Remove at 0 (for)
		robot.interact(() -> {
			for (int i = 0; i < 4; i++) list.getItems().removeFirst();
		});
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(0, 4, 56, 4, 0, 0, 0); // A lot of index updates because of for cycle
	}

	@Test
	void testRemoveAtMiddle(FxRobot robot) {
		StackPane pane = TestFXUtils.setupStage();
		PList list = new PList(items(50));
		robot.interact(() -> pane.getChildren().add(list));

		// Assert init
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(14, 1, 14, 14, 0, 0, 0);

		// Scroll to middle
		robot.interact(() -> list.setPage(2));
		counter.reset(); // Reset otherwise wrong statistics

		// Remove before no intersect
		robot.interact(() -> Utils.removeAll(list, 0, 1, 2));
		assertState(list, IntegerRange.of(18, 31));
		assertCounter(0, 1, 14, 3, 0, 0, 0);

		// Remove before intersect
		robot.interact(() -> Utils.removeAll(list, IntegerRange.of(16, 19)));
		assertState(list, IntegerRange.of(18, 31));
		assertCounter(0, 1, 14, 4, 0, 0, 0);

		// Remove after no intersect
		robot.interact(() -> Utils.removeAll(list, 34, 35));
		assertState(list, IntegerRange.of(18, 31));
		assertCounter(0, 1, 0, 0, 0, 0, 0);

		// Remove after intersect
		robot.interact(() -> Utils.removeAll(list, IntegerRange.of(29, 33)));
		assertState(list, IntegerRange.of(18, 31));
		assertCounter(0, 1, 0, 3, 0, 0, 0);

		// Remove enough to change page
		// Removed up until now: 14. Remaining -> 36. Max page -> 3
		// Remove to 18 -> Max Page -> 1
		robot.interact(() -> Utils.removeAll(list, IntegerRange.of(0, 17)));
		assertState(list, IntegerRange.of(4, 17));
		assertCounter(0, 1, 14, 4, 0, 0, 0);
		assertEquals(1, list.getPage());

		// Do it again but this time from bottom
		robot.interact(() -> Utils.removeAll(list, IntegerRange.of(8, 17)));
		assertState(list, IntegerRange.of(0, 7));
		assertCounter(0, 1, 4, 4, 0, 6, 0);
		assertEquals(0, list.getPage());
	}

	@Test
	void testRemoveAtEnd(FxRobot robot) {
		StackPane pane = TestFXUtils.setupStage();
		PList list = new PList(items(50));
		robot.interact(() -> pane.getChildren().add(list));

		// Assert init
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(14, 1, 14, 14, 0, 0, 0);

		robot.interact(list::scrollToLast);
		counter.reset(); // Reset otherwise wrong statistics

		// Remove all at end
		robot.interact(() -> Utils.removeAll(list, IntegerRange.of(46, 49)));
		assertState(list, IntegerRange.of(32, 45));
		assertCounter(0, 1, 4, 4, 0, 0, 0);

		// Remove at end (for)
		robot.interact(() -> {
			for (int i = 0; i < 4; i++) list.getItems().removeLast();
		});
		assertState(list, IntegerRange.of(28, 41));
		assertCounter(0, 4, 4, 4, 0, 0, 0);

		// Remove before no intersect
		robot.interact(() -> Utils.removeAll(list, 0, 1, 2));
		assertState(list, IntegerRange.of(25, 38));
		assertCounter(0, 1, 14, 0, 0, 0, 0);
		assertEquals(3, list.getMaxPage());
		assertEquals(3, list.getPage());

		// Remove before intersect
		robot.interact(() -> Utils.removeAll(list, IntegerRange.of(23, 26)));
		assertState(list, IntegerRange.of(21, 34));
		assertCounter(0, 1, 14, 2, 0, 0, 0);
		assertEquals(3, list.getMaxPage());
		assertEquals(3, list.getPage());

		// Remove enough to cache cells
		robot.interact(() -> Utils.removeAll(list, IntegerRange.of(5, 34)));
		assertState(list, IntegerRange.of(0, 4));
		assertCounter(0, 1, 5, 5, 0, 9, 0);
		assertEquals(0, list.getMaxPage());
		assertEquals(0, list.getPage());
	}

	@Test
	void testRemoveSparse(FxRobot robot) {
		StackPane pane = TestFXUtils.setupStage();
		PList list = new PList(items(50));
		robot.interact(() -> pane.getChildren().add(list));

		// Assert init
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(14, 1, 14, 14, 0, 0, 0);

		robot.interact(() -> Utils.removeAll(list, 0, 3, 4, 8, 10, 11));
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(0, 1, 14, 6, 0, 0, 0);
	}

	@Test
	void testSwitchOrientation(FxRobot robot) {
		StackPane pane = TestFXUtils.setupStage();
		PList list = new PList(items(50));
		robot.interact(() -> pane.getChildren().add(list));
		counter.reset();

		robot.interact(() -> list.setOrientation(Orientation.HORIZONTAL));
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(0, 2, 0, 0, 0, 0, 0); // 2 layouts because for paginated variant geometry changes too

		robot.interact(() -> list.setPage(2));
		assertEquals(0.0, list.getVPos());
		assertEquals(32 * 10 * 2, list.getHPos());
		assertState(list, IntegerRange.of(18, 31));
		assertCounter(0, 1, 14, 14, 0, 0, 0);

		robot.interact(list::scrollToLast);
		assertEquals(0.0, list.getVPos());
		assertEquals(32 * 10 * 4, list.getHPos());
		assertState(list, IntegerRange.of(36, 49));
		assertCounter(0, 1, 14, 14, 0, 0, 0);

		robot.interact(() -> list.setOrientation(Orientation.VERTICAL));
		assertEquals(0.0, list.getVPos());
		assertEquals(0.0, list.getHPos());
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(0, 2, 14, 14, 0, 0, 0); // 2 layouts, same reason as above
	}

	@Test
	void testSwitchOrientationFitToViewport(FxRobot robot) {
		StackPane pane = TestFXUtils.setupStage();
		PList list = new PList(items(50));
		robot.interact(() -> {
			list.setOrientation(Orientation.HORIZONTAL);
			pane.getChildren().add(list);
		});

		// Assert init
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(14, 2, 14, 14, 0, 0, 0);

		// Change factory
		robot.interact(() -> list.setCellFactory(i -> new TestFXUtils.SimpleCell(i) {
			@Override
			protected double computePrefWidth(double height) {
				return 500.0;
			}

			@Override
			protected double computePrefHeight(double width) {
				return 500.0;
			}
		}));
		assertEquals(0.0, list.getVPos());
		assertEquals(0.0, list.getHPos());
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(14, 1, 14, 14, 0, 14, 14);
		assertEquals(400.0, list.getHelper().getVirtualMaxY());

		// Allow variable height
		robot.interact(() -> {
			list.setFitToViewport(false);
			list.setVPos(Double.MAX_VALUE);
		});
		assertEquals(100.0, list.getVPos());
		assertEquals(0.0, list.getHPos());
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(0, 1, 0, 0, 0, 0, 0);
		assertEquals(500.0, list.getHelper().getVirtualMaxY());

		robot.interact(() -> {
			list.scrollToLast();
			counter.reset();
			list.setOrientation(Orientation.VERTICAL);
		});
		assertEquals(0.0, list.getVPos());
		assertEquals(0.0, list.getHPos());
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(0, 2, 14, 14, 0, 0, 0);
		assertEquals(500.0, list.getHelper().getVirtualMaxX());

	}

	@Test
	void testSpacing(FxRobot robot) {
		StackPane pane = TestFXUtils.setupStage();
		PList list = new PList(items(50));
		robot.interact(() -> pane.getChildren().add(list));

		// Assert init
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(14, 1, 14, 14, 0, 0, 0);

		// Set spacing 10.0
		robot.interact(() -> list.setSpacing(10.0));
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(0, 1, 0, 0, 0, 0, 0);
		assertEquals((10 * (32 + 10)) - 10, (int) list.getHeight());

		// Set spacing 30.0
		robot.interact(() -> list.setSpacing(30.0));
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(0, 1, 0, 0, 0, 0, 0);
		assertEquals((10 * (32 + 30)) - 30, (int) list.getHeight());

		// Try scrolling
		robot.interact(list::next);
		assertState(list, IntegerRange.of(8, 21));
		assertCounter(0, 1, 8, 8, 0, 0, 0);

		// Scroll to end
		robot.interact(list::scrollToLast);
		assertState(list, IntegerRange.of(36, 49));
		assertCounter(0, 1, 14, 14, 0, 0, 0);

		// Remove spacing
		robot.interact(() -> list.setSpacing(0.0));
		assertState(list, IntegerRange.of(36, 49));
		assertCounter(0, 1, 0, 0, 0, 0, 0);
		assertEquals(32 * 10, (int) list.getHeight());
	}
}
