/*
 * Copyright (C) 2024 Parisi Alessandro - alessandro.parisi406@gmail.com
 * This file is part of VirtualizedFX (https://github.com/palexdev/VirtualizedFX)
 *
 * VirtualizedFX is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * VirtualizedFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with VirtualizedFX. If not, see <http://www.gnu.org/licenses/>.
 */

package interactive.grid;

import cells.TestCell;
import cells.TestGridCell;
import interactive.grid.GridTestUtils.Grid;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.controls.Label;
import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.mfxcore.utils.GridUtils;
import io.github.palexdev.mfxcore.utils.RandomUtils;
import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import io.github.palexdev.virtualizedfx.enums.BufferSize;
import io.github.palexdev.virtualizedfx.grid.VFXGrid;
import io.github.palexdev.virtualizedfx.grid.VFXGridHelper;
import io.github.palexdev.virtualizedfx.grid.VFXGridSkin;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import static interactive.grid.GridTestUtils.assertLength;
import static interactive.grid.GridTestUtils.assertState;
import static io.github.palexdev.virtualizedfx.utils.Utils.INVALID_RANGE;
import static model.User.faker;
import static model.User.users;
import static org.junit.jupiter.api.Assertions.*;
import static utils.TestFXUtils.*;
import static utils.Utils.*;

@ExtendWith(ApplicationExtension.class)
public class GridTests {

	/*
	 * Many of the numbers you see here may make no sense to you. Well, that's because a 2D structure like the Grid
	 * definitely makes things a lot more complex to visualize in our brain, which is more used to think in one dimension.
	 * For this reason, to confirm the numbers, to visualize the changes, it's highly recommended to use the GridVisualizer "app"
	 */

	@Start
	void start(Stage stage) {stage.show();}

	@BeforeEach
	void setup() {
		resetCounters();
	}

	@Test
	void testHelperDefault(FxRobot robot) {
		StackPane pane = setupStage();
		Grid grid = new Grid(items(50));
		robot.interact(() -> pane.getChildren().add(grid));

		VFXGridHelper<Integer, VFXCell<Integer>> helper = grid.getHelper();
		assertNotNull(helper);
		assertEquals(5, helper.maxColumns());
		assertEquals(4, helper.visibleColumns());
		assertEquals(5, helper.totalColumns());
		assertEquals(IntegerRange.of(0, 4), helper.columnsRange());
		assertEquals(10, helper.maxRows());
		assertEquals(4, helper.visibleRows());
		assertEquals(6, helper.totalRows());
		assertEquals(IntegerRange.of(0, 5), helper.rowsRange());
	}

	@Test
	void testHelperComplex(FxRobot robot) {
		StackPane pane = setupStage();
		Grid grid = new Grid(items(50));
		VFXGridHelper<Integer, VFXCell<Integer>> helper = grid.getHelper();
		robot.interact(() -> pane.getChildren().add(grid));
		assertNotNull(helper);

		// Test 1
		robot.interact(() -> {
			grid.setColumnsNum(10);
			grid.setSpacing(10);
		});
		assertEquals(10, helper.maxColumns());
		assertEquals(4, helper.visibleColumns());
		assertEquals(6, helper.totalColumns());
		assertEquals(IntegerRange.of(0, 5), helper.columnsRange());
		assertEquals(5, helper.maxRows());
		assertEquals(4, helper.visibleRows());
		assertEquals(5, helper.totalRows());
		assertEquals(IntegerRange.of(0, 4), helper.rowsRange());

		// Test 2
		robot.interact(() -> grid.setColumnsNum(20));
		assertEquals(20, helper.maxColumns());
		assertEquals(4, helper.visibleColumns()); // 10 because there is 10px spacing
		assertEquals(6, helper.totalColumns());
		assertEquals(IntegerRange.of(0, 5), helper.columnsRange());
		assertEquals(3, helper.maxRows());
		assertEquals(4, helper.visibleRows()); // 10 because there is 10px spacing
		assertEquals(3, helper.totalRows());
		assertEquals(IntegerRange.of(0, 2), helper.rowsRange());

		// Test 3
		robot.interact(() -> grid.setColumnsNum(51)); // More than items num
		assertEquals(50, helper.maxColumns());
		assertEquals(4, helper.visibleColumns()); // 10 because there is 10px spacing
		assertEquals(6, helper.totalColumns());
		assertEquals(IntegerRange.of(0, 5), helper.columnsRange());
		assertEquals(1, helper.maxRows());
		assertEquals(4, helper.visibleRows()); // 10 because there is 10px spacing
		assertEquals(1, helper.totalRows());
		assertEquals(IntegerRange.of(0, 0), helper.rowsRange());

		// What happens if we add items now in Test 3?
		robot.interact(() -> {
			grid.getItems().addAll(items(10));
			grid.setColumnsNum(51);
		});
		assertEquals(51, helper.maxColumns());
		assertEquals(4, helper.visibleColumns()); // 10 because there is 10px spacing
		assertEquals(6, helper.totalColumns());
		assertEquals(IntegerRange.of(0, 5), helper.columnsRange());
		assertEquals(2, helper.maxRows());
		assertEquals(4, helper.visibleRows()); // 10 because there is 10px spacing
		assertEquals(2, helper.totalRows());
		assertEquals(IntegerRange.of(0, 1), helper.rowsRange());
	}

	@Test
	void testInitAndGeometry(FxRobot robot) {
		StackPane pane = setupStage();
		Grid grid = new Grid(items(50));
		grid.setAlignment(Pos.CENTER); // This is to ensure that assertPositions doesn't depend on the alignment
		robot.interact(() -> pane.getChildren().add(grid));

		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 4));
		assertCounter(30, 1, 30, 30, 0, 0, 0);

		// Expand and test again
		robot.interact(() -> setWindowSize(pane, 600, -1)); // Unchanged because columns are already at max
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 4));
		assertCounter(0, 0, 0, 0, 0, 0, 0);

		robot.interact(() -> setWindowSize(pane, -1, 600));
		assertState(grid, IntegerRange.of(0, 7), IntegerRange.of(0, 4));
		assertCounter(10, 1, 10, 10, 0, 0, 0);

		// Shrink and test again
		robot.interact(() -> setWindowSize(pane, 300, -1)); // Unchanged because of double buffer
		assertState(grid, IntegerRange.of(0, 7), IntegerRange.of(0, 4));
		assertCounter(0, 0, 0, 0, 0, 0, 0);

		robot.interact(() -> setWindowSize(pane, -1, 300));
		assertState(grid, IntegerRange.of(0, 4), IntegerRange.of(0, 4));
		assertCounter(0, 1, 0, 0, 0, 15, 5);

		robot.interact(() -> setWindowSize(pane, -1, 500));
		assertState(grid, IntegerRange.of(0, 6), IntegerRange.of(0, 4));
		assertCounter(0, 1, 10, 10, 10, 0, 0);

		// Edge case set width to 0
		robot.interact(() -> {
			grid.setMinWidth(0);
			grid.setPrefWidth(0);
			grid.setMaxWidth(0);
			robot.interact(() -> setWindowSize(pane, 0, -1));
		});
		assertState(grid, INVALID_RANGE, INVALID_RANGE);
		assertCounter(0, 0, 0, 0, 0, 35, 25);
	}

	@Test
	void testInitAndGeometryMaxX(FxRobot robot) {
		StackPane pane = setupStage();
		Grid grid = new Grid(items(50));
		robot.interact(() -> {
			pane.getChildren().add(grid);
			grid.scrollToLastColumn();
		});

		// Check hPos!!
		assertEquals(100, grid.getHPos());

		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 4));
		assertCounter(30, 1, 30, 30, 0, 0, 0);

		// Expand (won't have any effect since columns are already all shown)
		robot.interact(() -> setWindowSize(pane, 600, -1));
		assertEquals(0, grid.getHPos());
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 4));
		assertCounter(0, 0, 0, 0, 0, 0, 0);

		// Shrink
		robot.interact(() -> {
			robot.interact(() -> setWindowSize(pane, 100, -1));
			// Why you be like that JavaFX :smh:
			grid.setMinWidth(100.0);
			grid.setPrefWidth(100.0);
			grid.setMaxWidth(100.0);
		});
		assertEquals(0, grid.getHPos());
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 2));
		assertCounter(0, 1, 0, 0, 0, 12, 2); // 12 because we lose 2 columns so 6 rows * 2
	}

	@Test
	void testInitAndGeometryMaxY(FxRobot robot) {
		StackPane pane = setupStage();
		Grid grid = new Grid(items(50));
		robot.interact(() -> {
			pane.getChildren().add(grid);
			grid.scrollToLastRow();
		});

		// Check vPos!!
		assertEquals(600, grid.getVPos());

		assertState(grid, IntegerRange.of(4, 9), IntegerRange.of(0, 4));
		assertCounter(30, 1, 30, 30, 0, 0, 0);


		// Expand
		robot.interact(() -> setWindowSize(pane, -1, 600));
		assertEquals(400, grid.getVPos());
		assertState(grid, IntegerRange.of(2, 9), IntegerRange.of(0, 4));
		assertCounter(10, 1, 10, 10, 0, 0, 0);

		// Shrink
		robot.interact(() -> setWindowSize(pane, -1, 300));
		assertEquals(400, grid.getVPos());
		assertState(grid, IntegerRange.of(3, 7), IntegerRange.of(0, 4));
		assertCounter(0, 1, 0, 0, 0, 15, 5);
	}

	@Test
	void testPopulateCache(FxRobot robot) {
		StackPane pane = setupStage();
		Grid grid = new Grid(items(80));
		grid.populateCache(); // 10 by default
		counter.reset(); // Cells created by cache do not count
		assertEquals(10, grid.cacheSize());
		robot.interact(() -> pane.getChildren().add(grid));

		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 4));
		assertCounter(20, 1, 30, 30, 10, 0, 0);
		assertEquals(0, grid.cacheSize());

		robot.interact(() -> setWindowSize(pane, -1, 600));
		assertState(grid, IntegerRange.of(0, 7), IntegerRange.of(0, 4));
		assertCounter(10, 1, 10, 10, 0, 0, 0);

		robot.interact(() -> setWindowSize(pane, -1, 200));
		assertState(grid, IntegerRange.of(0, 3), IntegerRange.of(0, 4));
		assertCounter(0, 1, 0, 0, 0, 20, 10);
		assertEquals(10, grid.cacheSize());

		// Re-expand and cache should put in some work
		robot.interact(() -> setWindowSize(pane, -1, 600));
		assertState(grid, IntegerRange.of(0, 7), IntegerRange.of(0, 4));
		assertCounter(10, 1, 20, 20, 10, 0, 0);
		assertEquals(0, grid.cacheSize());
		// For a 2D structure like the grid, maybe it's better to have a bigger cache
		// As you can see from the above test, only 10 of the 20 lost cells (after the shrink) have been
		// retrieved from the cache. The remaining created from scratch
	}

	@Test
	void testScrollVertical(FxRobot robot) {
		StackPane pane = setupStage();
		Grid grid = new Grid(items(100));
		grid.populateCache(); // 10 by default
		counter.reset();
		robot.interact(() -> pane.getChildren().add(grid));

		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 4));
		assertCounter(20, 1, 30, 30, 10, 0, 0);

		robot.interact(() -> grid.setVPos(400));
		assertState(grid, IntegerRange.of(3, 8), IntegerRange.of(0, 4));
		assertCounter(0, 1, 15, 15, 0, 0, 0);

		robot.interact(grid::scrollToLastRow);
		assertState(grid, IntegerRange.of(14, 19), IntegerRange.of(0, 4));
		assertCounter(0, 1, 30, 30, 0, 0, 0);

		robot.interact(() -> {
			grid.setItems(items(50));
			grid.scrollToFirstRow(); // Here range becomes [0, 5]
			counter.reset();
			grid.setVPos(300);
		});
		assertState(grid, IntegerRange.of(2, 7), IntegerRange.of(0, 4));
		assertCounter(0, 1, 10, 10, 0, 0, 0); // R. indexes 2,3,4,5 are in common
	}

	@Test
	void testScrollVerticalIncompleteRow(FxRobot robot) {
		StackPane pane = setupStage();
		Grid grid = new Grid(items(96));
		grid.populateCache(); // 10 by default
		counter.reset();
		robot.interact(() -> pane.getChildren().add(grid));

		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 4));
		assertCounter(20, 1, 30, 30, 10, 0, 0);

		robot.interact(() -> grid.setVPos(400));
		assertState(grid, IntegerRange.of(3, 8), IntegerRange.of(0, 4));
		assertCounter(0, 1, 15, 15, 0, 0, 0);

		robot.interact(grid::scrollToLastRow);
		assertState(grid, IntegerRange.of(14, 19), IntegerRange.of(0, 4));
		assertCounter(0, 1, 26, 26, 0, 4, 0);

		robot.interact(() -> grid.setItems(items(53)));
		assertState(grid, IntegerRange.of(5, 10), IntegerRange.of(0, 4));
		assertCounter(0, 1, 28, 28, 2, 0, 0);
		assertEquals(2, grid.cacheSize());

		robot.interact(grid::scrollToFirstRow);
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 4));
		assertCounter(0, 1, 25, 25, 2, 0, 0);
		assertEquals(0, grid.cacheSize());

		robot.interact(() -> grid.setVPos(300));
		assertState(grid, IntegerRange.of(2, 7), IntegerRange.of(0, 4));
		assertCounter(0, 1, 10, 10, 0, 0, 0); // R. indexes 2,3,4,5 are in common
	}

	@Test
	void testScrollHorizontal(FxRobot robot) {
		StackPane pane = setupStage();
		Grid grid = new Grid(items(100));
		grid.populateCache(); // 10 by default
		counter.reset();
		robot.interact(() -> {
			grid.setColumnsNum(10); // Need more columns to test
			pane.getChildren().add(grid);
		});

		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 5));
		assertCounter(26, 1, 36, 36, 10, 0, 0);

		robot.interact(() -> grid.setHPos(400));
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(3, 8));
		assertCounter(0, 1, 18, 18, 0, 0, 0);

		robot.interact(grid::scrollToLastColumn);
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(4, 9));
		assertCounter(0, 1, 6, 6, 0, 0, 0);

		robot.interact(() -> {
			grid.setItems(items(50));
			grid.scrollToFirstRow(); // Here range becomes [0, 5]
			counter.reset();
			grid.setHPos(300);
		});
		assertState(grid, IntegerRange.of(0, 4), IntegerRange.of(2, 7));
		assertCounter(0, 1, 10, 10, 0, 0, 0);
		assertEquals(6, grid.cacheSize());
	}

	@Test
	void testScrollHorizontalIncompleteRow(FxRobot robot) {
		StackPane pane = setupStage();
		Grid grid = new Grid(items(93));
		grid.populateCache(); // 10 by default
		counter.reset();
		robot.interact(() -> {
			grid.setColumnsNum(10); // Need more columns to test
			pane.getChildren().add(grid);
		});

		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 5));
		assertCounter(26, 1, 36, 36, 10, 0, 0);

		robot.interact(() -> grid.setHPos(400));
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(3, 8));
		assertCounter(0, 1, 18, 18, 0, 0, 0);

		robot.interact(grid::scrollToLastColumn);
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(4, 9));
		assertCounter(0, 1, 6, 6, 0, 0, 0);

		robot.interact(grid::scrollToLastRow);
		assertState(grid, IntegerRange.of(4, 9), IntegerRange.of(4, 9));
		assertCounter(0, 1, 18, 18, 0, 6, 0);

		// Common items are from 44 to 54. We need one extra cell 30 -> 31. So, 1 de-cache, 31 - (54 - 44) = 24 updates
		robot.interact(() -> grid.setItems(items(55)));
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(4, 9));
		assertCounter(0, 1, 24, 24, 1, 0, 0);
		assertEquals(5, grid.cacheSize());

		// We need 35 cells now, because the last visible item would be 55 (which is not in the list), therefore 4 de-caches
		robot.interact(grid::scrollToFirstColumn);
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 5));
		assertCounter(0, 1, 24, 24, 4, 0, 0);
		assertEquals(1, grid.cacheSize());

		// 23 are in common. 3 are non-existent (in the last row). 36 - 23 - 3 = 10 updates
		robot.interact(() -> grid.setHPos(300));
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(2, 7));
		assertCounter(0, 1, 10, 10, 0, 2, 0);
		assertEquals(3, grid.cacheSize());
	}

	@Test
	void testBufferChangeTopLeft(FxRobot robot) {
		StackPane pane = setupStage();
		Grid grid = new Grid(items(80));
		VFXGridHelper<Integer, VFXCell<Integer>> helper = grid.getHelper();
		robot.interact(() -> {
			grid.setColumnsNum(10); // To test both buffers
			pane.getChildren().add(grid);
		});

		// Small buffer
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 5));
		assertEquals(16, helper.visibleCells());
		assertEquals(36, helper.totalCells());
		assertCounter(36, 1, 36, 36, 0, 0, 0);

		// Medium buffer
		robot.interact(() -> grid.setBufferSize(BufferSize.MEDIUM));
		assertState(grid, IntegerRange.of(0, 7), IntegerRange.of(0, 7));
		assertEquals(16, helper.visibleCells());
		assertEquals(64, helper.totalCells());
		assertCounter(28, 1, 28, 28, 0, 0, 0);

		// Big buffer (not enough items)
		robot.interact(() -> grid.setBufferSize(BufferSize.BIG));
		assertState(grid, IntegerRange.of(0, 7), IntegerRange.of(0, 9));
		assertEquals(16, helper.visibleCells());
		assertEquals(80, helper.totalCells());
		assertCounter(16, 1, 16, 16, 0, 0, 0);
	}

	@Test
	void testBufferChangeMiddle(FxRobot robot) {
		StackPane pane = setupStage();
		Grid grid = new Grid(items(100));
		VFXGridHelper<Integer, VFXCell<Integer>> helper = grid.getHelper();
		robot.interact(() -> {
			grid.setColumnsNum(10);
			grid.setHPos(300);
			grid.setVPos(300);
			pane.getChildren().add(grid);
		});

		// Small buffer
		assertState(grid, IntegerRange.of(2, 7), IntegerRange.of(2, 7));
		assertEquals(16, helper.visibleCells());
		assertEquals(36, helper.totalCells());
		assertCounter(36, 1, 36, 36, 0, 0, 0);

		// Medium buffer
		robot.interact(() -> grid.setBufferSize(BufferSize.MEDIUM));
		assertState(grid, IntegerRange.of(1, 8), IntegerRange.of(1, 8));
		assertEquals(16, helper.visibleCells());
		assertEquals(64, helper.totalCells());
		assertCounter(28, 1, 28, 28, 0, 0, 0);

		// Big buffer
		robot.interact(() -> grid.setBufferSize(BufferSize.BIG));
		assertState(grid, IntegerRange.of(0, 9), IntegerRange.of(0, 9));
		assertEquals(16, helper.visibleCells());
		assertEquals(100, helper.totalCells());
		assertCounter(36, 1, 36, 36, 0, 0, 0);
	}

	@Test
	void testBufferChangeBottomRight(FxRobot robot) {
		StackPane pane = setupStage();
		Grid grid = new Grid(items(80));
		VFXGridHelper<Integer, VFXCell<Integer>> helper = grid.getHelper();
		robot.interact(() -> {
			grid.setColumnsNum(10);
			grid.scrollToLastColumn();
			grid.scrollToLastRow();
			pane.getChildren().add(grid);
		});

		// Small buffer
		assertState(grid, IntegerRange.of(2, 7), IntegerRange.of(4, 9));
		assertEquals(16, helper.visibleCells());
		assertEquals(36, helper.totalCells());
		assertCounter(36, 1, 36, 36, 0, 0, 0);

		// Medium buffer
		robot.interact(() -> grid.setBufferSize(BufferSize.MEDIUM));
		assertState(grid, IntegerRange.of(0, 7), IntegerRange.of(2, 9));
		assertEquals(16, helper.visibleCells());
		assertEquals(64, helper.totalCells());
		assertCounter(28, 1, 28, 28, 0, 0, 0);

		// Big buffer (not enough items)
		robot.interact(() -> grid.setBufferSize(BufferSize.BIG));
		assertState(grid, IntegerRange.of(0, 7), IntegerRange.of(0, 9));
		assertEquals(16, helper.visibleCells());
		assertEquals(80, helper.totalCells());
		assertCounter(16, 1, 16, 16, 0, 0, 0);
	}

	@Test
	void testChangeFactory(FxRobot robot) {
		StackPane pane = setupStage();
		Grid grid = new Grid(items(100));
		VFXGridHelper<Integer, VFXCell<Integer>> helper = grid.getHelper();
		robot.interact(() -> {
			grid.setColumnsNum(10); // To allow horizontal movement
			pane.getChildren().add(grid);
		});

		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 5));
		assertCounter(36, 1, 36, 36, 0, 0, 0);

		// Test at both pos != 0
		robot.interact(() -> {
			grid.setVPos(300);
			grid.setHPos(300);
		});
		assertState(grid, IntegerRange.of(2, 7), IntegerRange.of(2, 7));
		assertCounter(0, 2, 24, 24, 0, 0, 0);
		// Counter's stats are doubled because we scroll two times

		// Change factory and test
		robot.interact(() ->
			grid.setCellFactory(t -> new TestGridCell(t) {
				{
					setConverter(t -> "New Factory! Index: %d Item: %s".formatted(
						getIndex(),
						t != null ? t.toString() : "")
					);
				}
			})
		);
		assertState(grid, IntegerRange.of(2, 7), IntegerRange.of(2, 7));
		assertEquals(16, helper.visibleCells());
		assertEquals(36, helper.totalCells());
		assertCounter(36, 1, 36, 36, 0, 36, 36);
		// The cache comes from the old state disposal, but those cells are then immediately
		// disposed and dropped by the cache, in fact there is no de-cache afterward
	}

	@Test
	void testChangeCellSizeTopLeft(FxRobot robot) {
		StackPane pane = setupStage();
		Grid grid = new Grid(items(50));
		robot.interact(() -> pane.getChildren().add(grid));

		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 4));
		assertCounter(30, 1, 30, 30, 0, 0, 0);
		assertLength(grid, (50.0 / 5) * 100, 5 * 100);

		// Decrease and test
		robot.interact(() -> grid.setCellSize(50));
		assertState(grid, IntegerRange.of(0, 9), IntegerRange.of(0, 4));
		assertCounter(20, 1, 20, 20, 0, 0, 0);
		assertLength(grid, (50.0 / 5) * 50, 5 * 50);

		// Increase and test
		robot.interact(() -> grid.setCellSize(250, 150));
		assertState(grid, IntegerRange.of(0, 4), IntegerRange.of(0, 3));
		assertCounter(0, 1, 0, 0, 0, 30, 20);
		assertLength(grid, (50.0 / 5) * 150, 5 * 250);
		assertEquals(10, grid.cacheSize());
	}

	@Test
	void testChangeCellSizeMiddle(FxRobot robot) {
		StackPane pane = setupStage();
		Grid grid = new Grid(items(300)); // To have some wiggle room :)
		robot.interact(() -> {
			grid.setColumnsNum(15);
			pane.getChildren().add(grid);
			grid.setVPos(550);
			grid.setHPos(550);
		});

		// Check positions!!!
		assertEquals(550, grid.getVPos());
		assertEquals(550, grid.getHPos());

		assertState(grid, IntegerRange.of(4, 9), IntegerRange.of(4, 9));
		assertCounter(36, 1, 36, 36, 0, 0, 0);
		assertLength(grid, (300.0 / 15) * 100, 15 * 100);

		// Decrease and test
		robot.interact(() -> grid.setCellSize(40));
		assertEquals(400, grid.getVPos());
		assertEquals(200, grid.getHPos());
		assertState(grid, IntegerRange.of(8, 19), IntegerRange.of(3, 14));
		assertCounter(108, 1, 132, 132, 0, 0, 0);
		assertLength(grid, (300.0 / 15) * 40, 15 * 40);
		// 144 - 36 = 108 new cells
		// 12 are in common
		// Therefore 144 - 12 = 132 total updates

		// Increase and test
		// Positions do not change!
		robot.interact(() -> grid.setCellSize(170));
		assertEquals(400, grid.getVPos());
		assertEquals(200, grid.getHPos());
		assertState(grid, IntegerRange.of(1, 5), IntegerRange.of(0, 4));
		assertCounter(0, 1, 25, 25, 0, 119, 109);
		assertLength(grid, (300.0 / 15) * 170, 15 * 170);
		assertEquals(10, grid.cacheSize());
	}

	@Test
	void testChangeCellSizeBottomRight(FxRobot robot) {
		StackPane pane = setupStage();
		Grid grid = new Grid(items(200));
		robot.interact(() -> {
			grid.setColumnsNum(10);
			pane.getChildren().add(grid);
			grid.scrollToLastRow();
			grid.scrollToLastColumn();
		});

		// Check positions!!!
		assertEquals(1600, grid.getVPos());
		assertEquals(600, grid.getHPos());

		assertState(grid, IntegerRange.of(14, 19), IntegerRange.of(4, 9));
		assertCounter(36, 1, 36, 36, 0, 0, 0);
		assertLength(grid, (200.0 / 10) * 100, 10 * 100);

		// Decrease and test
		robot.interact(() -> grid.setCellSize(50));
		assertEquals(600, grid.getVPos());
		assertEquals(100, grid.getHPos());
		assertState(grid, IntegerRange.of(10, 19), IntegerRange.of(0, 9));
		assertCounter(64, 1, 64, 64, 0, 0, 0);
		assertLength(grid, (200.0 / 10) * 50, 10 * 50);
		// 100 - 36 = 64 new cells
		// 36 are in common
		// Therefore 100 - 36 = 64 total updates

		// Increase and test
		robot.interact(() -> grid.setCellSize(145));
		assertEquals(600, grid.getVPos());
		assertEquals(100, grid.getHPos());
		assertState(grid, IntegerRange.of(3, 7), IntegerRange.of(0, 4));
		assertCounter(0, 1, 25, 25, 0, 75, 65);
		assertLength(grid, (200.0 / 10) * 145, 10 * 145);
	}

	@Test
	void testChangeCellSizeTo0(FxRobot robot) {
		StackPane pane = setupStage();
		Grid grid = new Grid(items(100));
		robot.interact(() -> pane.getChildren().add(grid));

		// Init test
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 4));
		assertCounter(30, 1, 30, 30, 0, 0, 0);

		robot.interact(() -> grid.setCellSize(0, 0));
		assertState(grid, INVALID_RANGE, INVALID_RANGE);
		assertCounter(0, 0, 0, 0, 0, 30, 20);
	}

	@Test
	void testChangeColumnsNumTopLeft(FxRobot robot) {
		StackPane pane = setupStage();
		Grid grid = new Grid(items(100));
		robot.interact(() -> pane.getChildren().add(grid));

		// Init test
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 4));
		assertCounter(30, 1, 30, 30, 0, 0, 0);

		// Increase and test
		robot.interact(() -> grid.setColumnsNum(10));
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 5));
		assertCounter(6, 1, 18, 18, 0, 0, 0);

		// Increase again to test edge case
		robot.interact(() -> grid.setColumnsNum(25));
		assertState(grid, IntegerRange.of(0, 3), IntegerRange.of(0, 5));
		assertCounter(0, 1, 10, 10, 0, 12, 2);

		// Decrease and test
		robot.interact(() -> grid.setColumnsNum(3));
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 2));
		assertCounter(0, 1, 12, 12, 0, 6, 6);

		// Decrease again to test edge case
		robot.interact(() -> grid.setColumnsNum(1));
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 0));
		assertCounter(0, 1, 0, 0, 0, 12, 12);

		// Edge case set to items num
		robot.interact(() -> grid.setColumnsNum(100));
		assertState(grid, IntegerRange.of(0, 0), IntegerRange.of(0, 5));
		assertCounter(0, 1, 0, 0, 0, 0, 0);

		// Edge case set to 0
		robot.interact(() -> grid.setColumnsNum(0));
		assertState(grid, INVALID_RANGE, INVALID_RANGE);
		assertCounter(0, 0, 0, 0, 0, 6, 6);
	}

	@Test
	void testChangeColumnsNumMiddle(FxRobot robot) {
		StackPane pane = setupStage();
		Grid grid = new Grid(items(100));
		robot.interact(() -> {
			// Let's start with 10 columns, so I can get a high enough hPos
			grid.setColumnsNum(10);
			grid.setHPos(300);
			grid.setVPos(300);
			pane.getChildren().add(grid);
		});

		// Init test
		assertState(grid, IntegerRange.of(2, 7), IntegerRange.of(2, 7));
		assertCounter(36, 1, 36, 36, 0, 0, 0);

		// Increase and test
		robot.interact(() -> grid.setColumnsNum(20));
		assertEquals(100, grid.getVPos());
		assertEquals(300, grid.getHPos());
		assertState(grid, IntegerRange.of(0, 4), IntegerRange.of(2, 7));
		assertCounter(0, 1, 12, 12, 0, 6, 0);

		// Decrease and test
		robot.interact(() -> grid.setColumnsNum(5));
		assertEquals(100, grid.getVPos());
		assertEquals(100, grid.getHPos());
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 4));
		assertCounter(0, 1, 18, 18, 0, 0, 0);
	}

	@Test
	void testChangeColumnsNumBottomRight(FxRobot robot) {
		StackPane pane = setupStage();
		Grid grid = new Grid(items(100));
		robot.interact(() -> {
			// Let's start with 10 columns, so I can get a high enough hPos
			grid.setColumnsNum(10);
			grid.scrollToLastColumn();
			grid.scrollToLastRow();
			pane.getChildren().add(grid);
		});

		// Init test
		assertState(grid, IntegerRange.of(4, 9), IntegerRange.of(4, 9));
		assertCounter(36, 1, 36, 36, 0, 0, 0);

		// Increase and test
		robot.interact(() -> grid.setColumnsNum(20));
		assertEquals(100, grid.getVPos());
		assertEquals(600, grid.getHPos());
		assertState(grid, IntegerRange.of(0, 4), IntegerRange.of(5, 10));
		assertCounter(0, 1, 15, 15, 0, 6, 0);

		// Decrease and test
		robot.interact(() -> grid.setColumnsNum(5));
		assertEquals(100, grid.getVPos());
		assertEquals(100, grid.getHPos());
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 4));
		assertCounter(0, 1, 19, 19, 0, 0, 0);
	}

	@Test
	void testChangeColumnsNumNoRangeChanges(FxRobot robot) {
		StackPane pane = setupStage();
		Grid grid = new Grid(items(200));
		robot.interact(() -> {
			grid.setColumnsNum(10);
			pane.getChildren().add(grid);
		});

		/*
		 * This test proves that the update of the cells is needed even if the ranges do not change.
		 * The reason behind this is pretty simple: when the number of columns changes, the items are arranged in a
		 * different way from before (from a 2D perspective).
		 */

		// Test init
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 5));
		assertCounter(36, 1, 36, 36, 0, 0, 0);

		// Increase and test
		robot.interact(() -> grid.setColumnsNum(11));
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 5));
		assertCounter(0, 1, 15, 15, 0, 0, 0);

		// Decrease and test
		robot.interact(() -> grid.setColumnsNum(9));
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 5));
		assertCounter(0, 1, 15, 15, 0, 0, 0);

		// I don't know why 15, but the GridVisualizer seem to confirm it
	}

	@Test
	void testSpacing(FxRobot robot) {
		StackPane pane = setupStage();
		Grid grid = new Grid(items(200));
		robot.interact(() -> {
			grid.setColumnsNum(10);
			pane.getChildren().add(grid);
		});

		// Test init
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 5));
		assertCounter(36, 1, 36, 36, 0, 0, 0);

		// Set spacing and test
		// No changes aside from layout
		robot.interact(() -> grid.setSpacing(20));
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 5));
		assertCounter(0, 2, 0, 0, 0, 0, 0);

		// Increase and test again
		robot.interact(() -> grid.setSpacing(50)); // High enough to cause the ranges to change
		assertState(grid, IntegerRange.of(0, 4), IntegerRange.of(0, 4));
		assertCounter(0, 2, 0, 0, 0, 11, 1);

		// 2 layouts because we are setting both vertical and horizontal spacings

		// Try scrolling
		robot.interact(() -> {
			grid.scrollToColumn(3);
			grid.scrollToRow(3);
		});
		assertEquals(450, grid.getHPos());
		assertEquals(450, grid.getVPos());
		assertState(grid, IntegerRange.of(2, 6), IntegerRange.of(2, 6));
		assertCounter(0, 2, 20, 20, 0, 0, 0);

		// Scroll some more
		robot.interact(() -> {
			grid.scrollToColumn(5);
			grid.scrollToRow(5);
		});
		assertEquals(750, grid.getHPos());
		assertEquals(750, grid.getVPos());
		assertState(grid, IntegerRange.of(4, 8), IntegerRange.of(4, 8));
		assertCounter(0, 2, 20, 20, 0, 0, 0);

		// Scroll to end
		robot.interact(() -> {
			grid.scrollToLastColumn();
			grid.scrollToLastRow();
		});
		assertEquals(1050, grid.getHPos());
		assertEquals(2550, grid.getVPos());
		assertState(grid, IntegerRange.of(15, 19), IntegerRange.of(5, 9));
		assertCounter(0, 2, 30, 30, 0, 0, 0);

		// Remove spacing
		robot.interact(() -> grid.setSpacing(0));
		assertEquals(600, grid.getHPos());
		assertEquals(1600, grid.getVPos());
		assertState(grid, IntegerRange.of(14, 19), IntegerRange.of(4, 9));
		assertCounter(1, 2, 11, 11, 10, 0, 0);
	}

	@Test
	void testAutoArrange(FxRobot robot) {
		/*
		 * We are not going to test the counter here since the type of change that occurs here is essentially
		 * 'testChangeColumnsNum...'
		 */
		StackPane pane = setupStage();
		Grid grid = new Grid(items(200));
		robot.interact(() -> {
			grid.setAlignment(Pos.TOP_CENTER);
			grid.setColumnsNum(10);
			When.onInvalidated(grid.widthProperty())
				.then(w -> grid.autoArrange())
				.invalidating(grid.hSpacingProperty())
				.listen();
			pane.getChildren().add(grid);
		});

		// Test init
		robot.interact(() -> grid.autoArrange());
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 3));
		// Note here that since the pane is 400px wide, we have 4 columns, therefore no buffer

		// Test at 450px
		robot.interact(() -> setWindowSize(pane, 450, -1));
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 3));

		// Test at 500px
		robot.interact(() -> setWindowSize(pane, 500, -1));
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 4));

		// Test with spacing (20px)
		robot.interact(() -> grid.setSpacing(20));
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 3));

		// Test at 450px
		robot.interact(() -> setWindowSize(pane, 450, -1));
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 2));

		// Test at 400px
		robot.interact(() -> setWindowSize(pane, 400, -1));
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 2));

		// Test at 200px
		robot.interact(() -> setWindowSize(pane, 200, -1));
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 0));

		// Auto-arrange with minimum
		robot.interact(() -> grid.autoArrange(6));
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 3));
		// Here the range is [0, 3] because 2 are buffers.
		// ceil(200 / 120) = 2 + 2 = 4

		// Test at 1000px
		robot.interact(() -> setWindowSize(pane, 1000, -1));
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 7));
	}

	@Test
	void testChangeList(FxRobot robot) {
		StackPane pane = setupStage();
		Grid grid = new Grid(items(100));
		robot.interact(() -> {
			grid.setColumnsNum(10);
			pane.getChildren().add(grid);
		});

		// Test init
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 5));
		assertCounter(36, 1, 36, 36, 0, 0, 0);

		// Change items property
		robot.interact(() -> grid.setItems(items(100, 100)));
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 5));
		assertCounter(0, 1, 0, 36, 0, 0, 0);

		// Change items property (fewer elements)
		robot.interact(() -> grid.setItems(items(100, 12)));
		assertState(grid, IntegerRange.of(0, 1), IntegerRange.of(0, 5));
		assertCounter(0, 1, 0, 0, 0, 28, 18);
		// We have only 8 cells because the last row has only two elements, therefore 28 cache

		// Change items property (more elements)
		robot.interact(() -> grid.setItems(items(150, 150)));
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 5));
		assertCounter(18, 1, 28, 36, 10, 0, 0);
		// 18 created because 10 are from cache
		// 28 index updates because only the previous 8 are ok to go

		// Scroll to middle
		robot.interact(() -> {
			grid.setHPos(300);
			grid.setVPos(550);
		});
		assertState(grid, IntegerRange.of(4, 9), IntegerRange.of(2, 7));
		assertCounter(0, 2, 36, 36, 0, 0, 0);

		// Change items property (fewer elements)
		robot.interact(() -> grid.setItems(items(50, 37)));
		assertEquals(300, grid.getHPos());
		assertEquals(0, grid.getVPos());
		assertState(grid, IntegerRange.of(0, 3), IntegerRange.of(2, 7));
		assertCounter(0, 1, 23, 23, 0, 13, 3);

		// Change items property (more elements)
		robot.interact(() -> grid.setItems(items(100, 100)));
		assertEquals(300, grid.getHPos());
		assertEquals(0, grid.getVPos());
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(2, 7));
		assertCounter(3, 1, 13, 36, 10, 0, 0);

		// Scroll to bottom
		robot.interact(() -> {
			grid.scrollToLastColumn();
			grid.scrollToLastRow();
		});
		assertState(grid, IntegerRange.of(4, 9), IntegerRange.of(4, 9));
		assertCounter(0, 2, 36, 36, 0, 0, 0);

		// Change items property (fewer elements)
		robot.interact(() -> grid.setItems(items(44)));
		assertEquals(600, grid.getHPos());
		assertEquals(100, grid.getVPos());
		assertState(grid, IntegerRange.of(0, 4), IntegerRange.of(4, 9));
		assertCounter(0, 1, 24, 24, 0, 12, 2);

		// Change items property (even fewer)
		robot.interact(() -> grid.setItems(items(7)));
		assertEquals(300, grid.getHPos());
		assertEquals(0, grid.getVPos());
		assertState(grid, IntegerRange.of(0), IntegerRange.of(1, 6));
		assertCounter(0, 1, 3, 3, 0, 18, 18);

		// Reset
		robot.interact(() -> {
			grid.setItems(items(100));
			grid.scrollToLastColumn();
			grid.scrollToLastRow();
		});
		counter.reset();
		// Change items property to empty
		robot.interact(() -> grid.setItems(null));
		assertEquals(0, grid.getHPos());
		assertEquals(0, grid.getVPos());
		assertState(grid, INVALID_RANGE, INVALID_RANGE);
		assertCounter(0, 0, 0, 0, 0, 36, 26);
		assertEquals(10, grid.cacheSize());
	}

	@Test
	void testPermutation(FxRobot robot) {
		StackPane pane = setupStage();
		Grid grid = new Grid(items(100));
		robot.interact(() -> {
			grid.setColumnsNum(10);
			pane.getChildren().add(grid);
		});

		// Test init
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 5));
		assertCounter(36, 1, 36, 36, 0, 0, 0);

		// Permutation change
		robot.interact(() -> FXCollections.sort(grid.getItems(), Comparator.reverseOrder()));
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 5));
		assertCounter(0, 1, 4, 32, 0, 0, 0);
		/*
		 * The peculiarity here is that cells with items 44, 45, 54, 55 are in common by item.
		 * In fact, as you can see, we have only 32 updates instead of 36, but their index change like this (thus the 4 index updates):
		 * Item 44 -> Index 55
		 * Item 45 -> Index 54
		 * Item 54 -> Index 45
		 * Item 55 -> Index 44
		 */
	}

	@Test
	void testSet(FxRobot robot) {
		StackPane pane = setupStage();
		Grid grid = new Grid(items(100));
		robot.interact(() -> {
			grid.setColumnsNum(10);
			pane.getChildren().add(grid);
		});

		// Test init
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 5));
		assertCounter(36, 1, 36, 36, 0, 0, 0);

		// Set all (more)
		robot.interact(() -> grid.getItems().setAll(items(100, 200)));
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 5));
		assertCounter(0, 1, 0, 36, 0, 0, 0);

		// Set all (fewer)
		robot.interact(() -> grid.getItems().setAll(items(100, 14)));
		assertState(grid, IntegerRange.of(0, 1), IntegerRange.of(0, 5));
		assertCounter(0, 1, 0, 0, 0, 26, 16);

		// Restore initial state, scroll to end and set all (more)
		robot.interact(() -> {
			grid.getItems().setAll(items(0, 100));
			grid.scrollToLastRow();
			grid.scrollToLastColumn();
		});
		counter.reset();
		robot.interact(() -> grid.getItems().setAll(items(0, 200)));
		assertEquals(600, grid.getHPos());
		assertEquals(600, grid.getVPos()); // The scroll is done when items count is 100
		assertState(grid, IntegerRange.of(5, 10), IntegerRange.of(4, 9));
		assertCounter(0, 1, 6, 6, 0, 0, 0);

		// Set random items
		robot.interact(() -> {
			for (int i = 0; i < 5; i++) {
				int rIdx = RandomUtils.random.nextInt(5, 11);
				int cIdx = RandomUtils.random.nextInt(4, 10);
				int index = GridUtils.subToInd(10, rIdx, cIdx);
				grid.getItems().set(index, 999 - i);
			}
		});
		assertState(grid, IntegerRange.of(5, 10), IntegerRange.of(4, 9));
		assertCounter(0, 5, 0, 5, 0, 0, 0);
	}

	@Test
	void testAddAt0(FxRobot robot) {
		StackPane pane = setupStage();
		Grid grid = new Grid(items(20));
		robot.interact(() -> pane.getChildren().add(grid));

		// Test init
		assertState(grid, IntegerRange.of(0, 3), IntegerRange.of(0, 4));
		assertCounter(20, 1, 20, 20, 0, 0, 0);

		// Add all at 0
		List<Integer> elements = IntStream.range(0, 3)
			.map(i -> 999 - i)
			.boxed()
			.toList();
		robot.interact(() -> grid.getItems().addAll(0, elements));
		assertState(grid, IntegerRange.of(0, 4), IntegerRange.of(0, 4));
		assertCounter(3, 1, 23, 3, 0, 0, 0);

		// Add at 0 (for)
		robot.interact(() -> {
			for (int i = 0; i < 4; i++) grid.getItems().addFirst(1999 - i);
		});
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 4));
		assertCounter(4, 4, 96, 4, 0, 0, 0); // A lot of index updates because of for cycle
	}

	@Test
	void testAddMiddle(FxRobot robot) {
		StackPane pane = setupStage();
		Grid grid = new Grid(items(100));
		robot.interact(() -> {
			grid.setColumnsNum(10);
			pane.getChildren().add(grid);
		});

		// Test init
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 5));
		assertCounter(36, 1, 36, 36, 0, 0, 0);

		// Scroll to middle
		robot.interact(() -> {
			grid.setHPos(300);
			grid.setVPos(300);
		});
		assertState(grid, IntegerRange.of(2, 7), IntegerRange.of(2, 7));
		assertCounter(0, 2, 24, 24, 0, 0, 0);

		// Add before no intersect (< 22)
		robot.interact(() -> grid.getItems().addAll(0, List.of(-1, -2, -3)));
		assertState(grid, IntegerRange.of(2, 7), IntegerRange.of(2, 7));
		assertCounter(0, 1, 36, 18, 0, 0, 0);
		// For every item added, we have the same amount of updates for each row. So, 3 * 6 = 18

		// Add before intersect (around 22)
		robot.interact(() -> grid.getItems().addAll(20, List.of(-4, -5, -6, -7)));
		assertState(grid, IntegerRange.of(2, 7), IntegerRange.of(2, 7));
		assertCounter(0, 1, 36, 24, 0, 0, 0);
		// Same logic as above, 4 * 6 = 24

		// Add in the middle of the range (> 22)
		robot.interact(() -> grid.getItems().addAll(50, List.of(-8, -9, -10, -11)));
		assertState(grid, IntegerRange.of(2, 7), IntegerRange.of(2, 7));
		assertCounter(0, 1, 18, 12, 0, 0, 0);
		// Same logic as above, 4 * 3 = 12 (3 rows: 5, 6, 7)

		// Add in the middle of the range, in viewport (> 22 & inside viewport)
		robot.interact(() -> grid.getItems().addAll(53, List.of(-12, -13, -14)));
		assertState(grid, IntegerRange.of(2, 7), IntegerRange.of(2, 7));
		assertCounter(0, 1, 17, 9, 0, 0, 0);
		// Same logic as above, 3 * 3 = 9 (3 rows: 5, 6, 7)

		// Add after intersect (around 77)
		robot.interact(() -> grid.getItems().addAll(75, List.of(-15, -16, -17, -18, -19, -20)));
		assertState(grid, IntegerRange.of(2, 7), IntegerRange.of(2, 7));
		assertCounter(0, 1, 0, 3, 0, 0, 0);
		// 3 updates because insertion is at 75, so cells 75, 76, 77 are updated

		// Add after no intersect (> 77)
		robot.interact(() -> grid.getItems().addAll(78, List.of(-21, -22, -23)));
		assertState(grid, IntegerRange.of(2, 7), IntegerRange.of(2, 7));
		assertCounter(0, 1, 0, 0, 0, 0, 0);
	}

	@Test
	void testAddAtEnd(FxRobot robot) {
		StackPane pane = setupStage();
		Grid grid = new Grid(items(100));
		robot.interact(() -> {
			grid.setColumnsNum(10);
			pane.getChildren().add(grid);
		});

		// Test init
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 5));
		assertCounter(36, 1, 36, 36, 0, 0, 0);

		// Scroll to end
		robot.interact(() -> {
			grid.scrollToLastColumn();
			grid.scrollToLastRow();
		});
		assertState(grid, IntegerRange.of(4, 9), IntegerRange.of(4, 9));
		assertCounter(0, 2, 48, 48, 0, 0, 0);

		// Add all at end (104)
		List<Integer> elements = IntStream.range(0, 4)
			.map(i -> 999 - i)
			.boxed()
			.toList();
		robot.interact(() -> grid.getItems().addAll(elements));
		assertState(grid, IntegerRange.of(5, 10), IntegerRange.of(4, 9));
		assertCounter(0, 1, 0, 0, 0, 6, 0);

		// Add all at end (for) (108)
		robot.interact(() -> {
			for (int i = 0; i < 4; i++) grid.getItems().add(1999 - i);
		});
		assertState(grid, IntegerRange.of(5, 10), IntegerRange.of(4, 9));
		assertCounter(0, 4, 4, 4, 4, 0, 0);
		assertEquals(2, grid.cacheSize());

		// Scroll again, add before no intersect (112)
		robot.interact(grid::scrollToLastRow);
		counter.reset();
		robot.interact(() -> grid.getItems().addAll(0, List.of(-1, -2, -3, -4)));
		assertState(grid, IntegerRange.of(6, 11), IntegerRange.of(4, 9));
		assertCounter(0, 1, 30, 20, 0, 4, 0);
		assertEquals(6, grid.cacheSize());
		// Last row contains only 2 cells, which are not in the columns range, so in the viewport we have 30 cells
		// 4 * 5 = 20 item updates (5 rows because the last one has no cells!)

		// Scroll again, add before intersect (117)
		robot.interact(grid::scrollToLastRow);
		counter.reset();
		robot.interact(() -> grid.getItems().addAll(62, List.of(-5, -6, -7, -8, -9)));
		assertState(grid, IntegerRange.of(6, 11), IntegerRange.of(4, 9));
		assertCounter(0, 1, 33, 23, 3, 0, 0);
		assertEquals(33, grid.getHelper().totalCells());
		// Last row only contains 3 cells, so in the viewport we have 33 cells
		// Items before:
		// [64,65,66,67,68,69]
		// [74,75,76,77,78,79]
		// [84,85,86,87,88,89]
		// [94,95,96,97,98,99]
		// [104,105,106,107,108,109]
		// [X,X,X,X,X,X]
		// Items after:
		// [-3,-4,-5,62,63,(64)]
		// [(69),70,71,72,73,(74)]
		// [(79),80,81,82,83,(84)]
		// [(89),90,91,92,93,(94)]
		// [(99),100,101,102,103,(104)]
		// [(109),110,111,X,X,X]
		// Items between () are in common, so...
		// Total cells: 33. 3 created. 10 in common. 33 - 23 = 23 total updates
	}

	@Test
	void testRemoveAt0(FxRobot robot) {
		StackPane pane = setupStage();
		Grid grid = new Grid(items(34));
		robot.interact(() -> pane.getChildren().add(grid));

		// Test init
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 4));
		assertCounter(30, 1, 30, 30, 0, 0, 0);

		// Remove all at 0
		robot.interact(() -> removeAll(grid, 0, 1, 2, 3));
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 4));
		assertCounter(0, 1, 30, 4, 0, 0, 0);

		// Remove once again to remove one buffer
		robot.interact(() -> removeAll(grid, 0, 1, 2, 3, 4));
		assertState(grid, IntegerRange.of(0, 4), IntegerRange.of(0, 4));
		assertCounter(0, 1, 25, 0, 0, 5, 0);

		// Remove at 0 (for)
		robot.interact(() -> {
			for (int i = 0; i < 5; i++) grid.getItems().removeFirst();
		});
		assertState(grid, IntegerRange.of(0, 3), IntegerRange.of(0, 4));
		assertCounter(0, 5, 110, 0, 0, 5, 0);
		assertEquals(10, grid.cacheSize());
		// Index updates are: 24 + 23 + 22 + 21 + 20
	}

	@Test
	void testRemoveMiddle(FxRobot robot) {
		StackPane pane = setupStage();
		Grid grid = new Grid(items(100));
		robot.interact(() -> {
			grid.setColumnsNum(10);
			pane.getChildren().add(grid);
		});

		// Test init
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 5));
		assertCounter(36, 1, 36, 36, 0, 0, 0);

		// Scroll to middle
		robot.interact(() -> {
			grid.setHPos(300);
			grid.setVPos(300);
		});
		assertState(grid, IntegerRange.of(2, 7), IntegerRange.of(2, 7));
		assertCounter(0, 2, 24, 24, 0, 0, 0);

		// Remove before no intersect (< 22)
		robot.interact(() -> removeAll(grid, 0, 1, 2));
		assertEquals(300, grid.getHPos());
		assertEquals(300, grid.getHPos());
		assertState(grid, IntegerRange.of(2, 7), IntegerRange.of(2, 7));
		assertCounter(0, 1, 36, 18, 0, 0, 0);
		// For every item removed, we have the same amount of updates for each row. So, 3 * 6 = 18

		// Remove before intersect (around 22)
		robot.interact(() -> removeAll(grid, 20, 21, 22, 23));
		assertEquals(300, grid.getHPos());
		assertEquals(300, grid.getHPos());
		assertState(grid, IntegerRange.of(2, 7), IntegerRange.of(2, 7));
		assertCounter(0, 1, 36, 24, 0, 0, 0);
		// Same logic as above, 4 * 6 = 24

		// Remove in the middle of the range (> 22)
		robot.interact(() -> removeAll(grid, 50, 51, 52, 53));
		assertEquals(300, grid.getHPos());
		assertEquals(300, grid.getHPos());
		assertState(grid, IntegerRange.of(2, 7), IntegerRange.of(2, 7));
		assertCounter(0, 1, 18, 12, 0, 0, 0);
		// Same logic as above, 4 * 3 = 12 (3 rows: 5, 6, 7)

		// Remove in the middle of the range, in viewport (> 22 & inside viewport)
		robot.interact(() -> removeAll(grid, 53, 54, 55));
		assertEquals(300, grid.getHPos());
		assertEquals(300, grid.getHPos());
		assertState(grid, IntegerRange.of(2, 7), IntegerRange.of(2, 7));
		assertCounter(0, 1, 17, 9, 0, 0, 0);
		// Same logic as above, 3 * 3 = 9 (3 rows: 5, 6, 7)

		// Remove after intersect (around 77)
		robot.interact(() -> removeAll(grid, 75, 76, 77, 78, 79, 80));
		assertEquals(300, grid.getHPos());
		assertEquals(300, grid.getHPos());
		assertState(grid, IntegerRange.of(2, 7), IntegerRange.of(2, 7));
		assertCounter(0, 1, 0, 3, 0, 0, 0);
		// 3 updates because removal is at 75, so cells 75, 76, 77 are updated

		// Remove after no intersect (> 77)
		robot.interact(() -> removeAll(grid, 78, 79));
		assertEquals(300, grid.getHPos());
		assertEquals(300, grid.getHPos());
		assertState(grid, IntegerRange.of(2, 7), IntegerRange.of(2, 7));
		assertCounter(0, 1, 0, 0, 0, 0, 0);

		// Remove enough to change the range
		robot.interact(() -> removeAll(grid, IntegerRange.of(70, 77)));
		assertEquals(300, grid.getHPos());
		assertEquals(300, grid.getVPos());
		assertState(grid, IntegerRange.of(1, 6), IntegerRange.of(2, 7));
		assertCounter(0, 1, 6, 6, 0, 0, 0);
	}

	@Test
	void testRemoveAtEnd(FxRobot robot) {
		StackPane pane = setupStage();
		Grid grid = new Grid(items(100));
		VFXGridHelper<Integer, VFXCell<Integer>> helper = grid.getHelper();
		robot.interact(() -> {
			grid.setColumnsNum(10);
			pane.getChildren().add(grid);
		});

		// Test init
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 5));
		assertCounter(36, 1, 36, 36, 0, 0, 0);

		// Scroll to end
		robot.interact(() -> {
			grid.scrollToLastColumn();
			grid.scrollToLastRow();
		});
		assertState(grid, IntegerRange.of(4, 9), IntegerRange.of(4, 9));
		assertCounter(0, 2, 48, 48, 0, 0, 0);

		// Remove all at end (96)
		robot.interact(() -> removeAll(grid, 96, 97, 98, 99));
		assertState(grid, IntegerRange.of(4, 9), IntegerRange.of(4, 9));
		assertCounter(0, 1, 0, 0, 0, 4, 0);
		assertEquals(32, helper.totalCells());

		// Remove all at end (for) (92)
		robot.interact(() -> {
			for (int i = 0; i < 4; i++) grid.getItems().removeLast();
		});
		assertState(grid, IntegerRange.of(4, 9), IntegerRange.of(4, 9));
		assertEquals(30, helper.totalCells());
		assertCounter(0, 4, 0, 0, 0, 2, 0);
		assertEquals(6, grid.cacheSize());
		// Only 2 cached instead of 4 because only 2 cells in the viewport are removed

		// Remove before no intersect (88)
		robot.interact(() -> removeAll(grid, 0, 1, 2, 3));
		assertState(grid, IntegerRange.of(3, 8), IntegerRange.of(4, 9));
		assertEquals(34, helper.totalCells());
		assertCounter(0, 1, 34, 24, 4, 0, 0);
		assertEquals(2, grid.cacheSize());
		// Last row contains only 4 cells, so in the viewport we have 34 cells
		// 4 * 6 = 24 item updates

		// Remove before intersect (82)
		robot.interact(() -> removeAll(grid, IntegerRange.of(32, 37)));
		assertState(grid, IntegerRange.of(3, 8), IntegerRange.of(4, 9));
		assertEquals(30, helper.totalCells());
		assertCounter(0, 1, 18, 20, 0, 4, 0);
		assertEquals(6, grid.cacheSize());
	}

	@Test
	void testRemoveSparse(FxRobot robot) {
		StackPane pane = setupStage();
		Grid grid = new Grid(items(50));
		robot.interact(() -> pane.getChildren().add(grid));

		// Test init
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 4));
		assertCounter(30, 1, 30, 30, 0, 0, 0);

		robot.interact(() -> removeAll(grid, 0, 3, 4, 8, 10, 11));
		assertState(grid, IntegerRange.of(0, 5), IntegerRange.of(0, 4));
		assertCounter(0, 1, 30, 6, 0, 0, 0);
	}

	@Test
	void testManualUpdate(FxRobot robot) {
		StackPane pane = setupStage();
		VFXGrid<User, VFXCell<User>> grid = new VFXGrid<>(users(50), u -> {
			TestCell<User> cell = new TestCell<>(u);
			counter.created();
			return cell;
		}) {
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
		};
		robot.interact(() -> pane.getChildren().add(grid));

		// Test init
		assertEquals(grid.getRowsRange(), IntegerRange.of(0, 7));
		assertEquals(grid.getColumnsRange(), IntegerRange.of(0, 4));
		assertCounter(40, 1, 40, 40, 0, 0, 0);

		// Get text before change cell 5
		VFXCell<User> cell5 = grid.getState().getCellsByIndexUnmodifiable().get(5);
		Label label5 = (Label) cell5.toNode().lookup(".label");
		String text5 = label5.getText();

		// Change item 5
		robot.interact(() -> grid.getItems().get(5).setFirstName(faker.name().firstName()));
		assertEquals(text5, label5.getText()); // No automatic update

		// Force update (all)
		robot.interact(() -> grid.update());
		assertNotEquals(text5, label5.getText());

		// Get text before change cell 7
		VFXCell<User> cell7 = grid.getState().getCellsByIndexUnmodifiable().get(7);
		Label label7 = (Label) cell7.toNode().lookup(".label");
		String text7 = label7.getText();

		// Change item 7
		robot.interact(() -> grid.getItems().get(7).setFirstName(faker.name().firstName()));
		assertEquals(text7, label7.getText()); // No automatic update

		// Force update (single)
		robot.interact(() -> grid.update(7));
		assertNotEquals(text7, label7.getText());

	}
}
