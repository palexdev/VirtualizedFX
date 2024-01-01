package interactive.list;

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
import javafx.geometry.Orientation;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;
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

import static interactive.list.ListTestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static utils.Utils.items;

@ExtendWith(ApplicationExtension.class)
public class ListTests {

	@Start
	void start(Stage stage) {stage.show();}

	@BeforeEach
	void setup() {counter.reset();}

	@Test
	void testInitAndGeometry(FxRobot robot) {
		StackPane pane = setupStage();
		List list = new List(items(20));
		robot.interact(() -> pane.getChildren().add(list));

		assertState(list, IntegerRange.of(0, 16)); // ceil(400/32) + 4 (buffer) -> 17
		assertCounter(17, 1, 17, 17, 0, 0);

		// Expand and test again
		robot.interact(() -> {
			Window window = pane.getScene().getWindow();
			window.setHeight(600);
		});
		assertState(list, IntegerRange.of(0, 19)); // ceil(600/32) + 4 (buffer) -> 23 (Items num 20!!!)
		assertCounter(3, 1, 3, 3, 0, 0);

		// Shrink and test again
		robot.interact(() -> {
			Window window = pane.getScene().getWindow();
			window.setHeight(300);
		});
		assertState(list, IntegerRange.of(0, 13)); // ceil(300/32) + 4 (buffer) -> 14
		assertCounter(0, 1, 0, 0, 0, 6);

		// Expand width and test again
		// Should be the same as before but with one extra layout
		robot.interact(() -> {
			Window window = pane.getScene().getWindow();
			window.setWidth(500);
		});
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(0, 1, 0, 0, 0, 0);

		// Edge case set height to 0
		robot.interact(() -> {
			list.setMinHeight(0);
			list.setPrefHeight(0);
			list.setMaxHeight(0);
			Window window = pane.getScene().getWindow();
			window.setHeight(0);
		});
		assertState(list, IntegerRange.of(-1));
		assertCounter(0, 0, 0, 0, 0, 14);
	}

	@Test
	void testInitAndGeometryAtBottom(FxRobot robot) {
		StackPane pane = setupStage();
		List list = new List(items(50));
		robot.interact(() -> {
			pane.getChildren().add(list);
			list.scrollToLast();
		});

		// Check vPos!!
		assertEquals(1200.0, list.getVPos());

		assertState(list, IntegerRange.of(33, 49)); // ceil(400/32) + 4 (buffer) -> 17
		assertCounter(17, 1, 17, 17, 0, 0);

		// Expand and check counter, state and pos
		robot.interact(() -> {
			Window w = list.getScene().getWindow();
			w.setHeight(600);
		});
		assertEquals(1000.0, list.getVPos());
		assertState(list, IntegerRange.of(27, 49)); // ceil(600/32) + 4 (buffer) -> 23
		assertCounter(6, 1, 6, 6, 0, 0);

		// Shrink and check again
		robot.interact(() -> {
			Window w = list.getScene().getWindow();
			w.setHeight(300);
		});
		assertEquals(1000.0, list.getVPos());
		assertState(list, IntegerRange.of(29, 42));
		assertCounter(0, 1, 0, 0, 0, 9);
	}

	@Test
	void testPopulateCache(FxRobot robot) {
		StackPane pane = setupStage();
		List list = new List(items(20));
		list.getCache().populate(); // 10 by default
		counter.reset(); // Cells created by cache do not count
		robot.interact(() -> pane.getChildren().add(list));

		assertState(list, IntegerRange.of(0, 16));
		assertCounter(7, 1, 17, 17, 10, 0);

		robot.interact(() -> {
			Window window = list.getScene().getWindow();
			window.setHeight(600);
		});
		assertState(list, IntegerRange.of(0, 19));
		assertCounter(3, 1, 3, 3, 0, 0);

		robot.interact(() -> {
			Window window = list.getScene().getWindow();
			window.setHeight(300);
		});
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(0, 1, 0, 0, 0, 6);

		// Re-expand and cache should put in some work
		robot.interact(() -> {
			Window window = list.getScene().getWindow();
			window.setHeight(600);
		});
		assertState(list, IntegerRange.of(0, 19));
		assertCounter(0, 1, 0, 0, 6, 0); // No index nor item updates!
	}

	@Test
	void testScroll(FxRobot robot) {
		StackPane pane = setupStage();
		List list = new List(items(100));
		list.getCache().populate(); // 10 by default
		robot.interact(() -> pane.getChildren().add(list));

		// Test init, why not
		assertState(list, IntegerRange.of(0, 16));
		assertCounter(17, 1, 17, 17, 10, 0);

		robot.interact(() -> list.setVPos(100));
		assertState(list, IntegerRange.of(1, 17)); // [0, 16] -> [1, 17] = 1 update
		assertCounter(0, 1, 1, 1, 0, 0);

		robot.interact(() -> list.setVPos(Double.MAX_VALUE));
		assertState(list, IntegerRange.of(83, 99)); // [1, 17] -> [83, 99] = big jump, all cells updated +17
		assertCounter(0, 1, 17, 17, 0, 0);

		robot.interact(() -> {
			list.setItems(items(50));
			list.setVPos(0);
			counter.reset();
			list.setVPos(600.0);
		});
		assertState(list, IntegerRange.of(16, 32));
		assertCounter(0, 1, 16, 16, 0, 0); // Only 16 updated because index 16 is common
	}

	@Test
	void testBufferChangeTop(FxRobot robot) {
		StackPane pane = setupStage();
		List list = new List(items(20));
		VFXListHelper<Integer, SimpleCell> helper = list.getHelper();
		robot.interact(() -> pane.getChildren().add(list));

		// Standard buffer -> 17 cells (4 buffer)
		assertState(list, IntegerRange.of(0, 16));
		assertEquals(13, helper.visibleNum());
		assertEquals(17, helper.totalNum());
		assertCounter(17, 1, 17, 17, 0, 0);

		// Small buffer -> 15 cells (2 buffer)
		robot.interact(() -> list.setBufferSize(BufferSize.SMALL));
		assertState(list, IntegerRange.of(0, 14));
		assertEquals(13, helper.visibleNum());
		assertEquals(15, helper.totalNum());
		assertCounter(0, 1, 0, 0, 0, 2);

		// Big buffer -> 19 cells (6 buffer)
		robot.interact(() -> list.setBufferSize(BufferSize.BIG));
		assertState(list, IntegerRange.of(0, 18));
		assertEquals(13, helper.visibleNum());
		assertEquals(19, helper.totalNum());
		assertCounter(2, 1, 2, 2, 2, 0);
	}

	@Test
	void testBufferChangeBottom(FxRobot robot) {
		StackPane pane = setupStage();
		List list = new List(items(20));
		VFXListHelper<Integer, SimpleCell> helper = list.getHelper();
		robot.interact(() -> {
			pane.getChildren().add(list);
			list.setVPos(Double.MAX_VALUE);
		});

		// Check vPos!!!
		assertEquals(240, list.getVPos());

		// Standard buffer -> 17 cells (4 buffer)
		assertState(list, IntegerRange.of(3, 19));
		assertEquals(13, helper.visibleNum());
		assertEquals(17, helper.totalNum());
		assertCounter(17, 1, 17, 17, 0, 0);

		// Small buffer -> 15 cells (2 buffer)
		robot.interact(() -> list.setBufferSize(BufferSize.SMALL));
		assertState(list, IntegerRange.of(5, 19));
		assertEquals(13, helper.visibleNum());
		assertEquals(15, helper.totalNum());
		assertCounter(0, 1, 0, 0, 0, 2);

		// Big buffer -> 19 cells (6 buffer)
		robot.interact(() -> list.setBufferSize(BufferSize.BIG));
		assertState(list, IntegerRange.of(1, 19));
		assertEquals(13, helper.visibleNum());
		assertEquals(19, helper.totalNum());
		assertCounter(2, 1, 4, 4, 2, 0);

		/*
		 * Updates are 4 because the implemented cache is simple but quick. Cells are not stored by index!
		 */
	}

	@Test
	void testBufferChangeMiddle(FxRobot robot) {
		StackPane pane = setupStage();
		List list = new List(items(50));
		VFXListHelper<Integer, SimpleCell> helper = list.getHelper();
		robot.interact(() -> {
			pane.getChildren().add(list);
			list.setVPos(600);
		});

		// Check vPos!!!
		assertEquals(600, list.getVPos());

		// Standard buffer -> 17 cells (4 buffer)
		assertState(list, IntegerRange.of(16, 32));
		assertEquals(13, helper.visibleNum());
		assertEquals(17, helper.totalNum());
		assertCounter(17, 1, 17, 17, 0, 0);

		// Small buffer -> 15 cells (2 buffer)
		robot.interact(() -> list.setBufferSize(BufferSize.SMALL));
		assertState(list, IntegerRange.of(17, 31));
		assertEquals(13, helper.visibleNum());
		assertEquals(15, helper.totalNum());
		assertCounter(0, 1, 0, 0, 0, 2);

		// Big buffer -> 19 cells (6 buffer)
		robot.interact(() -> list.setBufferSize(BufferSize.BIG));
		assertState(list, IntegerRange.of(15, 33));
		assertEquals(13, helper.visibleNum());
		assertEquals(19, helper.totalNum());
		assertCounter(2, 1, 4, 4, 2, 0);
	}

	@Test
	void testChangeFactory(FxRobot robot) {
		StackPane pane = setupStage();
		List list = new List(items(50));
		VFXListHelper<Integer, SimpleCell> helper = list.getHelper();
		robot.interact(() -> pane.getChildren().add(list));

		// Test init
		assertState(list, IntegerRange.of(0, 16));
		assertCounter(17, 1, 17, 17, 0, 0);

		robot.interact(() -> list.setVPos(600));
		// Check vPos!!!
		assertEquals(600, list.getVPos());

		// Test scroll
		assertState(list, IntegerRange.of(16, 32));
		assertCounter(0, 1, 16, 16, 0, 0);

		// Change factory and test
		robot.interact(() ->
			list.setCellFactory(i -> new SimpleCell(i) {
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

		assertState(list, IntegerRange.of(16, 32));
		assertEquals(13, helper.visibleNum());
		assertEquals(17, helper.totalNum());
		assertCounter(17, 1, 17, 17, 0, 17);
		// The cache comes from the old state disposal, but those cells are then immediately
		// disposed and dropped by the cache, in fact there is no de-cache afterward
	}

	@Test
	void testFitToBreadth(FxRobot robot) {
		StackPane pane = setupStage();
		List list = new List(items(30), i -> new SimpleCell(i) {
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
		robot.interact(() -> list.setFitToBreadth(false));
		assertEquals(800.0, list.getHelper().maxBreadthProperty().get());
		list.getState().getCellsByIndexUnmodifiable().values().stream()
			.map(CellBase::toNode)
			.forEach(n -> assertNotEquals(400.0, n.getLayoutBounds().getWidth()));

		// Scroll to max and then disable again
		robot.interact(() -> list.setHPos(Double.MAX_VALUE));
		assertEquals(400.0, list.getHPos()); // maxBreadth(800) - viewportWidth(400)

		robot.interact(() -> list.setFitToBreadth(true));
		assertEquals(0.0, list.getHPos());
		list.getState().getCellsByIndexUnmodifiable().values().stream()
			.map(CellBase::toNode)
			.forEach(n -> assertEquals(400.0, n.getLayoutBounds().getWidth()));
	}

	@Test
	void testChangeCellSizeTop(FxRobot robot) {
		StackPane pane = setupStage();
		List list = new List(items(50));
		VFXListHelper<Integer, SimpleCell> helper = list.getHelper();
		robot.interact(() -> pane.getChildren().add(list));

		// Test init, why not
		assertState(list, IntegerRange.of(0, 16));
		assertCounter(17, 1, 17, 17, 0, 0);
		assertEquals(32 * 50, helper.estimatedLengthProperty().get());

		// Decrease and test again
		robot.interact(() -> list.setCellSize(20));
		assertState(list, IntegerRange.of(0, 23));
		assertCounter(7, 1, 7, 7, 0, 0);
		assertEquals(20 * 50, helper.estimatedLengthProperty().get());

		// Increase and test again
		robot.interact(() -> list.setCellSize(36));
		assertState(list, IntegerRange.of(0, 15));
		assertCounter(0, 1, 0, 0, 0, 8);
		assertEquals(36 * 50, helper.estimatedLengthProperty().get());
	}

	@Test
	void testChangeCellSizeMiddle(FxRobot robot) {
		StackPane pane = setupStage();
		List list = new List(items(50));
		VFXListHelper<Integer, SimpleCell> helper = list.getHelper();
		robot.interact(() -> {
			pane.getChildren().add(list);
			list.setVPos(600.0);
		});

		// Check vPos!!!
		assertEquals(600, list.getVPos());

		// Test init, why not
		assertState(list, IntegerRange.of(16, 32));
		assertCounter(17, 1, 17, 17, 0, 0);
		assertEquals(32 * 50, helper.estimatedLengthProperty().get());

		// Decrease and test again
		robot.interact(() -> list.setCellSize(10));
		assertState(list, IntegerRange.of(6, 49));
		assertCounter(27, 1, 27, 27, 0, 0);
		assertEquals(10 * 50, helper.estimatedLengthProperty().get());
		assertEquals(100, list.getVPos());

		// Increase and test again
		// vPos is now at 100!
		robot.interact(() -> list.setCellSize(50));
		assertState(list, IntegerRange.of(0, 11));
		assertCounter(0, 1, 6, 6, 0, 32);
		assertEquals(50 * 50, helper.estimatedLengthProperty().get());
		assertEquals(100, list.getVPos());
	}

	@Test
	void testChangeCellSizeBottom(FxRobot robot) {
		StackPane pane = setupStage();
		List list = new List(items(50));
		VFXListHelper<Integer, SimpleCell> helper = list.getHelper();
		robot.interact(() -> {
			pane.getChildren().add(list);
			list.setVPos(Double.MAX_VALUE);
		});

		// Check vPos!!!
		assertEquals(1200.0, list.getVPos());

		// Test init, why not
		assertState(list, IntegerRange.of(33, 49));
		assertCounter(17, 1, 17, 17, 0, 0);
		assertEquals(32 * 50, helper.estimatedLengthProperty().get());

		// Decrease and test again
		robot.interact(() -> list.setCellSize(24));
		assertState(list, IntegerRange.of(29, 49));
		assertCounter(4, 1, 4, 4, 0, 0);
		assertEquals(24 * 50, helper.estimatedLengthProperty().get());

		// Increase and test again
		// vPos is now at 800!
		robot.interact(() -> list.setCellSize(44));
		assertState(list, IntegerRange.of(16, 29));
		assertCounter(0, 1, 13, 13, 0, 7);
		assertEquals(44 * 50, helper.estimatedLengthProperty().get());
	}

	@Test
	void testChangeList(FxRobot robot) {
		StackPane pane = setupStage();
		List list = new List(items(50));
		robot.interact(() -> pane.getChildren().add(list));

		assertState(list, IntegerRange.of(0, 16));
		assertCounter(17, 1, 17, 17, 0, 0);

		// Change items property
		robot.interact(() -> list.setItems(items(50, 50)));
		assertState(list, IntegerRange.of(0, 16));
		assertCounter(0, 1, 0, 17, 0, 0);

		// Change items property (fewer elements)
		robot.interact(() -> list.setItems(items(50, 10)));
		assertState(list, IntegerRange.of(0, 9));
		assertCounter(0, 1, 0, 0, 0, 7);

		// Change items property (more elements)
		robot.interact(() -> list.setItems(items(100, 50)));
		assertState(list, IntegerRange.of(0, 16));
		assertCounter(0, 1, 0, 17, 7, 0); // No index updates because of cached cells

		// Scroll(to bottom) and change items property (fewer elements)
		robot.interact(() -> {
			list.setVPos(Double.MAX_VALUE);
			list.setItems(items(50, 50));
		});
		assertState(list, IntegerRange.of(33, 49));
		assertCounter(0, 2, 17, 34, 0, 0); // 2 layouts because of scroll

		// Change items property again (more elements)
		robot.interact(() -> list.setItems(items(50, 100)));
		assertState(list, IntegerRange.of(35, 51));
		assertCounter(0, 1, 2, 2, 0, 0);

		// Change items to empty
		robot.interact(() -> list.setItems(null));
		assertState(list, IntegerRange.of(-1));
		assertCounter(0, 0, 0, 0, 0, 17);
	}

	@Test
	void testPermutation(FxRobot robot) {
		StackPane pane = setupStage();
		List list = new List(items(50));
		robot.interact(() -> pane.getChildren().add(list));

		// Assert init
		assertState(list, IntegerRange.of(0, 16));
		assertCounter(17, 1, 17, 17, 0, 0);

		// Permutation change
		robot.interact(() -> FXCollections.sort(list.getItems(), Comparator.reverseOrder()));
		assertState(list, IntegerRange.of(0, 16));
		assertCounter(0, 1, 0, 17, 0, 0);
	}

	@Test
	void testSet(FxRobot robot) {
		StackPane pane = setupStage();
		List list = new List(items(50));
		robot.interact(() -> pane.getChildren().add(list));

		// Assert init
		assertState(list, IntegerRange.of(0, 16));
		assertCounter(17, 1, 17, 17, 0, 0);

		// Set all (more)
		robot.interact(() -> list.getItems().setAll(items(100, 100)));
		assertState(list, IntegerRange.of(0, 16));
		assertCounter(0, 1, 0, 17, 0, 0);

		// Set all (less)
		robot.interact(() -> list.getItems().setAll(items(100, 15)));
		assertState(list, IntegerRange.of(0, 14));
		assertCounter(0, 1, 0, 0, 0, 2);

		// Restore old items count, scroll and set all (more)
		robot.interact(() -> {
			list.getItems().setAll(items(100, 50));
			list.setVPos(Double.MAX_VALUE);
			list.getItems().setAll(items(100, 100));
		});
		assertState(list, IntegerRange.of(35, 51));
		assertCounter(0, 3, 19, 19, 2, 0); // 3 layouts, one scroll, two updates

		// Set random items
		robot.interact(() -> {
			for (int i = 0; i < 5; i++) {
				int index = RandomUtils.random.nextInt(35, 52);
				list.getItems().set(index, 999 - i);
			}
		});
		assertState(list, IntegerRange.of(35, 51));
		assertCounter(0, 5, 0, 5, 0, 0);
	}

	@Test
	void testAddAt0(FxRobot robot) {
		StackPane pane = setupStage();
		List list = new List(items(50));
		robot.interact(() -> pane.getChildren().add(list));

		// Assert init
		assertState(list, IntegerRange.of(0, 16));
		assertCounter(17, 1, 17, 17, 0, 0);

		// Add all at 0
		java.util.List<Integer> elements = IntStream.range(0, 4)
			.map(i -> 999 - i)
			.boxed()
			.toList();
		robot.interact(() -> list.getItems().addAll(0, elements));
		assertState(list, IntegerRange.of(0, 16));
		assertCounter(0, 1, 17, 4, 0, 0); // 17 by index, 13 are shifted, 4 are updated

		// Add at 0 (for)
		robot.interact(() -> {
			for (int i = 0; i < 4; i++) list.getItems().addFirst(1999 - i);
		});
		assertState(list, IntegerRange.of(0, 16));
		assertCounter(0, 4, 68, 4, 0, 0); // A lot of index updates because of for cycle
	}

	@Test
	void testAddMiddle(FxRobot robot) {
		StackPane pane = setupStage();
		List list = new List(items(50));
		robot.interact(() -> pane.getChildren().add(list));

		// Assert init
		assertState(list, IntegerRange.of(0, 16));
		assertCounter(17, 1, 17, 17, 0, 0);

		// Scroll to middle
		robot.interact(() -> list.setVPos(600));
		counter.reset(); // Reset otherwise wrong statistics

		// Add before no intersect
		robot.interact(() -> list.getItems().addAll(0, java.util.List.of(-1, -2, -3)));
		assertState(list, IntegerRange.of(16, 32));
		assertCounter(0, 1, 17, 3, 0, 0);

		// Add before intersect
		robot.interact(() -> list.getItems().addAll(14, java.util.List.of(-4, -5, -6, -7)));
		assertState(list, IntegerRange.of(16, 32));
		assertCounter(0, 1, 17, 4, 0, 0);

		// Add after intersect
		robot.interact(() -> list.getItems().addAll(29, java.util.List.of(-8, -9, -10, -11, -12)));
		assertState(list, IntegerRange.of(16, 32));
		assertCounter(0, 1, 0, 4, 0, 0);

		// Add after no intersect
		robot.interact(() -> list.getItems().addAll(40, java.util.List.of(483, 9822, 75486, 1240, 1151)));
		assertState(list, IntegerRange.of(16, 32));
		assertCounter(0, 1, 0, 0, 0, 0);
	}

	@Test
	void testAddAtEnd(FxRobot robot) {
		StackPane pane = setupStage();
		List list = new List(items(50));
		robot.interact(() -> pane.getChildren().add(list));

		// Assert init
		assertState(list, IntegerRange.of(0, 16));
		assertCounter(17, 1, 17, 17, 0, 0);

		robot.interact(() -> list.setVPos(Double.MAX_VALUE));
		counter.reset(); // Reset otherwise wrong statistics

		// Add all at end
		java.util.List<Integer> elements = IntStream.range(0, 4)
			.map(i -> 999 - i)
			.boxed()
			.toList();
		robot.interact(() -> list.getItems().addAll(elements));
		assertState(list, IntegerRange.of(35, 51));
		assertCounter(0, 1, 2, 2, 0, 0); // These 2 updates are because the new elements allow the buffer to be at the bottom

		// Add at end (for)
		robot.interact(() -> {
			for (int i = 0; i < 4; i++) list.getItems().add(1999 - i);
		});
		assertState(list, IntegerRange.of(35, 51));
		assertCounter(0, 4, 0, 0, 0, 0);

		// Add before no intersect
		robot.interact(() -> list.getItems().addAll(0, java.util.List.of(99, 98, 97, 96, 95)));
		assertState(list, IntegerRange.of(35, 51));
		assertCounter(0, 1, 17, 5, 0, 0);

		// Add before intersect
		robot.interact(() -> list.getItems().addAll(32, java.util.List.of(-1, -2, -3, -4, -5)));
		assertState(list, IntegerRange.of(35, 51));
		assertCounter(0, 1, 17, 5, 0, 0);
	}

	@Test
	void testRemoveAt0(FxRobot robot) {
		StackPane pane = setupStage();
		List list = new List(items(50));
		robot.interact(() -> pane.getChildren().add(list));

		// Assert init
		assertState(list, IntegerRange.of(0, 16));
		assertCounter(17, 1, 17, 17, 0, 0);

		// Remove all at 0
		robot.interact(() -> Utils.removeAll(list.getItems(), 0, 1, 2, 3));
		assertState(list, IntegerRange.of(0, 16));
		assertCounter(0, 1, 17, 4, 0, 0);

		// Remove at 0 (for)
		robot.interact(() -> {
			for (int i = 0; i < 4; i++) list.getItems().removeFirst();
		});
		assertState(list, IntegerRange.of(0, 16));
		assertCounter(0, 4, 68, 4, 0, 0); // A lot of index updates because of for cycle
	}

	@Test
	void testRemoveMiddle(FxRobot robot) {
		StackPane pane = setupStage();
		List list = new List(items(50));
		robot.interact(() -> pane.getChildren().add(list));

		// Assert init
		assertState(list, IntegerRange.of(0, 16));
		assertCounter(17, 1, 17, 17, 0, 0);

		// Scroll to middle
		robot.interact(() -> list.setVPos(600));
		counter.reset(); // Reset otherwise wrong statistics

		// Remove before no intersect
		robot.interact(() -> Utils.removeAll(list.getItems(), 0, 1, 2));
		assertState(list, IntegerRange.of(16, 32));
		assertCounter(0, 1, 17, 3, 0, 0);

		// Remove before intersect
		robot.interact(() -> Utils.removeAll(list.getItems(), 14, 15, 16, 17));
		assertState(list, IntegerRange.of(16, 32));
		assertCounter(0, 1, 17, 4, 0, 0);

		// Remove after no intersect
		robot.interact(() -> Utils.removeAll(list.getItems(), 34, 35));
		assertState(list, IntegerRange.of(16, 32));
		assertCounter(0, 1, 0, 0, 0, 0);

		// Remove after intersect
		robot.interact(() -> Utils.removeAll(list.getItems(), 30, 31, 32, 33, 34));
		assertState(list, IntegerRange.of(16, 32));
		assertCounter(0, 1, 0, 3, 0, 0);

		// Remove enough to change vPos
		// Removed up until now: 14. Remaining -> 36. Max vPos -> 1152
		// Remove to 27 -> Max vPos -> 464
		robot.interact(() -> Utils.removeAll(list.getItems(), 0, 1, 2, 3, 4, 5, 6, 7, 8));
		assertState(list, IntegerRange.of(10, 26));
		assertCounter(0, 1, 17, 3, 0, 0);
		assertEquals(list.getCellSize() * list.size() - list.getHeight(), list.getVPos());

		// Do it again but this time from bottom
		robot.interact(() -> Utils.removeAll(list.getItems(), 26, 25, 24));
		assertState(list, IntegerRange.of(7, 23));
		assertCounter(0, 1, 3, 3, 0, 0);
		assertEquals(list.getCellSize() * list.size() - list.getHeight(), list.getVPos());
	}

	@Test
	void testRemoveAtEnd(FxRobot robot) {
		StackPane pane = setupStage();
		List list = new List(items(50));
		robot.interact(() -> pane.getChildren().add(list));

		// Assert init
		assertState(list, IntegerRange.of(0, 16));
		assertCounter(17, 1, 17, 17, 0, 0);

		robot.interact(() -> list.setVPos(Double.MAX_VALUE));
		counter.reset(); // Reset otherwise wrong statistics

		// Remove all at end
		robot.interact(() -> Utils.removeAll(list.getItems(), 49, 48, 47, 46));
		assertState(list, IntegerRange.of(29, 45));
		assertCounter(0, 1, 4, 4, 0, 0);
		assertEquals(list.getCellSize() * list.size() - list.getHeight(), list.getVPos());

		// Remove at end (for)
		robot.interact(() -> {
			for (int i = 0; i < 4; i++) list.getItems().removeLast();
		});
		assertState(list, IntegerRange.of(25, 41));
		assertCounter(0, 4, 4, 4, 0, 0);
		assertEquals(list.getCellSize() * list.size() - list.getHeight(), list.getVPos());

		// Remove before no intersect
		robot.interact(() -> Utils.removeAll(list.getItems(), 0, 1, 2));
		assertState(list, IntegerRange.of(22, 38));
		assertCounter(0, 1, 17, 0, 0, 0);
		assertEquals(list.getCellSize() * list.size() - list.getHeight(), list.getVPos());

		// Remove before intersect
		robot.interact(() -> Utils.removeAll(list.getItems(), 20, 21, 22, 23));
		assertState(list, IntegerRange.of(18, 34));
		assertCounter(0, 1, 17, 2, 0, 0);
		assertEquals(list.getCellSize() * list.size() - list.getHeight(), list.getVPos());

		// Remove enough to cache cells
		robot.interact(() -> Utils.removeAll(list.getItems(), IntegerRange.of(15, 34)));
		assertState(list, IntegerRange.of(0, 14));
		assertCounter(0, 1, 15, 15, 0, 2);
		assertEquals(list.getCellSize() * list.size() - list.getHeight(), list.getVPos());
	}

	@Test
	void testRemoveSparse(FxRobot robot) {
		StackPane pane = setupStage();
		List list = new List(items(50));
		robot.interact(() -> pane.getChildren().add(list));

		// Assert init
		assertState(list, IntegerRange.of(0, 16));
		assertCounter(17, 1, 17, 17, 0, 0);

		robot.interact(() -> Utils.removeAll(list.getItems(), 0, 3, 4, 8, 10, 11));
		assertState(list, IntegerRange.of(0, 16));
		assertCounter(0, 1, 17, 6, 0, 0);
	}

	@Test
	void testSwitchOrientation(FxRobot robot) {
		StackPane pane = setupStage();
		List list = new List(items(50));
		robot.interact(() -> pane.getChildren().add(list));
		counter.reset();

		robot.interact(() -> list.setOrientation(Orientation.HORIZONTAL));
		assertState(list, IntegerRange.of(0, 16));
		assertCounter(0, 1, 0, 0, 0, 0);

		robot.interact(() -> list.scrollToPixel(600.0));
		assertEquals(0.0, list.getVPos());
		assertEquals(600.0, list.getHPos());
		assertState(list, IntegerRange.of(16, 32));
		assertCounter(0, 1, 16, 16, 0, 0);

		robot.interact(() -> list.scrollToPixel(Double.MAX_VALUE));
		assertEquals(0.0, list.getVPos());
		assertEquals(1200.0, list.getHPos());
		assertState(list, IntegerRange.of(33, 49));
		assertCounter(0, 1, 17, 17, 0, 0);

		robot.interact(() -> list.setOrientation(Orientation.VERTICAL));
		assertEquals(0.0, list.getVPos());
		assertEquals(0.0, list.getHPos());
		assertState(list, IntegerRange.of(0, 16));
		assertCounter(0, 1, 17, 17, 0, 0);
	}

	@Test
	void testSwitchOrientationFitToBreadth(FxRobot robot) {
		StackPane pane = setupStage();
		List list = new List(items(50));
		robot.interact(() -> {
			list.setOrientation(Orientation.HORIZONTAL);
			pane.getChildren().add(list);
		});

		// Assert init
		assertState(list, IntegerRange.of(0, 16));
		assertCounter(17, 2, 17, 17, 0, 0);

		// Change factory
		robot.interact(() -> list.setCellFactory(i -> new SimpleCell(i) {
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
		assertState(list, IntegerRange.of(0, 16));
		assertCounter(17, 1, 17, 17, 0, 17);
		assertEquals(400.0, list.getHelper().maxBreadthProperty().get());

		// Allow variable height
		robot.interact(() -> {
			list.setFitToBreadth(false);
			list.setVPos(Double.MAX_VALUE);
		});
		assertEquals(100.0, list.getVPos());
		assertEquals(0.0, list.getHPos());
		assertState(list, IntegerRange.of(0, 16));
		assertCounter(0, 1, 0, 0, 0, 0);
		assertEquals(500.0, list.getHelper().maxBreadthProperty().get());

		robot.interact(() -> {
			list.setHPos(Double.MAX_VALUE);
			counter.reset();
			list.setOrientation(Orientation.VERTICAL);
		});
		assertEquals(0.0, list.getVPos());
		assertEquals(0.0, list.getHPos());
		assertState(list, IntegerRange.of(0, 16));
		assertCounter(0, 1, 17, 17, 0, 0);
		assertEquals(500.0, list.getHelper().maxBreadthProperty().get());
	}

	@Test
	void testSpacing(FxRobot robot) {
		StackPane pane = setupStage();
		List list = new List(items(50));
		robot.interact(() -> pane.getChildren().add(list));

		// Assert init
		assertState(list, IntegerRange.of(0, 16));
		assertCounter(17, 1, 17, 17, 0, 0);

		// Set spacing 10.0
		robot.interact(() -> list.setSpacing(10.0));
		assertState(list, IntegerRange.of(0, 13));
		assertCounter(0, 1, 0, 0, 0, 3);

		// Set spacing 30.0
		robot.interact(() -> list.setSpacing(30.0));
		assertState(list, IntegerRange.of(0, 10));
		assertCounter(0, 1, 0, 0, 0, 3);

		// Try scrolling
		robot.interact(() -> list.setVPos(100.0));
		assertState(list, IntegerRange.of(0, 10));
		assertCounter(0, 0, 0, 0, 0, 0);

		// Scroll more
		robot.interact(() -> list.scrollToIndex(5));
		assertEquals(310.0, list.getVPos());
		assertState(list, IntegerRange.of(3, 13));
		assertCounter(0, 1, 3, 3, 0, 0);

		// Scroll to end
		robot.interact(() -> list.setVPos(Double.MAX_VALUE));
		assertState(list, IntegerRange.of(39, 49));
		assertCounter(0, 1, 11, 11, 0, 0);

		// Remove spacing
		robot.interact(() -> list.setSpacing(0.0));
		assertState(list, IntegerRange.of(33, 49));
		assertCounter(0, 1, 6, 6, 6, 0);
	}
}