/*
 * Copyright (C) 2026 Parisi Alessandro - alessandro.parisi406@gmail.com
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

package interactive.table;

import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.utils.RandomUtils;
import io.github.palexdev.mfxcore.controls.Label;
import io.github.palexdev.mfxcore.utils.fx.CSSFragment;
import io.github.palexdev.mfxcore.utils.fx.ColorUtils;
import io.github.palexdev.mfxcore.utils.fx.StyleUtils;
import io.github.palexdev.mfxeffects.animations.Animations.KeyFrames;
import io.github.palexdev.mfxeffects.animations.Animations.SequentialBuilder;
import io.github.palexdev.mfxeffects.animations.Animations.TimelineBuilder;
import io.github.palexdev.mfxeffects.enums.Interpolators;
import io.github.palexdev.mfxresources.icon.MFXFontIcon;
import io.github.palexdev.virtualizedfx.cells.VFXObservingTableCell;
import io.github.palexdev.virtualizedfx.cells.base.VFXTableCell;
import io.github.palexdev.virtualizedfx.enums.BufferSize;
import io.github.palexdev.virtualizedfx.table.VFXTable;
import io.github.palexdev.virtualizedfx.table.VFXTableColumn;
import io.github.palexdev.virtualizedfx.table.VFXTableHelper;
import io.github.palexdev.virtualizedfx.table.VFXTableRow;
import io.github.palexdev.virtualizedfx.table.defaults.VFXSimpleTableColumn;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import src.model.FXUser;
import src.model.User;

import static interactive.table.TableTestUtils.*;
import static interactive.table.TableTestUtils.Table.emptyColumns;
import static io.github.palexdev.mfxcore.base.beans.Size.size;
import static io.github.palexdev.mfxcore.utils.fx.InsetsUtils.insets;
import static io.github.palexdev.mfxcore.utils.fx.InsetsUtils.uniform;
import static io.github.palexdev.virtualizedfx.utils.Utils.INVALID_RANGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static src.model.FXUser.fxusers;
import static src.model.User.faker;
import static src.model.User.users;
import static src.utils.TestFXUtils.FP_ASSERTIONS_DELTA;
import static src.utils.TestFXUtils.setupStage;
import static src.utils.Utils.*;

@ExtendWith(ApplicationExtension.class)
public class TableTests {

    /*
     * How do counters work?
     * In this note, I want to shed some light on the numbers you may see in these tests.
     * There is a difference between the number of updates occurring and the number of updates issued.
     * The first ones are the ones responsible for the cell's content to effectively change, e.g., a cell goes from item A to item B.
     * The latter ones are invoked by the container's subsystem and may or may not end up changing the cell's content,
     * e.g, cell goes from item C to item C -> even though the item is the same, the subsystem still issues the update,
     * but it won't have any effect on the cell because of no invalidation (this also depends on the cell's implementation!)
     * Why the subsystem issues "useless" updates then?
     * Because not always it's possible for the subsystem to know whether a cell needs to be updated or not, it just assumes.
     * This allows keeping the container's state stable, ensuring that each cell has the right properties set.
     * Is there a performance cost for this?
     * Yes and no. 1) It depends on the cell's implementation. If the cell is programmed to update only and only after
     * an invalidation of its properties then, no, there is no significant hit on performance. 2) We are just calling setters
     * after all, performance cost is negligible, even with a lot of cells
     *
     * So, the counters used by these tests will keep track of the "issued" updates to verify the correctness of the
     * subsystem's algorithms
     */

    // TODO incomplete: the handler set is complete, but two features are not.
    //  Blocked on autosize (VFXTable AUTOSIZE_ONCE, getWidthOf on the row): testAutosizeFixed/Variable/Empty/AllEmpty.
    //  testLastColumnResize covers the LAST fill policy, not the resize path.

    @Start
    void start(Stage stage) {
        stage.show();
    }

    @BeforeEach
    void setup() {
        resetCounters();
    }

    // TODO worth re-adding the comments from v1 too, even if wrong I'll correct them later with the right numbers or remove if unnecessary anymore
    @Test
    void testInitAndGeometry(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(50));
        robot.interact(() -> pane.getChildren().add(table));

        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);

        // Expand and test again
        robot.interact(() -> setWindowSize(pane, 600, -1));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(0, 1, 0, 0, 0, 0, 0, 0);

        robot.interact(() -> setWindowSize(pane, -1, 600));
        assertState(table, IntegerRange.of(0, 21), IntegerRange.of(0, 6));
        assertCounter(42, 1, 42, 42, 42, 0, 0, 0);
        assertRowsCounter(6, 6, 6, 0, 0, 0);

        // Shrink and test again
        robot.interact(() -> setWindowSize(pane, 300, -1));
        assertState(table, IntegerRange.of(0, 21), IntegerRange.of(0, 5));
        assertCounter(0, 1, 0, 0, 0, 0, 22, 12); // 22 for the same column

        robot.interact(() -> setWindowSize(pane, -1, 300));
        assertState(table, IntegerRange.of(0, 12), IntegerRange.of(0, 5));
        assertCounter(0, 1, 0, 0, 0, 0, 54, 0); // 54 for different columns. 9 for each. No disposals.
        assertRowsCounter(0, 0, 0, 0, 9, 0);

        robot.interact(() -> setWindowSize(pane, -1, 500));
        assertState(table, IntegerRange.of(0, 18), IntegerRange.of(0, 5));
        assertCounter(0, 1, 36, 36, 36, 36, 0, 0);
        assertRowsCounter(0, 6, 6, 6, 0, 0);

        // Edge case set width to 0
        robot.interact(() -> {
            table.setMinWidth(0);
            table.setPrefWidth(0);
            table.setMaxWidth(0);
            robot.interact(() -> setWindowSize(pane, 0, -1));
        });
        assertState(table, INVALID_RANGE, INVALID_RANGE);
        assertCounter(0, 0, 0, 0, 0, 0, 114, 72); // Each column already has 3 cells in cache, so 54 + 18 = 72
        assertRowsCounter(0, 0, 0, 0, 19, 12); // 12 disposed because 3 already in cache
    }

    @Test
    void testInitAndGeometryMaxX(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(50));
        robot.interact(() -> {
            pane.getChildren().add(table);
            table.scrollToLastColumn();
        });

        // Check hPos!!
        assertEquals(860.0, table.getHPos());

        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);

        // Expand (won't have any effect since columns are already all shown)
        robot.interact(() -> setWindowSize(pane, 600, -1));
        assertEquals(660.0, table.getHPos());
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(0, 1, 0, 0, 0, 0, 0, 0);

        // Shrink
        robot.interact(() -> {
            robot.interact(() -> setWindowSize(pane, 100, -1));
            // Why you be like that JavaFX :smh:
            table.setMinWidth(100.0);
            table.setPrefWidth(100.0);
            table.setMaxWidth(100.0);
        });
        assertEquals(660.0, table.getHPos());
        // Column 3 is [540, 720) and column 4 is [720, 900): the viewport [660, 760] straddles both
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(1, 6));
        assertCounter(0, 1, 0, 0, 0, 0, 16, 6);
    }

    @Test
    void testInitAndGeometryMaxY(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(50));
        robot.interact(() -> {
            pane.getChildren().add(table);
            table.scrollToLastRow();
        });

        // Check vPos!!
        assertEquals(1232.0, table.getVPos());

        assertState(table, IntegerRange.of(34, 49), IntegerRange.of(0, 6));
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);

        // Expand
        robot.interact(() -> setWindowSize(pane, -1, 600));
        assertEquals(1032.0, table.getVPos());
        assertState(table, IntegerRange.of(28, 49), IntegerRange.of(0, 6));
        assertCounter(42, 1, 42, 42, 42, 0, 0, 0);
        assertRowsCounter(6, 6, 6, 0, 0, 0);

        // Shrink
        robot.interact(() -> setWindowSize(pane, -1, 300));
        assertEquals(1032.0, table.getVPos());
        assertState(table, IntegerRange.of(30, 42), IntegerRange.of(0, 6));
        assertCounter(0, 1, 0, 0, 0, 0, 63, 0);
        assertRowsCounter(0, 0, 0, 0, 9, 0);
    }

    @Test
    void testPopulateCache(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(50));
        table.populateRowsCache();
        resetCounters();
        robot.interact(() -> pane.getChildren().add(table));

        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(6, 16, 16, 10, 0, 0);
    }

    @Test
    void testPopulateCacheAll(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(50));
        table.populateRowsCache()
            .populateCellsCache();
        resetCounters();

        for (VFXTableRow<User> row : table.getRowsCache().cells()) {
            assertNotNull(row.getTable());
        }

        for (VFXTableColumn<User, ? extends VFXTableCell<User>> column : table.columns()) {
            for (UserCell<?> cell : ((TestColumn<?>) column).getCellsCache().cells()) {
                assertNotNull(cell.getTable());
            }
        }

        robot.interact(() -> pane.getChildren().add(table));

        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(42, 1, 112, 112, 112, 70, 0, 0);
        assertRowsCounter(6, 16, 16, 10, 0, 0);
    }

    @Test
    void testScrollVertical(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(50));
        robot.interact(() -> pane.getChildren().add(table));

        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);

        robot.interact(() -> table.setVPos(400.0));
        assertState(table, IntegerRange.of(10, 25), IntegerRange.of(0, 6));
        assertCounter(0, 1, 0, 0, 70, 0, 0, 0);
        assertRowsCounter(0, 10, 10, 0, 0, 0);

        robot.interact(table::scrollToLastRow);
        assertState(table, IntegerRange.of(34, 49), IntegerRange.of(0, 6));
        assertCounter(0, 1, 0, 0, 112, 0, 0, 0);
        assertRowsCounter(0, 16, 16, 0, 0, 0);

        robot.interact(() -> {
            table.setItems(users(35));
            table.scrollToFirstRow(); // Here range becomes [0, 15]
            resetCounters();
            table.setVPos(300.0);
        });
        assertState(table, IntegerRange.of(7, 22), IntegerRange.of(0, 6));
        assertCounter(0, 1, 0, 0, 49, 0, 0, 0);
        assertRowsCounter(0, 7, 7, 0, 0, 0);
    }

    @Test
    void testScrollHorizontal(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(50))
            .addEmptyColumns(10);
        robot.interact(() -> pane.getChildren().add(table));

        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);

        robot.interact(() -> table.setHPos(800.0));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(2, 8));
        assertCounter(32, 1, 32, 32, 32, 0, 32, 12);

        robot.interact(table::scrollToLastColumn);
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(10, 16));
        assertCounter(112, 1, 112, 112, 112, 0, 112, 42);

        // The following test is complex but interesting because we are also removing columns from the table
        // For this reason I'm going to test every step
        robot.interact(() -> table.columns().remove(10, 16));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(4, 10));
        assertCounter(46, 1, 112, 112, 96, 50, 96, 36);

        robot.interact(table::scrollToFirstColumn);
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(24, 1, 64, 64, 64, 40, 64, 24);

        robot.interact(() -> table.setHPos(600.0));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(1, 7));
        assertCounter(6, 1, 16, 16, 16, 10, 16, 6);
    }

    @Test
    void testScrollHorizontalNoItems(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(0))
            .addEmptyColumns(10);
        robot.interact(() -> pane.getChildren().add(table));

        assertState(table, INVALID_RANGE, IntegerRange.of(0, 6), 0);
        assertCounter(0, 0, 0, 0, 0, 0, 0, 0);
        assertRowsCounter(0, 0, 0, 0, 0, 0);

        Animation a1 = TimelineBuilder.build()
            .add(KeyFrames.of(500, table.hPosProperty(), 800, Interpolator.LINEAR))
            .getAnimation();
        robot.interact(a1::play);
        sleep(550);
        assertState(table, INVALID_RANGE, IntegerRange.of(2, 8), 0);
        assertCounter(0, 0, 0, 0, 0, 0, 0, 0);

        Animation a2 = TimelineBuilder.build()
            .add(KeyFrames.of(500, table.hPosProperty(), table.getMaxHScroll(), Interpolators.LINEAR))
            .getAnimation();
        robot.interact(a2::play);
        sleep(550);
        assertState(table, INVALID_RANGE, IntegerRange.of(10, 16), 0);
        assertCounter(0, 0, 0, 0, 0, 0, 0, 0);

        robot.interact(() -> table.columns().remove(10, 16));
        assertState(table, INVALID_RANGE, IntegerRange.of(4, 10), 0);
        assertCounter(0, 0, 0, 0, 0, 0, 0, 0);

        robot.interact(table::scrollToFirstColumn);
        assertState(table, INVALID_RANGE, IntegerRange.of(0, 6), 0);
        assertCounter(0, 0, 0, 0, 0, 0, 0, 0);

        robot.interact(() -> table.setHPos(600.0));
        assertState(table, INVALID_RANGE, IntegerRange.of(1, 7), 0);
        assertCounter(0, 0, 0, 0, 0, 0, 0, 0);
    }

    /*
     * NOTE: setBufferSize is overridden in Table to change both buffers
     * which means that each call is two states -> two layouts
     */

    @Test
    void testBufferChangeTopLeft(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(50))
            .addEmptyColumns(9);
        VFXTableHelper<User> helper = table.getHelper();
        robot.interact(() -> pane.getChildren().add(table));

        // Medium buffer (default)
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertEquals(36, helper.visibleCells());
        assertEquals(112, helper.totalCells());
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);

        // Small buffer
        robot.interact(() -> table.setBufferSize(BufferSize.SMALL));
        assertState(table, IntegerRange.of(0, 13), IntegerRange.of(0, 4));
        assertEquals(36, helper.visibleCells());
        assertEquals(70, helper.totalCells());
        assertCounter(0, 2, 0, 0, 0, 0, 42, 12);
        assertRowsCounter(0, 0, 0, 0, 2, 0);

        // Big buffer
        robot.interact(() -> table.setBufferSize(BufferSize.BIG));
        assertState(table, IntegerRange.of(0, 17), IntegerRange.of(0, 8));
        assertEquals(36, helper.visibleCells());
        assertEquals(162, helper.totalCells());
        assertCounter(62, 2, 92, 92, 92, 30, 0, 0);
        assertRowsCounter(2, 4, 4, 2, 0, 0);
    }

    @Test
    void testBufferChangeMiddle(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(50))
            .addEmptyColumns(9);
        VFXTableHelper<User> helper = table.getHelper();
        robot.interact(() -> {
            table.setVPos(616.0);
            table.setHPos(1240.0);
            pane.getChildren().add(table);
        });

        // Medium buffer (default)
        assertState(table, IntegerRange.of(17, 32), IntegerRange.of(4, 11));
        assertEquals(48, helper.visibleCells());
        assertEquals(128, helper.totalCells());
        assertCounter(128, 1, 128, 128, 128, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);

        // Small buffer
        robot.interact(() -> table.setBufferSize(BufferSize.SMALL));
        assertState(table, IntegerRange.of(18, 31), IntegerRange.of(5, 10));
        assertEquals(48, helper.visibleCells());
        assertEquals(84, helper.totalCells());
        assertCounter(0, 2, 0, 0, 0, 0, 44, 12);
        assertRowsCounter(0, 0, 0, 0, 2, 0);

        // Big buffer
        // rows [18,31] -> [16,33] | columns [5,10] -> [3,12]
        // pass 1 (rows):    difference([5,10], [5,10]) = INVALID -> Y_ONLY
        //                   4 rows x 6 cols = 24 new cells, 24 cellLayouts
        // pass 2 (columns): difference([3,12], [5,10]) = [3,12]   (left && right)
        //                   markDirty(3) (4) (11) (12) -> [3,12]
        //                   18 rows x 4 cols = 72 new cells, 18 x 10 = 180 cellLayouts
        // cellLayouts 24 + 180 = 204 | new cells 24 + 72 = 96 = created 64 + deCached 32
        robot.interact(() -> table.setBufferSize(BufferSize.BIG));
        assertState(table, IntegerRange.of(16, 33), IntegerRange.of(3, 12));
        assertEquals(48, helper.visibleCells());
        assertEquals(180, helper.totalCells());
        assertCounter(64, 2, 204, 96, 96, 32, 0, 0);
        assertRowsCounter(2, 4, 4, 2, 0, 0);
    }

    @Test
    void testBufferChangeBottomRight(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(50))
            .addEmptyColumns(9);
        VFXTableHelper<User> helper = table.getHelper();
        robot.interact(() -> {
            table.scrollToLastRow();
            table.scrollToLastColumn();
            pane.getChildren().add(table);
        });

        // Medium buffer
        assertState(table, IntegerRange.of(34, 49), IntegerRange.of(9, 15));
        assertEquals(36, helper.visibleCells());
        assertEquals(112, helper.totalCells());
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);

        // Small buffer
        robot.interact(() -> table.setBufferSize(BufferSize.SMALL));
        assertState(table, IntegerRange.of(36, 49), IntegerRange.of(11, 15));
        assertEquals(36, helper.visibleCells());
        assertEquals(70, helper.totalCells());
        assertCounter(0, 2, 0, 0, 0, 0, 42, 12);
        assertRowsCounter(0, 0, 0, 0, 2, 0);

        // Big buffer
        robot.interact(() -> table.setBufferSize(BufferSize.BIG));
        assertState(table, IntegerRange.of(32, 49), IntegerRange.of(7, 15));
        assertEquals(36, helper.visibleCells());
        assertEquals(162, helper.totalCells());
        assertCounter(62, 2, 92, 92, 92, 30, 0, 0);
        assertRowsCounter(2, 4, 4, 2, 0, 0);
    }

    @Test
    void testColumnsPermutation(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(20))
            .addEmptyColumns(9);
        robot.interact(() -> pane.getChildren().add(table));

        // Assert init
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);

        // Permutation change
        robot.interact(() -> FXCollections.sort(table.columns(), Collections.reverseOrder()));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(112, 1, 112, 112, 112, 0, 112, 42);
    }

    @Test
    void testColumnsPermutationMiddle(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(20))
            .addEmptyColumns(9);
        robot.interact(() -> {
            table.setHPos(1000.0);
            pane.getChildren().add(table);
        });

        // Assert init
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(3, 9));
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);

        // Permutation change
        robot.interact(() -> FXCollections.sort(table.columns(), Collections.reverseOrder()));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(3, 9));
        assertCounter(48, 1, 112, 112, 48, 0, 48, 18);
    }

    @Test
    void testSetColumns(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(20))
            .addEmptyColumns(9);
        robot.interact(() -> pane.getChildren().add(table));

        // Assert init
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);

        // Set all (more)
        robot.interact(() -> table.columns().setAll(emptyColumns("Set", 15)));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(112, 1, 112, 112, 112, 0, 112, 42);

        // Set all (less)
        robot.interact(() -> table.columns().setAll(emptyColumns("Set", 5)));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 4));
        assertCounter(80, 1, 80, 80, 80, 0, 112, 42);

        // Restore old count
        // Go to last and set all (more)
        robot.interact(() -> {
            table.columns().setAll(emptyColumns(16));
            table.scrollToLastColumn();
        });
        resetCounters();
        robot.interact(() -> table.columns().setAll(emptyColumns("Set", 30)));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(11, 18));
        assertCounter(128, 1, 128, 128, 128, 0, 112, 42);

        // Set columns inside the range
        robot.interact(() -> {
            IntegerRange columnsRange = table.getState().getColumnsRange();
            for (int i = 0; i < 5; i++) {
                table.columns().set(columnsRange.getMin() + i, new EmptyColumn("Set at", 999 - i));
            }
        });
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(11, 18));
        assertCounter(80, 5, 480, 640, 80, 0, 80, 30);
        // Every time a change occurs, all the cells have their index updated.
        // This is because their parent column may be in a different position after the change.
        // This should be improved on the cell implementation side with a basic check.
        // Also remember... cells here CANNOT be reused as every column produces its own kind
        // Layout:
        // There are a total of 128 cells in the viewport. With partial layouts the count is as follows:
        // 128 + 112 + 96 + 80 + 64 = 480
    }

    @Test
    void testAddColumnsAt0(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(20))
            .addEmptyColumns(9);
        robot.interact(() -> pane.getChildren().add(table));

        // Assert init
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);

        // Add all at 0
        robot.interact(() -> table.columns().addAll(0, emptyColumns("Add", 4)));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(64, 1, 112, 112, 64, 0, 64, 24);

        // Add at 0 (for)
        robot.interact(() -> {
            for (int i = 0; i < 4; i++) table.columns().addFirst(new EmptyColumn("Add for", 999 - i));
        });
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(64, 4, 448, 448, 64, 0, 64, 24);
    }

    @Test
    void testAddColumnsAtMiddle(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(20))
            .addEmptyColumns(9);
        robot.interact(() -> {
            table.setHPos(900.0);
            pane.getChildren().add(table);
        });

        // Assert init
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(3, 9));
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);

        // Add before no intersect
        robot.interact(() -> table.columns().addAll(0, emptyColumns("Add bni", 2)));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(3, 9));
        assertCounter(32, 1, 112, 112, 32, 0, 32, 12);

        // Add before intersect
        robot.interact(() -> table.columns().addAll(2, emptyColumns("Add bi", 3)));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(3, 9));
        assertCounter(48, 1, 112, 112, 48, 0, 48, 18);

        // Add after intersect
        robot.interact(() -> table.columns().addAll(9, emptyColumns("Add ai", 2)));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(3, 9));
        assertCounter(16, 1, 16, 112, 16, 0, 16, 6);

        // Add after no intersect
        robot.interact(() -> table.columns().addAll(10, emptyColumns("Add ani", 2)));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(3, 9));
        assertCounter(0, 1, 0, 112, 0, 0, 0, 0);
    }

    @Test
    void testAddColumnsAtEnd(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(20))
            .addEmptyColumns(9);
        robot.interact(() -> {
            table.scrollToLastColumn();
            pane.getChildren().add(table);
        });

        // Assert init
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(9, 15));
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);

        // Add all at end
        robot.interact(() -> table.columns().addAll(emptyColumns("End", 2)));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(10, 17));
        assertCounter(32, 1, 32, 128, 32, 0, 16, 6);

        // Add all at end (for)
        robot.interact(() -> {
            for (int i = 0; i < 2; i++) table.columns().add(new EmptyColumn("EndFor", 999 - i));
        });
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(11, 18));
        assertCounter(16, 2, 16, 256, 16, 0, 16, 6);

        // Add before no intersect
        robot.interact(() -> table.columns().addAll(0, emptyColumns("Add bni", 2)));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(11, 18));
        assertCounter(12, 1, 128, 128, 32, 20, 32, 12);

        // Add before intersect
        robot.interact(() -> table.columns().addAll(10, emptyColumns("Add bi", 3)));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(11, 18));
        assertCounter(48, 1, 128, 128, 48, 0, 48, 18);
    }

    @Test
    void testRemoveColumnsAt0(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(20))
            .addEmptyColumns(9);
        robot.interact(() -> pane.getChildren().add(table));

        // Assert init
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);

        // Remove all at 0
        robot.interact(() -> removeAll(table.columns(), 0, 1, 2));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(48, 1, 112, 112, 48, 0, 48, 18);

        // Remove at 0 (for)
        robot.interact(() -> {
            for (int i = 0; i < 4; i++) table.columns().removeFirst();
        });
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(64, 4, 448, 448, 64, 0, 64, 24);

        // Clear and assert INVALID state
        robot.interact(() -> table.columns().clear());
        assertState(table, INVALID_RANGE, INVALID_RANGE);
        assertCounter(0, 0, 0, 0, 0, 0, 112, 42);
        assertEquals(0.0, table.getHPos());
    }

    @Test
    void testRemoveColumnsAtMiddle(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(20))
            .addEmptyColumns(15);
        robot.interact(() -> {
            table.setHPos(1600.0);
            pane.getChildren().add(table);
        });

        // Assert init
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(6, 13));
        assertCounter(128, 1, 128, 128, 128, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);

        // Remove before no intersect
        robot.interact(() -> removeAll(table.columns(), 0, 1));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(6, 13));
        assertCounter(32, 1, 128, 128, 32, 0, 32, 12);

        // Remove before intersect
        robot.interact(() -> removeAll(table.columns(), 5, 6));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(6, 13));
        assertCounter(32, 1, 128, 128, 32, 0, 32, 12);

        // Remove after intersect
        robot.interact(() -> removeAll(table.columns(), 12, 13));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(6, 13));
        assertCounter(32, 1, 32, 128, 32, 0, 32, 12);

        // Remove after no intersect
        robot.interact(() -> removeAll(table.columns(), 13, 14));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(6, 13));
        assertCounter(16, 1, 16, 128, 16, 0, 16, 6);

        // Clear and assert INVALID state
        robot.interact(() -> table.columns().clear());
        assertState(table, INVALID_RANGE, INVALID_RANGE);
        assertCounter(0, 0, 0, 0, 0, 0, 128, 48);
        assertEquals(0.0, table.getHPos());
    }

    @Test
    void testRemoveColumnsAtEnd(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(20))
            .addEmptyColumns(15);
        robot.interact(() -> {
            table.scrollToLastColumn();
            pane.getChildren().add(table);
        });

        // Assert init
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(15, 21));
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);

        // Remove all at end
        robot.interact(() -> removeAll(table.columns(), 19, 20, 21));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(12, 18));
        assertCounter(48, 1, 112, 112, 48, 0, 48, 18);

        // Remove at end (for)
        robot.interact(() -> {
            for (int i = 0; i < 2; i++) table.columns().removeLast();
        });
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(10, 16));
        assertCounter(32, 2, 224, 224, 32, 0, 32, 12);

        // Remove before no intersect
        robot.interact(() -> removeAll(table.columns(), 0, 1));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(8, 14));
        assertCounter(0, 1, 112, 112, 0, 0, 0, 0);

        // Remove before intersect
        robot.interact(() -> removeAll(table.columns(), 6, 7, 8));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(5, 11));
        assertCounter(16, 1, 112, 112, 16, 0, 16, 6);

        // Clear and assert INVALID state
        robot.interact(() -> table.columns().clear());
        assertState(table, INVALID_RANGE, INVALID_RANGE);
        assertCounter(0, 0, 0, 0, 0, 0, 112, 42);
        assertEquals(0.0, table.getHPos());
    }

    @Test
    void testRemoveAndAddColumn(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(20));
        robot.interact(() -> pane.getChildren().add(table));

        // Assert init
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);

        // Remove
        var removed = new AtomicReference<VFXTableColumn<User, ? extends VFXTableCell<User>>>();
        robot.interact(() -> removed.set(table.columns().removeFirst()));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 5));
        assertCounter(0, 1, 96, 96, 0, 0, 16, 6);

        // Add it back, now as the last column
        robot.interact(() -> table.columns().add(removed.get()));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(6, 1, 16, 112, 16, 10, 0, 0);
    }

    @Test
    void testChangeItemsList(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(50));
        robot.interact(() -> pane.getChildren().add(table));

        // Assert init
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);

        // Change items property
        robot.interact(() -> table.setItems(users(50)));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(0, 1, 0, 0, 112, 0, 0, 0);
        assertRowsCounter(0, 16, 16, 0, 0, 0);

        // Change items property (fewer elements)
        robot.interact(() -> table.setItems(users(10)));
        assertState(table, IntegerRange.of(0, 9), IntegerRange.of(0, 6));
        assertCounter(0, 1, 0, 0, 70, 0, 42, 0);
        assertRowsCounter(0, 10, 10, 0, 6, 0);

        // Change items property (more elements)
        robot.interact(() -> table.setItems(users(50)));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(0, 1, 42, 42, 112, 42, 0, 0);
        assertRowsCounter(0, 16, 16, 6, 0, 0);
        // Unfortunately, we have to update all the rows and cells since the new list contains only new items
        // Still we are not creating 6 rows for a total of 42 reused cells, great

        // Scroll(to bottom) and change items property (fewer elements)
        robot.interact(() -> {
            table.scrollToLastRow();
            resetCounters();
            table.setItems(users(15));
        });
        assertState(table, IntegerRange.of(0, 14), IntegerRange.of(0, 6));
        assertCounter(0, 1, 0, 0, 105, 0, 7, 0);
        assertRowsCounter(0, 15, 15, 0, 1, 0);

        // Fill the viewport, then scroll to max again and set more
        robot.interact(() -> {
            table.setItems(users(25));
            table.scrollToLastRow();
            resetCounters();
            table.setItems(users(40));
        });
        assertState(table, IntegerRange.of(11, 26), IntegerRange.of(0, 6));
        assertCounter(0, 1, 0, 0, 112, 0, 0, 0);
        assertRowsCounter(0, 16, 16, 0, 0, 0);

        // Change items to empty
        robot.interact(() -> table.setItems(null));
        assertState(table, INVALID_RANGE, IntegerRange.of(0, 6), 0);
        assertCounter(0, 0, 0, 0, 0, 0, 112, 42);
        assertRowsCounter(0, 0, 0, 0, 16, 6);
    }

    @Test
    void testItemsPermutation(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(50));
        robot.interact(() -> pane.getChildren().add(table));

        // Assert init
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);

        // Permutation change
        robot.interact(() -> FXCollections.sort(table.getItems(), Comparator.comparing(User::id).reversed()));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(0, 1, 0, 0, 112, 0, 0, 0);
        assertRowsCounter(0, 16, 16, 0, 0, 0);
    }

    @Test
    void testSetItems(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(50));
        robot.interact(() -> pane.getChildren().add(table));

        // Assert init
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);

        // Set all (more)
        robot.interact(() -> table.getItems().setAll(users(80)));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(0, 1, 0, 0, 112, 0, 0, 0);
        assertRowsCounter(0, 16, 16, 0, 0, 0);

        // Set all (less)
        robot.interact(() -> table.getItems().setAll(users(10)));
        assertState(table, IntegerRange.of(0, 9), IntegerRange.of(0, 6));
        assertCounter(0, 1, 0, 0, 70, 0, 42, 0);
        assertRowsCounter(0, 10, 10, 0, 6, 0);

        // Restore old items count, scroll and set all (more)
        // Also check re-usability (by identity!!!)
        ObservableList<User> tmp = users(50);
        robot.interact(() -> {
            table.getItems().setAll(tmp.subList(0, 40));
            table.scrollToLastRow(); // Range is [24, 39]
            resetCounters();
            table.setItems(tmp); // Range is [26, 41]
        });
        assertState(table, IntegerRange.of(26, 41), IntegerRange.of(0, 6));
        assertCounter(0, 1, 0, 0, 14, 0, 0, 0);
        assertRowsCounter(0, 16, 2, 0, 0, 0);

        // Set random items
        robot.interact(() -> {
            for (int i = 0; i < 5; i++) {
                int index = RandomUtils.random.nextInt(26, 42);
                table.getItems().set(index, new User());
            }
        });
        assertState(table, IntegerRange.of(26, 41), IntegerRange.of(0, 6));
        assertCounter(0, 5, 0, 0, 35, 0, 0, 0);
        assertRowsCounter(0, 80, 5, 0, 0, 0);
    }

    @Test
    void testAddItemsAt0(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(50));
        robot.interact(() -> pane.getChildren().add(table));

        // Assert init
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);

        // Add all at 0
        robot.interact(() -> table.getItems().addAll(0, users(4)));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(0, 1, 0, 0, 28, 0, 0, 0);
        assertRowsCounter(0, 16, 4, 0, 0, 0);

        // Add at 0 (for)
        robot.interact(() -> {
            for (int i = 0; i < 3; i++) table.getItems().addFirst(new User());
        });
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(0, 3, 0, 0, 21, 0, 0, 0);
        assertRowsCounter(0, 48, 3, 0, 0, 0);
    }

    @Test
    void testAddItemsAtMiddle(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(50));
        robot.interact(() -> {
            table.setVPos(600.0);
            pane.getChildren().add(table);
        });

        // Assert init
        assertState(table, IntegerRange.of(16, 31), IntegerRange.of(0, 6));
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);

        // Add before no intersect
        robot.interact(() -> table.getItems().addAll(0, users(3)));
        assertState(table, IntegerRange.of(16, 31), IntegerRange.of(0, 6));
        assertCounter(0, 1, 0, 0, 21, 0, 0, 0);
        assertRowsCounter(0, 16, 3, 0, 0, 0);

        // Add before intersect
        robot.interact(() -> table.getItems().addAll(14, users(4)));
        assertState(table, IntegerRange.of(16, 31), IntegerRange.of(0, 6));
        assertCounter(0, 1, 0, 0, 28, 0, 0, 0);
        assertRowsCounter(0, 16, 4, 0, 0, 0);

        // Add after intersect
        robot.interact(() -> table.getItems().addAll(29, users(4)));
        assertState(table, IntegerRange.of(16, 31), IntegerRange.of(0, 6));
        assertCounter(0, 1, 0, 0, 21, 0, 0, 0);
        assertRowsCounter(0, 16, 3, 0, 0, 0);

        // Add after no intersect
        robot.interact(() -> table.getItems().addAll(users(2)));
        assertState(table, IntegerRange.of(16, 31), IntegerRange.of(0, 6));
        assertCounter(0, 1, 0, 0, 0, 0, 0, 0);
        assertRowsCounter(0, 16, 0, 0, 0, 0);
    }

    @Test
    void testAddItemsAtEnd(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(50));
        robot.interact(() -> {
            table.scrollToLastRow();
            pane.getChildren().add(table);
        });

        // Assert init
        assertState(table, IntegerRange.of(34, 49), IntegerRange.of(0, 6));
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);

        // Add only one at end
        robot.interact(() -> table.getItems().add(new User()));
        assertState(table, IntegerRange.of(35, 50), IntegerRange.of(0, 6));
        assertCounter(0, 1, 0, 0, 7, 0, 0, 0);
        assertRowsCounter(0, 16, 1, 0, 0, 0);

        // Add all at end (for)
        robot.interact(() -> {
            for (int i = 0; i < 3; i++) table.getItems().add(new User());
        });
        assertState(table, IntegerRange.of(36, 51), IntegerRange.of(0, 6));
        assertCounter(0, 3, 0, 0, 7, 0, 0, 0);
        assertRowsCounter(0, 48, 1, 0, 0, 0);

        // Add before no intersect
        robot.interact(() -> table.getItems().addAll(0, users(2)));
        assertState(table, IntegerRange.of(36, 51), IntegerRange.of(0, 6));
        assertCounter(0, 1, 0, 0, 14, 0, 0, 0);
        assertRowsCounter(0, 16, 2, 0, 0, 0);

        // Add before intersect
        robot.interact(() -> table.getItems().addAll(34, users(4)));
        assertState(table, IntegerRange.of(36, 51), IntegerRange.of(0, 6));
        assertCounter(0, 1, 0, 0, 28, 0, 0, 0);
        assertRowsCounter(0, 16, 4, 0, 0, 0);
    }

    @Test
    void testRemoveItemsAt0(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(50));
        robot.interact(() -> pane.getChildren().add(table));

        // Assert init
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);

        // Remove all at 0
        robot.interact(() -> removeAll(table, 0, 1, 2, 3));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(0, 1, 0, 0, 28, 0, 0, 0);
        assertRowsCounter(0, 16, 4, 0, 0, 0);

        // Remove at 0 (for)
        robot.interact(() -> {
            for (int i = 0; i < 4; i++) table.getItems().removeFirst();
        });
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(0, 4, 0, 0, 28, 0, 0, 0);
        assertRowsCounter(0, 64, 4, 0, 0, 0);

        // Remove until cannot fill viewport
        robot.interact(() -> removeAll(table, IntegerRange.of(0, 31)));
        assertEquals(10, table.size());
        assertState(table, IntegerRange.of(0, 9), IntegerRange.of(0, 6));
        assertCounter(0, 1, 0, 0, 70, 0, 42, 0);
        assertRowsCounter(0, 10, 10, 0, 6, 0);
    }

    @Test
    void testRemoveItemsAtMiddle(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(50));
        robot.interact(() -> {
            table.setVPos(600.0);
            pane.getChildren().add(table);
        });

        // Assert init
        assertState(table, IntegerRange.of(16, 31), IntegerRange.of(0, 6));
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);

        // Remove before no intersect
        robot.interact(() -> removeAll(table, 0, 1));
        assertState(table, IntegerRange.of(16, 31), IntegerRange.of(0, 6));
        assertCounter(0, 1, 0, 0, 14, 0, 0, 0);
        assertRowsCounter(0, 16, 2, 0, 0, 0);

        // Remove before intersect
        robot.interact(() -> removeAll(table, 14, 15, 16, 17));
        assertState(table, IntegerRange.of(16, 31), IntegerRange.of(0, 6));
        assertCounter(0, 1, 0, 0, 28, 0, 0, 0);
        assertRowsCounter(0, 16, 4, 0, 0, 0);

        // Remove after intersect
        robot.interact(() -> removeAll(table, 31, 32, 33));
        assertState(table, IntegerRange.of(16, 31), IntegerRange.of(0, 6));
        assertCounter(0, 1, 0, 0, 7, 0, 0, 0);
        assertRowsCounter(0, 16, 1, 0, 0, 0);

        // Remove after no intersect
        robot.interact(() -> removeAll(table, 32, 33));
        assertState(table, IntegerRange.of(16, 31), IntegerRange.of(0, 6));
        assertCounter(0, 1, 0, 0, 0, 0, 0, 0);
        assertRowsCounter(0, 16, 0, 0, 0, 0);

        // Remove enough to change vPos and range
        robot.interact(() -> removeAll(table, IntegerRange.of(0, 18)));
        assertEquals(20, table.size());
        assertEquals(272.0, table.getVPos());
        assertState(table, IntegerRange.of(4, 19), IntegerRange.of(0, 6));
        assertCounter(0, 1, 0, 0, 49, 0, 0, 0);
        assertRowsCounter(0, 16, 7, 0, 0, 0);

        // Do it again but this time from bottom
        robot.interact(() -> removeAll(table, 18, 19));
        assertEquals(18, table.size());
        assertEquals(208.0, table.getVPos());
        assertState(table, IntegerRange.of(2, 17), IntegerRange.of(0, 6));
        assertCounter(0, 1, 0, 0, 14, 0, 0, 0);
        assertRowsCounter(0, 16, 2, 0, 0, 0);
    }

    @Test
    void testRemoveItemsAtEnd(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(50));
        robot.interact(() -> {
            table.scrollToLastRow();
            pane.getChildren().add(table);
        });

        // Assert init
        assertState(table, IntegerRange.of(34, 49), IntegerRange.of(0, 6));
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);

        // Remove all at end
        robot.interact(() -> removeAll(table, 46, 47, 48, 49));
        assertEquals(1104.0, table.getVPos());
        assertState(table, IntegerRange.of(30, 45), IntegerRange.of(0, 6));
        assertCounter(0, 1, 0, 0, 28, 0, 0, 0);
        assertRowsCounter(0, 16, 4, 0, 0, 0);

        // Remove at end (for)
        robot.interact(() -> {
            for (int i = 0; i < 4; i++) table.getItems().removeLast();
        });
        assertEquals(976.0, table.getVPos());
        assertState(table, IntegerRange.of(26, 41), IntegerRange.of(0, 6));
        assertCounter(0, 4, 0, 0, 28, 0, 0, 0);
        assertRowsCounter(0, 64, 4, 0, 0, 0);

        // Remove before no intersect
        robot.interact(() -> removeAll(table, 0, 1));
        assertEquals(912.0, table.getVPos());
        assertState(table, IntegerRange.of(24, 39), IntegerRange.of(0, 6));
        assertCounter(0, 1, 0, 0, 0, 0, 0, 0);
        assertRowsCounter(0, 16, 0, 0, 0, 0);

        // Remove before intersect
        robot.interact(() -> removeAll(table, 22, 23, 24, 25));
        assertEquals(784.0, table.getVPos());
        assertState(table, IntegerRange.of(20, 35), IntegerRange.of(0, 6));
        assertCounter(0, 1, 0, 0, 14, 0, 0, 0);
        assertRowsCounter(0, 16, 2, 0, 0, 0);

        // Remove enough to cache cells
        robot.interact(() -> removeAll(table, IntegerRange.of(0, 21)));
        assertEquals(80.0, table.getVPos());
        assertState(table, IntegerRange.of(0, 13), IntegerRange.of(0, 6));
        assertCounter(0, 1, 0, 0, 0, 0, 14, 0);
        assertRowsCounter(0, 14, 0, 0, 2, 0);
    }

    @Test
    void testRemoveItemsSparse(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(50));
        robot.interact(() -> pane.getChildren().add(table));

        // Assert init
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);

        robot.interact(() -> removeAll(table, 0, 3, 4, 8, 10, 11));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(0, 1, 0, 0, 42, 0, 0, 0);
        assertRowsCounter(0, 16, 6, 0, 0, 0);
    }

    @Test
    void testChangeCellHeightTopLeft(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(50))
            .addEmptyColumns(9);
        robot.interact(() -> pane.getChildren().add(table));

        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);

        // Decrease and test
        robot.interact(() -> table.setRowsHeight(20.0));
        assertState(table, IntegerRange.of(0, 22), IntegerRange.of(0, 6));
        assertCounter(49, 1, 161, 49, 49, 0, 0, 0);
        assertRowsCounter(7, 7, 7, 0, 0, 0);
        assertLength(table, 50.0 * 20, 16 * 180);

        // Increase and test
        robot.interact(() -> table.setRowsHeight(50));
        assertState(table, IntegerRange.of(0, 11), IntegerRange.of(0, 6));
        assertCounter(0, 1, 84, 0, 0, 0, 77, 7);
        assertLength(table, 50 * 50, 16 * 180);
    }

    @Test
    void testChangeCellHeightMiddle(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(50))
            .addEmptyColumns(9);
        robot.interact(() -> {
            pane.getChildren().add(table);
            table.setVPos(600.0);
            table.setHPos(1000.0);
        });

        // Check positions!!!
        assertEquals(600, table.getVPos());
        assertEquals(1000, table.getHPos());

        assertState(table, IntegerRange.of(16, 31), IntegerRange.of(3, 9));
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);

        // Decrease and test
        robot.interact(() -> table.setRowsHeight(18.0));
        assertEquals(532, table.getVPos());
        assertEquals(1000, table.getHPos());
        assertState(table, IntegerRange.of(25, 49), IntegerRange.of(3, 9));
        assertCounter(63, 1, 175, 63, 126, 0, 0, 0);
        assertRowsCounter(9, 18, 18, 0, 0, 0);
        assertLength(table, 50.0 * 18, 16 * 180);
        // 25 rows. 7 in common and 9 reusable. 25 - 7 - 9 = 9 new rows
        // 9 * 7 = 63 new cells
        // (25 rows - 7 in common) * 7 columns = 126 cell updates (only items!!)
        // The index updates are only for new cells or column change

        // Increase and test
        robot.interact(() -> table.setRowsHeight(40.0));
        assertEquals(532, table.getVPos());
        assertEquals(1000, table.getHPos());
        assertState(table, IntegerRange.of(11, 24), IntegerRange.of(3, 9));
        assertCounter(0, 1, 98, 0, 98, 0, 77, 7);
        assertRowsCounter(0, 14, 14, 0, 11, 1);
        assertLength(table, 50.0 * 40, 16 * 180);
    }

    @Test
    void testChangeCellHeightBottomRight(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(50))
            .addEmptyColumns(9);
        robot.interact(() -> {
            pane.getChildren().add(table);
            table.scrollToLastRow();
            table.scrollToLastColumn();
        });

        assertState(table, IntegerRange.of(34, 49), IntegerRange.of(9, 15));
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);

        // Check positions!!!
        assertEquals(1232, table.getVPos());
        assertEquals(2480, table.getHPos());

        // Decrease and test
        robot.interact(() -> table.setRowsHeight(25.0));
        assertEquals(882, table.getVPos());
        assertEquals(2480, table.getHPos());
        assertState(table, IntegerRange.of(31, 49), IntegerRange.of(9, 15));
        assertCounter(21, 1, 133, 21, 21, 0, 0, 0);
        assertRowsCounter(3, 3, 3, 0, 0, 0);

        // Increase and test
        robot.interact(() -> table.setRowsHeight(44.0));
        assertEquals(882, table.getVPos());
        assertEquals(2480, table.getHPos());
        assertState(table, IntegerRange.of(18, 30), IntegerRange.of(9, 15));
        assertCounter(0, 1, 91, 0, 91, 0, 42, 0);
        assertRowsCounter(0, 13, 13, 0, 6, 0);
    }

    @Test
    void testChangeRowsFactory(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(50))
            .addEmptyColumns(9);
        VFXTableHelper<User> helper = table.getHelper();
        robot.interact(() -> pane.getChildren().add(table));

        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);

        // Test at both pos != 0
        robot.interact(() -> {
            table.setVPos(600.0);
            table.setHPos(1000.0);
        });
        assertState(table, IntegerRange.of(16, 31), IntegerRange.of(3, 9));
        assertCounter(48, 2, 48, 48, 160, 0, 48, 18); // Counter's stats are bigger because we scroll two times
        assertRowsCounter(0, 16, 16, 0, 0, 0);

        // Change rows factory and test
        robot.interact(() -> table.setRowsFactory(u -> new TestRow(u) {
            {
                StyleUtils.setBackground(this, ColorUtils.getRandomColor(0.2));
            }
        }));
        assertState(table, IntegerRange.of(16, 31), IntegerRange.of(3, 9));
        assertEquals(36, helper.visibleCells());
        assertEquals(112, helper.totalCells());
        assertCounter(0, 1, 0, 0, 0, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 16, 16);
    }

    @Test
    void testChangeRowsFactory2(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(50))
            .addEmptyColumns(9);
        robot.interact(() -> pane.getChildren().add(table));

        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);

        // Test at both pos != 0
        robot.interact(() -> {
            table.setVPos(600.0);
            table.setHPos(1000.0);
        });
        assertState(table, IntegerRange.of(16, 31), IntegerRange.of(3, 9));
        assertCounter(48, 2, 48, 48, 160, 0, 48, 18); // Counter's stats are bigger because we scroll two times
        assertRowsCounter(0, 16, 16, 0, 0, 0);

        // Change rows factory to null
        robot.interact(() -> table.setRowsFactory(null));
        assertState(table, INVALID_RANGE, IntegerRange.of(3, 9));
        assertCounter(0, 0, 0, 0, 0, 0, 112, 42);
        assertRowsCounter(0, 0, 0, 0, 16, 16);

        robot.interact(() -> table.setRowsFactory(u -> new TestRow(u) {
            {
                StyleUtils.setBackground(this, ColorUtils.getRandomColor(0.2));
            }
        }));
        assertState(table, IntegerRange.of(16, 31), IntegerRange.of(3, 9));
        assertCounter(42, 1, 112, 112, 112, 70, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testChangeCellFactory(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(50))
            .addEmptyColumns(9);
        robot.interact(() -> pane.getChildren().add(table));

        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);

        // Test at both pos != 0
        robot.interact(() -> {
            table.setVPos(600.0);
            table.setHPos(1000.0);
        });
        assertState(table, IntegerRange.of(16, 31), IntegerRange.of(3, 9));
        assertCounter(48, 2, 48, 48, 160, 0, 48, 18); // Counter's stats are bigger because we scroll two times
        assertRowsCounter(0, 16, 16, 0, 0, 0);

        // Change cell factory of column 5
        robot.interact(() -> {
            VFXTableColumn<User, VFXTableCell<User>> column = (VFXTableColumn<User, VFXTableCell<User>>) table.columns().get(5);
            column.setCellFactory(u -> Table.factory(u, User::blood, c -> StyleUtils.setBackground(c, ColorUtils.getRandomColor(0.2))));
        });
        assertState(table, IntegerRange.of(16, 31), IntegerRange.of(3, 9));
        assertCounter(16, 1, 16, 16, 16, 0, 0, 16);
        assertRowsCounter(0, 0, 0, 0, 0, 0);

        // Change cell factory of column 5 to null
        robot.interact(() -> {
            VFXTableColumn<User, VFXTableCell<User>> column = (VFXTableColumn<User, VFXTableCell<User>>) table.columns().get(5);
            column.setCellFactory(null);
        });
        assertState(table, IntegerRange.of(16, 31), IntegerRange.of(3, 9), 96);
        assertCounter(0, 1, 0, 0, 0, 0, 0, 16); // old cells are disposed immediately, no caching first
        assertRowsCounter(0, 0, 0, 0, 0, 0);

        // Scroll and verify everything is fine
        long duration = 500;
        Animation scroll = SequentialBuilder.build()
            .add(KeyFrames.of(duration, table.vPosProperty(), 0.0, Interpolator.LINEAR))
            .add(KeyFrames.of(duration, table.vPosProperty(), 600.0, Interpolators.LINEAR))
            .add(KeyFrames.of(duration, table.hPosProperty(), 0.0, Interpolators.LINEAR))
            .add(KeyFrames.of(duration, table.hPosProperty(), 1000.0, Interpolators.LINEAR))
            .getAnimation();
        robot.interact(scroll::play);
        sleep(duration * 4 + 200);
        assertState(table, IntegerRange.of(16, 31), IntegerRange.of(3, 9), 96);
        resetCounters();

        // Change factory outside range, nothing visible should change
        robot.interact(() -> {
            VFXTableColumn<User, VFXTableCell<User>> column = (VFXTableColumn<User, VFXTableCell<User>>) table.columns().getLast();
            column.setCellFactory(u -> Table.factory(u, _ -> "Outside", c -> StyleUtils.setBackground(c, ColorUtils.getRandomColor(0.2))));
        });
        assertState(table, IntegerRange.of(16, 31), IntegerRange.of(3, 9), 96);
        assertCounter(0, 1, 0, 0, 0, 0, 0, 0);
        assertRowsCounter(0, 0, 0, 0, 0, 0);

        // Cause empty state and try changing cell factory again
        robot.interact(() -> table.setRowsHeight(0.0));
        assertState(table, INVALID_RANGE, IntegerRange.of(3, 9));
        assertCounter(0, 0, 0, 0, 0, 0, 96, 36);
        assertRowsCounter(0, 0, 0, 0, 16, 6);

        // Now change factory, nothing should happen
        robot.interact(() -> {
            VFXTableColumn<User, VFXTableCell<User>> column = (VFXTableColumn<User, VFXTableCell<User>>) table.columns().get(5);
            column.setCellFactory(u -> Table.factory(u, User::blood, c -> StyleUtils.setBackground(c, ColorUtils.getRandomColor(0.2))));
        });
        assertState(table, INVALID_RANGE, IntegerRange.of(3, 9));
        assertCounter(0, 0, 0, 0, 0, 0, 0, 0);
        assertRowsCounter(0, 0, 0, 0, 0, 0);
    }

    @Test
    void testChangeColumnsSizeTopLeft(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(50))
            .addEmptyColumns(9);
        robot.interact(() -> {
            // Let's start from a height >= 48 otherwise things get complicated for the decrease test
            table.setColumnsSize(180, 52);
            pane.getChildren().add(table);
        });

        assertState(table, IntegerRange.of(0, 14), IntegerRange.of(0, 6));
        assertCounter(105, 1, 105, 105, 105, 0, 0, 0);
        assertRowsCounter(15, 15, 15, 0, 0, 0);

        // Decrease and test
        robot.interact(() -> table.setColumnsSize(100, 24));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 8));
        assertCounter(39, 1, 144, 39, 39, 0, 0, 0);
        assertRowsCounter(1, 1, 1, 0, 0, 0);

        // Increase and test
        robot.interact(() -> table.setColumnsSize(240, 100));
        assertState(table, IntegerRange.of(0, 13), IntegerRange.of(0, 5));
        assertCounter(0, 1, 84, 0, 0, 0, 60, 18);
        assertRowsCounter(0, 0, 0, 0, 2, 0);

        // Change, do not cause any actual change
        robot.interact(() -> table.setColumnsSize(230, 90));
        assertState(table, IntegerRange.of(0, 13), IntegerRange.of(0, 5));
        assertCounter(0, 1, 84, 0, 0, 0, 0, 0);
        assertRowsCounter(0, 0, 0, 0, 0, 0);
    }

    @Test
    void testChangeColumnsSizeMiddle(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(50))
            .addEmptyColumns(9);
        robot.interact(() -> {
            // Let's start from a height >= 48 otherwise things get complicated for the decrease test
            table.setColumnsSize(180, 52);
            table.setVPos(600.0);
            table.setHPos(1000.0);
            pane.getChildren().add(table);
        });

        // Check positions!!!
        assertEquals(600.0, table.getVPos());
        assertEquals(1000.0, table.getHPos());

        assertState(table, IntegerRange.of(16, 30), IntegerRange.of(3, 9));
        assertCounter(105, 1, 105, 105, 105, 0, 0, 0);
        assertRowsCounter(15, 15, 15, 0, 0, 0);
        assertScrollable(table, 50 * 32 - 400 + 52, 180 * 16 - 400);

        // Decrease and test
        robot.interact(() -> table.setColumnsSize(100, 24));
        assertEquals(600.0, table.getVPos());
        assertEquals(1000.0, table.getHPos());
        assertState(table, IntegerRange.of(16, 31), IntegerRange.of(7, 15));
        assertCounter(99, 1, 144, 99, 99, 0, 60, 20);
        assertRowsCounter(1, 1, 1, 0, 0, 0);
        assertScrollable(table, 50 * 32 - 400 + 24, 100 * 16 - 400);

        // Increase and test
        robot.interact(() -> table.setColumnsSize(240, 100));
        assertEquals(600.0, table.getVPos());
        assertEquals(1000.0, table.getHPos());
        assertState(table, IntegerRange.of(16, 29), IntegerRange.of(2, 7));
        assertCounter(30, 1, 84, 70, 70, 40, 130, 48);
        assertRowsCounter(0, 0, 0, 0, 2, 0);
        assertScrollable(table, 50 * 32 - 400 + 100, 240 * 16 - 400);

        // Unlike v1 this does cause a change: at hPos 1000 a 230 baseline puts the viewport's right
        // edge in column 6 rather than 5, so the range widens by one
        robot.interact(() -> table.setColumnsSize(230, 90));
        assertEquals(600.0, table.getVPos());
        assertEquals(1000.0, table.getHPos());
        assertState(table, IntegerRange.of(16, 29), IntegerRange.of(2, 8));
        assertCounter(4, 1, 98, 14, 14, 10, 0, 0);
        assertRowsCounter(0, 0, 0, 0, 0, 0);
        assertScrollable(table, 50 * 32 - 400 + 90, 230 * 16 - 400);
    }

    @Test
    void testChangeColumnsSizeBottomRight(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(50))
            .addEmptyColumns(9);
        robot.interact(() -> {
            // Let's start from a height >= 48 otherwise things get complicated for the decrease test
            table.setColumnsSize(180, 52);
            table.scrollToLastRow();
            table.scrollToLastColumn();
            pane.getChildren().add(table);
        });

        // Check positions!!!
        assertEquals(1252.0, table.getVPos());
        assertEquals(2480.0, table.getHPos());

        assertState(table, IntegerRange.of(35, 49), IntegerRange.of(9, 15));
        assertCounter(105, 1, 105, 105, 105, 0, 0, 0);
        assertRowsCounter(15, 15, 15, 0, 0, 0);
        assertScrollable(table, 50 * 32 - 400 + 52, 180 * 16 - 400);

        // Decrease and test
        robot.interact(() -> table.setColumnsSize(100, 24));
        assertEquals(1224.0, table.getVPos());
        assertEquals(1200.0, table.getHPos());
        assertState(table, IntegerRange.of(34, 49), IntegerRange.of(8, 15));
        assertCounter(23, 1, 128, 23, 23, 0, 0, 0);
        assertRowsCounter(1, 1, 1, 0, 0, 0);
        assertScrollable(table, 50 * 32 - 400 + 24, 100 * 16 - 400);

        // Increase and test
        robot.interact(() -> table.setColumnsSize(240, 100));
        assertEquals(1224.0, table.getVPos());
        assertEquals(1200.0, table.getHPos());
        assertState(table, IntegerRange.of(36, 49), IntegerRange.of(3, 8));
        assertCounter(70, 1, 84, 70, 70, 0, 114, 42);
        assertRowsCounter(0, 0, 0, 0, 2, 0);
        assertScrollable(table, 50 * 32 - 400 + 100, 240 * 16 - 400);

        // Change, do not cause any actual change
        robot.interact(() -> table.setColumnsSize(230, 90));
        assertEquals(1224.0, table.getVPos());
        assertEquals(1200.0, table.getHPos());
        assertState(table, IntegerRange.of(36, 49), IntegerRange.of(3, 8));
        assertCounter(0, 1, 84, 0, 0, 0, 0, 0);
        assertRowsCounter(0, 0, 0, 0, 0, 0);
        assertScrollable(table, 50 * 32 - 400 + 90, 230 * 16 - 400);
    }

    @Test
    void testChangeColumnsSizeSeparately(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(50))
            .addEmptyColumns(9);
        robot.interact(() -> {
            // Let's start from a height >= 48 otherwise things get complicated for the decrease test
            table.setColumnsSize(180, 52);
            pane.getChildren().add(table);
        });

        assertState(table, IntegerRange.of(0, 14), IntegerRange.of(0, 6));
        assertCounter(105, 1, 105, 105, 105, 0, 0, 0);
        assertRowsCounter(15, 15, 15, 0, 0, 0);

        // Decrease height
        robot.interact(() -> table.setColumnsHeight(24));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(7, 1, 112, 7, 7, 0, 0, 0);
        assertRowsCounter(1, 1, 1, 0, 0, 0);

        // Decrease width
        robot.interact(() -> table.setColumnsWidth(100));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 8));
        assertCounter(32, 1, 144, 32, 32, 0, 0, 0);
        assertRowsCounter(0, 0, 0, 0, 0, 0);

        // Increase height
        robot.interact(() -> table.setColumnsHeight(100));
        assertState(table, IntegerRange.of(0, 13), IntegerRange.of(0, 8));
        assertCounter(0, 1, 126, 0, 0, 0, 18, 0);
        assertRowsCounter(0, 0, 0, 0, 2, 0);

        // Increase width
        robot.interact(() -> table.setColumnsWidth(240));
        assertState(table, IntegerRange.of(0, 13), IntegerRange.of(0, 5));
        assertCounter(0, 1, 84, 0, 0, 0, 42, 18);
        assertRowsCounter(0, 0, 0, 0, 0, 0);
    }

    @Test
    void testChangeColumnsSizeSeparatelyNoItems(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(0))
            .addEmptyColumns(9);
        robot.interact(() -> {
            // Let's start from a height >= 48 otherwise things get complicated for the decrease test
            table.setColumnsSize(180, 52);
            pane.getChildren().add(table);
        });

        assertState(table, INVALID_RANGE, IntegerRange.of(0, 6), 0);
        assertCounter(0, 0, 0, 0, 0, 0, 0, 0);
        assertRowsCounter(0, 0, 0, 0, 0, 0);

        // Decrease height
        robot.interact(() -> table.setColumnsHeight(24));
        assertState(table, INVALID_RANGE, IntegerRange.of(0, 6), 0);
        assertCounter(0, 0, 0, 0, 0, 0, 0, 0);
        assertRowsCounter(0, 0, 0, 0, 0, 0);

        // Decrease width
        robot.interact(() -> table.setColumnsWidth(100));
        assertState(table, INVALID_RANGE, IntegerRange.of(0, 8), 0);
        assertCounter(0, 0, 0, 0, 0, 0, 0, 0);
        assertRowsCounter(0, 0, 0, 0, 0, 0);

        // Increase height
        robot.interact(() -> table.setColumnsHeight(100));
        assertState(table, INVALID_RANGE, IntegerRange.of(0, 8), 0);
        assertCounter(0, 0, 0, 0, 0, 0, 0, 0);
        assertRowsCounter(0, 0, 0, 0, 0, 0);

        // Increase width
        robot.interact(() -> table.setColumnsWidth(240));
        assertState(table, INVALID_RANGE, IntegerRange.of(0, 5), 0);
        assertCounter(0, 0, 0, 0, 0, 0, 0, 0);
        assertRowsCounter(0, 0, 0, 0, 0, 0);

        // See Table class for why there are no layouts
    }

    @Test
    void testChangeColumnsSizeBelowUserPref(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(50))
            .addEmptyColumns(9);
        robot.interact(() -> {
            table.setColumnsSize(180, 32);
            pane.getChildren().add(table);
        });
        resetCounters();

        // A pref smaller than the baseline is masked: the cache keeps no override for it
        robot.interact(() -> table.columns().get(2).setUserPrefWidth(150.0));
        assertLength(table, 50.0 * 32, 16 * 180);

        // Shrinking the baseline below that pref must un-mask it: column 2 becomes the only override.
        // The cache sweeps `widths[]` alone, where -1 means both "no pref" and "pref masked by a
        // bigger baseline", so a shrink cannot tell the two apart without asking the column again.
        robot.interact(() -> table.setColumnsSize(100, 32));
        assertLength(table, 50.0 * 32, 15 * 100 + 150);
        assertEquals(150.0, table.columns().get(2).getBoundsInParent().getWidth(), FP_ASSERTIONS_DELTA);
    }

    @Test
    void testChangeColumnWidth(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(50))
            .addEmptyColumns(3);
        robot.interact(() -> pane.getChildren().add(table));

        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);
        assertLength(table, 50 * 32, 10 * 180);

        double inc1 = 200, inc2 = 300;
        // Increase width of column (out of viewport)
        robot.interact(() -> setColumnWidth(table, 8, inc1));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(0, 1, 0, 0, 0, 0, 0, 0);
        assertRowsCounter(0, 0, 0, 0, 0, 0);
        assertLength(table, 50 * 32, (9 * 180) + inc1);

        // Increase width of column (in viewport)
        robot.interact(() -> setColumnWidth(table, 1, inc2));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 5)); // now the visible columns are 0 and 1 (+4 buffer)
        assertCounter(0, 1, 80, 0, 0, 0, 16, 6);
        assertRowsCounter(0, 0, 0, 0, 0, 0);
        assertLength(table, 50 * 32, (8 * 180) + inc1 + inc2);

        double dec1 = 100, dec2 = 100;
        // Decrease below minimum (out of viewport)
        robot.interact(() -> setColumnWidth(table, 6, dec1));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 5));
        assertCounter(0, 0, 0, 0, 0, 0, 0, 0); // effective width unchanged: no layout at all
        assertRowsCounter(0, 0, 0, 0, 0, 0);
        assertLength(table, 50 * 32, (7 * 180) + inc1 + inc2 + 180); // doesn't go below min, therefore +180

        // Decrease below the minimum (in viewport)
        robot.interact(() -> setColumnWidth(table, 1, dec2));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(6, 1, 96, 16, 16, 10, 0, 0);
        assertRowsCounter(0, 0, 0, 0, 0, 0);
        assertLength(table, 50 * 32, (7 * 180) + inc1 + 180 + 180); // columns 1 and 6 both back to 180 (min)

        // Increase width of column (in viewport), not enough to change the range
        robot.interact(() -> setColumnWidth(table, 5, 200));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(0, 1, 32, 0, 0, 0, 0, 0); // columns 5 and 6 shift, 2 cells per row
        assertRowsCounter(0, 0, 0, 0, 0, 0);
        assertLength(table, 50 * 32, (6 * 180) + inc1 + 180 + 180 + 200);
    }

    @Test
    void testScrollNonUniformColumns(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(50))
            .addEmptyColumns(3);
        robot.interact(() -> {
            // Varied but deterministic widths
            ObservableList<VFXTableColumn<User, ? extends VFXTableCell<User>>> columns = table.columns();
            for (int i = 0; i < columns.size(); i++) columns.get(i).setUserPrefWidth(i % 2 == 0 ? 180 : 220);
            pane.getChildren().add(table);
        });

        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);
        assertLength(table, 50 * 32, (5 * 180) + (5 * 220));

        robot.interact(() -> table.setHPos(table.getMaxHScroll() / 2.0));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(2, 8));
        assertCounter(32, 1, 32, 32, 32, 0, 32, 12);
        assertRowsCounter(0, 0, 0, 0, 0, 0);

        robot.interact(table::scrollToLastColumn);
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(4, 9));
        assertCounter(16, 1, 16, 16, 16, 0, 32, 12);
        assertRowsCounter(0, 0, 0, 0, 0, 0);
    }

    @Test
    void testColumnsChangedWithUserPrefs(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(50))
            .addEmptyColumns(3);
        robot.interact(() -> {
            ObservableList<VFXTableColumn<User, ? extends VFXTableCell<User>>> columns = table.columns();
            for (int i = 0; i < columns.size(); i++) columns.get(i).setUserPrefWidth(i % 2 == 0 ? 180 : 220);
            pane.getChildren().add(table);
        });

        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);
        assertLength(table, 50 * 32, (5 * 180) + (5 * 220));

        // Remove a column carrying a pref: the cache rebuilds and must read the remaining prefs back
        robot.interact(() -> table.columns().remove(1));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertLength(table, 50 * 32, (5 * 180) + (4 * 220));

        // Add one with a pref wider than any other, at 0 so it also moves the range
        robot.interact(() -> {
            EmptyColumn added = new EmptyColumn("Wide", 999);
            added.setUserPrefWidth(260.0);
            table.columns().addFirst(added);
        });
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 5));
        assertLength(table, 50 * 32, (5 * 180) + (4 * 220) + 260);
    }

    // Invariants rather than literal ranges. The other tests pin specific scenarios with exact
    // numbers, which is what makes them useful but also what makes them brittle to a change in the
    // default buffer size or column widths. These assert the properties x-axis virtualization is
    // supposed to guarantee, so they keep holding across those.
    @Test
    void testColumnsRangeIsBounded(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(50))
            .addEmptyColumns(13); // 7 default + 13
        robot.interact(() -> pane.getChildren().add(table));

        int columnsCount = table.columns().size();
        assertEquals(20, columnsCount);

        // The range is a window over the columns, never the whole list
        IntegerRange initial = table.getHelper().columnsRange();
        assertTrue(initial.diff() + 1 < columnsCount);

        // Narrowing the viewport cannot widen the range
        robot.interact(() -> setWindowSize(table, 260, -1));
        IntegerRange narrow = table.getHelper().columnsRange();
        assertTrue(narrow.diff() <= initial.diff());

        // Widening it cannot shrink it
        robot.interact(() -> setWindowSize(table, 900, -1));
        IntegerRange wide = table.getHelper().columnsRange();
        assertTrue(wide.diff() >= narrow.diff());

        // At max scroll the range must still be valid, in bounds, contain the last column, and
        // remain a window. The state has to agree with it
        robot.interact(() -> table.setHPos(Double.MAX_VALUE));
        IntegerRange atMax = table.getHelper().columnsRange();
        assertNotEquals(INVALID_RANGE, atMax);
        assertTrue(atMax.getMin() >= 0);
        assertEquals(columnsCount - 1, atMax.getMax());
        assertTrue(atMax.diff() + 1 < columnsCount);
        assertState(table, table.getHelper().rowsRange(), atMax);
    }

    @Test
    void testManualUpdate(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(20));
        robot.interact(() -> pane.getChildren().add(table));

        // Assert init
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);

        // Get text before change row 5
        VFXTableCell<User> row5 = table.getState().getRowsByIndex().get(5).cellsByIndex().get(0);
        Label label5 = (Label) row5.toNode().lookup(".label");
        String text5 = label5.getText();

        // Change item 5
        robot.interact(() -> table.getItems().get(5).setFirstName(faker.name().firstName()));
        assertEquals(text5, label5.getText()); // No automatic update

        // Force update (all)
        robot.interact((Runnable) table::update);
        assertNotEquals(text5, label5.getText());

        // Get text before change row 7
        VFXTableCell<User> row7 = table.getState().getRowsByIndex().get(7).cellsByIndex().get(0);
        Label label7 = (Label) row7.toNode().lookup(".label");
        String text7 = label7.getText();

        // Change item 7
        robot.interact(() -> table.getItems().get(7).setFirstName(faker.name().firstName()));
        assertEquals(text7, label7.getText()); // No automatic update
    }

    @SuppressWarnings("unchecked")
    @Test
    void testAutomaticUpdate(FxRobot robot) {
        StackPane pane = setupStage();
        // Prepare table
        VFXTable<FXUser> table = new VFXTable<>(fxusers(20));
        table.setColumnsSize(size(180, 32));
        CSSFragment.Builder.build()
            .select(".vfx-table")
            .border("#353839")
            .select(".vfx-table > .viewport > .columns")
            .border("transparent transparent #353839 transparent")
            .select(".vfx-table > .viewport > .columns > .vfx-column")
            .padding(insets().horizontal(10.0))
            .border("transparent #353839 transparent transparent")
            .select(".vfx-table > .viewport > .columns > .vfx-column:hover > .overlay")
            .and(".vfx-table > .viewport > .columns > .vfx-column:dragged > .overlay")
            .background("rgba(53, 56, 57, 0.1)")
            .select(".vfx-table > .viewport > .rows > .vfx-row")
            .border("#353839")
            .borderInsets(uniform(1.25))
            .borderWidth(0.5)
            .select(".vfx-table > .viewport > .rows > .vfx-row > .table-cell")
            .padding(insets().horizontal(10.0))
            .applyOn(table);

        int ICON_SIZE = 18;
        Color ICON_COLOR = Color.rgb(53, 57, 53);

        VFXSimpleTableColumn<FXUser, VFXObservingTableCell<FXUser, String>> firstNameColumn = new VFXSimpleTableColumn<>("First name");
        firstNameColumn.setCellFactory(u -> new VFXObservingTableCell<>(u, FXUser::firstNameProperty));
        firstNameColumn.setGraphic(new MFXFontIcon("fas-user", ICON_SIZE, ICON_COLOR));
        VFXSimpleTableColumn<FXUser, VFXObservingTableCell<FXUser, String>> lastNameColumn = new VFXSimpleTableColumn<>("Last name");
        lastNameColumn.setCellFactory(u -> new VFXObservingTableCell<>(u, FXUser::lastNameProperty));
        lastNameColumn.setGraphic(new MFXFontIcon("fas-user", ICON_SIZE, ICON_COLOR));
        VFXSimpleTableColumn<FXUser, VFXObservingTableCell<FXUser, Number>> birthColumn = new VFXSimpleTableColumn<>("Birth year");
        birthColumn.setCellFactory(u -> new VFXObservingTableCell<>(u, FXUser::birthYearProperty));
        birthColumn.setGraphic(new MFXFontIcon("fas-cake-candles", ICON_SIZE, ICON_COLOR));
        VFXSimpleTableColumn<FXUser, VFXObservingTableCell<FXUser, String>> zodiacColumn = new VFXSimpleTableColumn<>("Zodiac Sign");
        zodiacColumn.setCellFactory(u -> new VFXObservingTableCell<>(u, FXUser::zodiacProperty));
        zodiacColumn.setGraphic(new MFXFontIcon("fas-star", ICON_SIZE, ICON_COLOR));
        VFXSimpleTableColumn<FXUser, VFXObservingTableCell<FXUser, String>> countryColumn = new VFXSimpleTableColumn<>("Country");
        countryColumn.setCellFactory(u -> new VFXObservingTableCell<>(u, FXUser::countryProperty));
        countryColumn.setGraphic(new MFXFontIcon("fas-globe", ICON_SIZE, ICON_COLOR));
        VFXSimpleTableColumn<FXUser, VFXObservingTableCell<FXUser, String>> bloodColumn = new VFXSimpleTableColumn<>("Blood");
        bloodColumn.setCellFactory(u -> new VFXObservingTableCell<>(u, FXUser::bloodProperty));
        bloodColumn.setGraphic(new MFXFontIcon("fas-droplet", ICON_SIZE, ICON_COLOR));
        VFXSimpleTableColumn<FXUser, VFXObservingTableCell<FXUser, String>> animalColumn = new VFXSimpleTableColumn<>("Pet");
        animalColumn.setCellFactory(u -> new VFXObservingTableCell<>(u, FXUser::petProperty));
        animalColumn.setGraphic(new MFXFontIcon("fas-paw", ICON_SIZE, ICON_COLOR));
        table.columns().addAll(firstNameColumn, lastNameColumn, birthColumn, zodiacColumn, countryColumn, bloodColumn, animalColumn);
        robot.interact(() -> pane.getChildren().add(table));

        // Assert init
        assertEquals(IntegerRange.of(0, 15), table.getRowsRange());
        assertEquals(IntegerRange.of(0, 6), table.getColumnsRange());

        // Get text before change row 5
        VFXTableCell<FXUser> row5 = table.getState().getRowsByIndex().get(5).cellsByIndex().get(0);
        Label label5 = (Label) row5.toNode().lookup(".label");
        String text5 = label5.getText();

        // Change item 5
        robot.interact(() -> table.getItems().get(5).setFirstName(faker.name().firstName()));
        assertNotEquals(text5, label5.getText());

        // Get text before change row 7
        VFXTableCell<FXUser> row7 = table.getState().getRowsByIndex().get(7).cellsByIndex().get(0);
        Label label7 = (Label) row7.toNode().lookup(".label");
        String text7 = label7.getText();

        // Change item 7
        robot.interact(() -> table.getItems().get(7).setFirstName(faker.name().firstName()));
        assertNotEquals(text7, label7.getText());

        // Scroll to check everything is ok
        Animation a1 = TimelineBuilder.build()
            .add(KeyFrames.of(500, table.vPosProperty(), table.getMaxVScroll(), Interpolator.LINEAR))
            .getAnimation();
        robot.interact(a1::play);
        sleep(550);

        Animation a2 = TimelineBuilder.build()
            .add(KeyFrames.of(500, table.hPosProperty(), table.getMaxHScroll(), Interpolator.LINEAR))
            .getAnimation();
        robot.interact(a2::play);
        sleep(550);

        Animation a3 = TimelineBuilder.build()
            .add(KeyFrames.of(500, table.vPosProperty(), 0.0, Interpolators.LINEAR))
            .getAnimation();
        robot.interact(a3::play);
        sleep(550);
    }

    @Test
    void testLastColumnResize(FxRobot robot) {
        StackPane pane = setupStage();
        Table table = new Table(users(50));
        robot.interact(() -> pane.getChildren().add(table));

        // Assert init
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(112, 1, 112, 112, 112, 0, 0, 0);
        assertRowsCounter(16, 16, 16, 0, 0, 0);
        assertLength(table, 50 * 32, 1260);

        // Expand
        robot.interact(() -> setWindowSize(pane, 1600, -1));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(0, 1, 16, 0, 0, 0, 0, 0);
        assertLength(table, 50 * 32, 1600);

        // Shrink a bit
        robot.interact(() -> setWindowSize(pane, 1300, -1));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(0, 1, 16, 0, 0, 0, 0, 0);
        assertLength(table, 50 * 32, 1300);

        // Shrink to the right size
        robot.interact(() -> setWindowSize(pane, 1260, -1));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(0, 1, 16, 0, 0, 0, 0, 0);
        assertLength(table, 50 * 32, 1260);

        // Shrink again a no layouts should occur
        robot.interact(() -> setWindowSize(pane, 800, -1));
        assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
        assertCounter(0, 1, 0, 0, 0, 0, 0, 0);
        assertLength(table, 50 * 32, 1260);
    }
}
