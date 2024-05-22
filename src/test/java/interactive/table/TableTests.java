package interactive.table;

import assets.TestResources;
import com.google.gson.reflect.TypeToken;
import io.github.palexdev.mfxcore.base.beans.Size;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.controls.Label;
import io.github.palexdev.mfxcore.utils.RandomUtils;
import io.github.palexdev.mfxcore.utils.fx.CSSFragment;
import io.github.palexdev.mfxcore.utils.fx.ColorUtils;
import io.github.palexdev.mfxcore.utils.fx.StyleUtils;
import io.github.palexdev.mfxeffects.animations.Animations.KeyFrames;
import io.github.palexdev.mfxeffects.animations.Animations.SequentialBuilder;
import io.github.palexdev.mfxeffects.animations.Animations.TimelineBuilder;
import io.github.palexdev.mfxeffects.enums.Interpolators;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
import io.github.palexdev.mfxresources.fonts.fontawesome.FontAwesomeSolid;
import io.github.palexdev.virtualizedfx.cells.VFXObservingTableCell;
import io.github.palexdev.virtualizedfx.cells.base.VFXTableCell;
import io.github.palexdev.virtualizedfx.enums.BufferSize;
import io.github.palexdev.virtualizedfx.table.VFXTable;
import io.github.palexdev.virtualizedfx.table.VFXTableColumn;
import io.github.palexdev.virtualizedfx.table.VFXTableHelper;
import io.github.palexdev.virtualizedfx.table.VFXTableState;
import io.github.palexdev.virtualizedfx.table.defaults.VFXDefaultTableColumn;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import model.FXUser;
import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import utils.Utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static interactive.table.TableTestUtils.*;
import static interactive.table.TableTestUtils.Table.emptyColumns;
import static io.github.palexdev.virtualizedfx.table.VFXTableColumn.swapColumns;
import static io.github.palexdev.virtualizedfx.utils.Utils.INVALID_RANGE;
import static model.FXUser.fxusers;
import static model.User.faker;
import static model.User.users;
import static org.junit.jupiter.api.Assertions.*;
import static utils.TestFXUtils.*;
import static utils.Utils.*;

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

	private static final List<User> AUTOSIZE_USERS = TestResources.gsonLoad("Autosize_Users.json", new TypeToken<List<User>>() {}.getType());

	@Start
	void start(Stage stage) {
		stage.show();
	}

	@BeforeEach
	void setup() {
		resetCounters();
	}

	@Test
	void testInitAndGeometry(FxRobot robot) {
		StackPane pane = setupStage();
		Table table = new Table(users(50));
		robot.interact(() -> pane.getChildren().add(table));

		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(112, 1, 112, 112, 0, 0, 0);
		assertRowsCounter(16, 16, 16, 0, 0, 0);

		// Expand and test again
		robot.interact(() -> setWindowSize(pane, 600, -1));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 0, 0, 0, 0);

		robot.interact(() -> setWindowSize(pane, -1, 600));
		assertState(table, IntegerRange.of(0, 21), IntegerRange.of(0, 6));
		assertCounter(42, 1, 42, 42, 0, 0, 0);
		assertRowsCounter(6, 6, 6, 0, 0, 0);

		// Shrink and test again
		robot.interact(() -> setWindowSize(pane, 300, -1));
		assertState(table, IntegerRange.of(0, 21), IntegerRange.of(0, 5));
		assertCounter(0, 1, 0, 0, 0, 22, 12); // 22 for the same column

		robot.interact(() -> setWindowSize(pane, -1, 300));
		assertState(table, IntegerRange.of(0, 12), IntegerRange.of(0, 5));
		assertCounter(0, 1, 0, 0, 0, 54, 0); // 54 for different columns. 9 for each. No disposals.
		assertRowsCounter(0, 0, 0, 0, 9, 0);

		robot.interact(() -> setWindowSize(pane, -1, 500));
		assertState(table, IntegerRange.of(0, 18), IntegerRange.of(0, 5));
		assertCounter(0, 1, 36, 36, 36, 0, 0);
		assertRowsCounter(0, 6, 6, 6, 0, 0);

		// Edge case set width to 0
		robot.interact(() -> {
			table.setMinWidth(0);
			table.setPrefWidth(0);
			table.setMaxWidth(0);
			robot.interact(() -> setWindowSize(pane, 0, -1));
		});
		assertState(table, INVALID_RANGE, INVALID_RANGE);
		assertCounter(0, 0, 0, 0, 0, 114, 72); // Each column already has 3 cells in cache, so 54 + 18 = 72
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
		assertCounter(112, 1, 112, 112, 0, 0, 0);
		assertRowsCounter(16, 16, 16, 0, 0, 0);

		// Expand (won't have any effect since columns are already all shown)
		robot.interact(() -> setWindowSize(pane, 600, -1));
		assertEquals(660.0, table.getHPos());
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 0, 0, 0, 0);

		// Shrink
		robot.interact(() -> {
			robot.interact(() -> setWindowSize(pane, 100, -1));
			// Why you be like that JavaFX :smh:
			table.setMinWidth(100.0);
			table.setPrefWidth(100.0);
			table.setMaxWidth(100.0);
		});
		assertEquals(660.0, table.getHPos());
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(1, 5)); // We are at hPos 660.0. floor(660 / 180) = 3 as first visible column
		assertCounter(0, 1, 0, 0, 0, 32, 12);
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
		assertCounter(112, 1, 112, 112, 0, 0, 0);
		assertRowsCounter(16, 16, 16, 0, 0, 0);

		// Expand
		robot.interact(() -> setWindowSize(pane, -1, 600));
		assertEquals(1032.0, table.getVPos());
		assertState(table, IntegerRange.of(28, 49), IntegerRange.of(0, 6));
		assertCounter(42, 1, 42, 42, 0, 0, 0);
		assertRowsCounter(6, 6, 6, 0, 0, 0);

		// Shrink
		robot.interact(() -> setWindowSize(pane, -1, 300));
		assertEquals(1032.0, table.getVPos());
		assertState(table, IntegerRange.of(30, 42), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 0, 0, 63, 0);
		assertRowsCounter(0, 0, 0, 0, 9, 0);
	}

	@Test
	void testLastColumnResize(FxRobot robot) {
		StackPane pane = setupStage();
		Table table = new Table(users(50));
		robot.interact(() -> pane.getChildren().add(table));

		// Assert init
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(112, 1, 112, 112, 0, 0, 0);
		assertRowsCounter(16, 16, 16, 0, 0, 0);

		// Expand
		robot.interact(() -> setWindowSize(pane, 1600, -1));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 0, 0, 0, 0);

		// Shrink a bit
		robot.interact(() -> setWindowSize(pane, 1300, -1));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 0, 0, 0, 0);

		// Shrink to the right size
		robot.interact(() -> setWindowSize(pane, 1260, -1));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 0, 0, 0, 0);

		// Shrink again a no layouts should occur
		robot.interact(() -> setWindowSize(pane, 800, -1));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 0, 0, 0, 0);
	}

	@Test
	void testPopulateCache(FxRobot robot) {
		StackPane pane = setupStage();
		Table table = new Table(users(50));
		table.populateCache();
		resetCounters();
		robot.interact(() -> pane.getChildren().add(table));

		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(112, 1, 112, 112, 0, 0, 0);
		assertRowsCounter(6, 16, 16, 10, 0, 0);
	}

	@Test
	void testPopulateCacheAll(FxRobot robot) {
		StackPane pane = setupStage();
		Table table = new Table(users(50));
		table.populateCacheAll();
		resetCounters();
		robot.interact(() -> pane.getChildren().add(table));

		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(42, 1, 112, 112, 70, 0, 0);
		assertRowsCounter(6, 16, 16, 10, 0, 0);
	}

	@Test
	void testScrollVertical(FxRobot robot) {
		StackPane pane = setupStage();
		Table table = new Table(users(50));
		robot.interact(() -> pane.getChildren().add(table));

		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(112, 1, 112, 112, 0, 0, 0);
		assertRowsCounter(16, 16, 16, 0, 0, 0);

		robot.interact(() -> table.setVPos(400.0));
		assertState(table, IntegerRange.of(10, 25), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 70, 0, 0, 0);
		assertRowsCounter(0, 10, 10, 0, 0, 0);

		robot.interact(table::scrollToLastRow);
		assertState(table, IntegerRange.of(34, 49), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 112, 0, 0, 0);
		assertRowsCounter(0, 16, 16, 0, 0, 0);

		robot.interact(() -> {
			table.setItems(users(35));
			table.scrollToFirstRow(); // Here range becomes [0, 15]
			counter.reset();
			rowsCounter.reset();
			table.setVPos(300.0);
		});
		assertState(table, IntegerRange.of(7, 22), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 49, 0, 0, 0);
		assertRowsCounter(0, 7, 7, 0, 0, 0);
	}

	@Test
	void testScrollHorizontal(FxRobot robot) {
		StackPane pane = setupStage();
		Table table = new Table(users(50))
			.addEmptyColumns(10);
		robot.interact(() -> pane.getChildren().add(table));

		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(112, 1, 112, 112, 0, 0, 0);
		assertRowsCounter(16, 16, 16, 0, 0, 0);

		robot.interact(() -> table.setHPos(800.0));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(2, 8));
		assertCounter(32, 1, 32, 32, 0, 32, 12);

		robot.interact(table::scrollToLastColumn);
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(10, 16));
		assertCounter(112, 1, 112, 112, 0, 112, 42);

		// The following test is complex but interesting because we are also removing columns from the table
		// For this reason I'm going to test every step
		robot.interact(() -> table.getColumns().remove(10, 16));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(4, 10));
		assertCounter(46, 1, 112, 96, 50, 96, 36);

		robot.interact(table::scrollToFirstColumn);
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(24, 1, 64, 64, 40, 64, 24);

		robot.interact(() -> table.setHPos(600.0));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(1, 7));
		assertCounter(6, 1, 16, 16, 10, 16, 6);
	}

	@Test
	void testScrollHorizontalNoItems(FxRobot robot) {
		StackPane pane = setupStage();
		Table table = new Table(users(0))
			.addEmptyColumns(10);
		VFXTableHelper<User> helper = table.getHelper();
		robot.interact(() -> pane.getChildren().add(table));

		assertState(table, INVALID_RANGE, IntegerRange.of(0, 6), 0);
		assertCounter(0, 0, 0, 0, 0, 0, 0);
		assertRowsCounter(0, 0, 0, 0, 0, 0);

		Animation a1 = TimelineBuilder.build()
			.add(KeyFrames.of(500, table.hPosProperty(), 800, Interpolator.LINEAR))
			.getAnimation();
		robot.interact(a1::play);
		sleep(550);
		assertState(table, INVALID_RANGE, IntegerRange.of(2, 8), 0);
		assertCounter(0, 0, 0, 0, 0, 0, 0);

		Animation a2 = TimelineBuilder.build()
			.add(KeyFrames.of(500, table.hPosProperty(), table.getMaxHScroll(), Interpolators.LINEAR))
			.getAnimation();
		robot.interact(a2::play);
		sleep(550);
		assertState(table, INVALID_RANGE, IntegerRange.of(10, 16), 0);
		assertCounter(0, 0, 0, 0, 0, 0, 0);

		// The following test is complex but interesting because we are also removing columns from the table
		// For this reason I'm going to test every step
		robot.interact(() -> table.getColumns().remove(10, 16));
		assertState(table, INVALID_RANGE, IntegerRange.of(4, 10), 0);
		assertCounter(0, 0, 0, 0, 0, 0, 0);

		robot.interact(table::scrollToFirstColumn);
		assertState(table, INVALID_RANGE, IntegerRange.of(0, 6), 0);
		assertCounter(0, 0, 0, 0, 0, 0, 0);

		robot.interact(() -> table.setHPos(600.0));
		assertState(table, INVALID_RANGE, IntegerRange.of(1, 7), 0);
		assertCounter(0, 0, 0, 0, 0, 0, 0);
	}

	@Test
	void testBufferChangeTopLeft(FxRobot robot) {
		StackPane pane = setupStage();
		Table table = new Table(users(50))
			.addEmptyColumns(9);
		VFXTableHelper<User> helper = table.getHelper();
		robot.interact(() -> pane.getChildren().add(table));

		// Medium Buffer
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertEquals(36, helper.visibleCells());
		assertEquals(112, helper.totalCells());
		assertCounter(112, 1, 112, 112, 0, 0, 0);
		assertRowsCounter(16, 16, 16, 0, 0, 0);

		// Small Buffer
		robot.interact(() -> table.setBufferSize(BufferSize.SMALL));
		assertState(table, IntegerRange.of(0, 13), IntegerRange.of(0, 4));
		assertEquals(36, helper.visibleCells());
		assertEquals(70, helper.totalCells());
		assertCounter(0, 2, 0, 0, 0, 42, 12);
		assertRowsCounter(0, 0, 0, 0, 2, 0);

		// Big buffer
		robot.interact(() -> table.setBufferSize(BufferSize.BIG));
		assertState(table, IntegerRange.of(0, 17), IntegerRange.of(0, 8));
		assertEquals(36, helper.visibleCells());
		assertEquals(162, helper.totalCells());
		assertCounter(62, 2, 92, 92, 30, 0, 0);
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

		// Medium buffer
		assertState(table, IntegerRange.of(17, 32), IntegerRange.of(4, 10));
		assertEquals(36, helper.visibleCells());
		assertEquals(112, helper.totalCells());
		assertCounter(112, 1, 112, 112, 0, 0, 0);
		assertRowsCounter(16, 16, 16, 0, 0, 0);

		// Small buffer
		robot.interact(() -> table.setBufferSize(BufferSize.SMALL));
		assertState(table, IntegerRange.of(18, 31), IntegerRange.of(5, 9));
		assertEquals(36, helper.visibleCells());
		assertEquals(70, helper.totalCells());
		assertCounter(0, 2, 0, 0, 0, 42, 12);
		assertRowsCounter(0, 0, 0, 0, 2, 0);


		// Big buffer
		robot.interact(() -> table.setBufferSize(BufferSize.BIG));
		assertState(table, IntegerRange.of(16, 33), IntegerRange.of(3, 11));
		assertEquals(36, helper.visibleCells());
		assertEquals(162, helper.totalCells());
		assertCounter(62, 2, 92, 92, 30, 0, 0);
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
		assertCounter(112, 1, 112, 112, 0, 0, 0);
		assertRowsCounter(16, 16, 16, 0, 0, 0);

		// Small buffer
		robot.interact(() -> table.setBufferSize(BufferSize.SMALL));
		assertState(table, IntegerRange.of(36, 49), IntegerRange.of(11, 15));
		assertEquals(36, helper.visibleCells());
		assertEquals(70, helper.totalCells());
		assertCounter(0, 2, 0, 0, 0, 42, 12);
		assertRowsCounter(0, 0, 0, 0, 2, 0);

		// Big buffer
		robot.interact(() -> table.setBufferSize(BufferSize.BIG));
		assertState(table, IntegerRange.of(32, 49), IntegerRange.of(7, 15));
		assertEquals(36, helper.visibleCells());
		assertEquals(162, helper.totalCells());
		assertCounter(62, 2, 92, 92, 30, 0, 0);
		assertRowsCounter(2, 4, 4, 2, 0, 0);
	}

	@Test
	void testChangeRowFactory(FxRobot robot) {
		StackPane pane = setupStage();
		Table table = new Table(users(50))
			.addEmptyColumns(9);
		VFXTableHelper<User> helper = table.getHelper();
		robot.interact(() -> pane.getChildren().add(table));

		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(112, 1, 112, 112, 0, 0, 0);
		assertRowsCounter(16, 16, 16, 0, 0, 0);

		// Test at both pos != 0
		robot.interact(() -> {
			table.setVPos(600.0);
			table.setHPos(1000.0);
		});
		assertState(table, IntegerRange.of(16, 31), IntegerRange.of(3, 9));
		assertCounter(48, 2, 48, 160, 0, 48, 18); // Counter's stats are bigger because we scroll two times
		assertRowsCounter(0, 16, 16, 0, 0, 0);

		// Change row factory and test
		robot.interact(() ->
			table.setRowFactory(u -> new TestRow(u) {
				{
					StyleUtils.setBackground(this, ColorUtils.getRandomColor(0.2));
				}
			})
		);
		assertState(table, IntegerRange.of(16, 31), IntegerRange.of(3, 9));
		assertEquals(36, helper.visibleCells());
		assertEquals(112, helper.totalCells());
		assertCounter(0, 1, 0, 0, 0, 0, 0);
		assertRowsCounter(16, 16, 16, 0, 16, 16);
	}

	@Test
	void testChangeRowFactory2(FxRobot robot) {
		StackPane pane = setupStage();
		Table table = new Table(users(50))
			.addEmptyColumns(9);
		robot.interact(() -> pane.getChildren().add(table));

		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(112, 1, 112, 112, 0, 0, 0);
		assertRowsCounter(16, 16, 16, 0, 0, 0);

		// Test at both pos != 0
		robot.interact(() -> {
			table.setVPos(600.0);
			table.setHPos(1000.0);
		});
		assertState(table, IntegerRange.of(16, 31), IntegerRange.of(3, 9));
		assertCounter(48, 2, 48, 160, 0, 48, 18); // Counter's stats are bigger because we scroll two times
		assertRowsCounter(0, 16, 16, 0, 0, 0);

		// Change row factory to null
		robot.interact(() -> table.setRowFactory(null));
		assertState(table, INVALID_RANGE, IntegerRange.of(3, 9));
		assertCounter(0, 0, 0, 0, 0, 112, 42);
		assertRowsCounter(0, 0, 0, 0, 16, 16);

		robot.interact(() ->
			table.setRowFactory(u -> new TestRow(u) {
				{
					StyleUtils.setBackground(this, ColorUtils.getRandomColor(0.2));
				}
			})
		);
		assertState(table, IntegerRange.of(16, 31), IntegerRange.of(3, 9));
		assertCounter(42, 1, 112, 112, 70, 0, 0);
		assertRowsCounter(16, 16, 16, 0, 0, 0);
	}

	@SuppressWarnings("unchecked") // Fuck Java generics
	@Test
	void testChangeCellFactory(FxRobot robot) {
		StackPane pane = setupStage();
		Table table = new Table(users(50))
			.addEmptyColumns(9);
		robot.interact(() -> pane.getChildren().add(table));

		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(112, 1, 112, 112, 0, 0, 0);
		assertRowsCounter(16, 16, 16, 0, 0, 0);

		// Test at both pos != 0
		robot.interact(() -> {
			table.setVPos(600.0);
			table.setHPos(1000.0);
		});
		assertState(table, IntegerRange.of(16, 31), IntegerRange.of(3, 9));
		assertCounter(48, 2, 48, 160, 0, 48, 18); // Counter's stats are bigger because we scroll two times
		assertRowsCounter(0, 16, 16, 0, 0, 0);

		// Change cell factory of column 5
		robot.interact(() -> {
			VFXTableColumn<User, VFXTableCell<User>> column = (VFXTableColumn<User, VFXTableCell<User>>) table.getColumns().get(5);
			column.setCellFactory(u -> Table.factory(u, User::blood, c -> StyleUtils.setBackground(c, ColorUtils.getRandomColor(0.2))));
		});
		VFXTableState<User> c1State = table.getState();
		assertTrue(c1State.isClone());
		assertState(table, IntegerRange.of(16, 31), IntegerRange.of(3, 9));
		assertCounter(16, 0, 16, 16, 0, 16, 16);
		assertRowsCounter(0, 0, 0, 0, 0, 0);

		// Change cell factory of column 5 to null
		robot.interact(() -> {
			VFXTableColumn<User, VFXTableCell<User>> column = (VFXTableColumn<User, VFXTableCell<User>>) table.getColumns().get(5);
			column.setCellFactory(null);
		});
		assertEquals(c1State, table.getState());
		assertState(table, IntegerRange.of(16, 31), IntegerRange.of(3, 9), 96);
		assertCounter(0, 0, 0, 0, 0, 16, 16);
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

		// Change factory outside range
		// State should not change
		VFXTableState<User> oState = table.getState();
		robot.interact(() -> {
			VFXTableColumn<User, VFXTableCell<User>> column = (VFXTableColumn<User, VFXTableCell<User>>) table.getColumns().getLast();
			column.setCellFactory(u -> Table.factory(u, u1 -> "Outside", c -> StyleUtils.setBackground(c, ColorUtils.getRandomColor(0.2))));
		});
		assertFalse(table.getState().isClone());
		assertSame(oState, table.getState());

		// Cause empty state and try changing cell factory again
		resetCounters(); // Reset counters before since we are not testing them above
		robot.interact(() -> table.setRowsHeight(0.0));
		assertState(table, INVALID_RANGE, IntegerRange.of(3, 9));
		assertCounter(0, 0, 0, 0, 0, 96, 36);
		assertRowsCounter(0, 0, 0, 0, 16, 6);

		// Now change factory, nothing should happen
		robot.interact(() -> {
			VFXTableColumn<User, VFXTableCell<User>> column = (VFXTableColumn<User, VFXTableCell<User>>) table.getColumns().get(5);
			column.setCellFactory(u -> Table.factory(u, User::blood, c -> StyleUtils.setBackground(c, ColorUtils.getRandomColor(0.2))));
		});
		assertState(table, INVALID_RANGE, IntegerRange.of(3, 9));
		assertCounter(0, 0, 0, 0, 0, 0, 0);
		assertRowsCounter(0, 0, 0, 0, 0, 0);
	}

	@Test
	void testChangeCellHeightTopLeft(FxRobot robot) {
		StackPane pane = setupStage();
		Table table = new Table(users(50))
			.addEmptyColumns(9);
		robot.interact(() -> pane.getChildren().add(table));

		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(112, 1, 112, 112, 0, 0, 0);
		assertRowsCounter(16, 16, 16, 0, 0, 0);

		// Decrease and test
		robot.interact(() -> table.setRowsHeight(20.0));
		assertState(table, IntegerRange.of(0, 22), IntegerRange.of(0, 6));
		assertCounter(49, 1, 49, 49, 0, 0, 0);
		assertRowsCounter(7, 7, 7, 0, 0, 0);
		assertLength(table, 50.0 * 20, 16 * 180);

		// Increase and test
		robot.interact(() -> table.setRowsHeight(50));
		assertState(table, IntegerRange.of(0, 11), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 0, 0, 77, 7);
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
		assertCounter(112, 1, 112, 112, 0, 0, 0);
		assertRowsCounter(16, 16, 16, 0, 0, 0);

		// Decrease and test
		robot.interact(() -> table.setRowsHeight(18.0));
		assertEquals(532, table.getVPos());
		assertEquals(1000, table.getHPos());
		assertState(table, IntegerRange.of(25, 49), IntegerRange.of(3, 9));
		assertCounter(63, 1, 63, 126, 0, 0, 0);
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
		assertCounter(0, 1, 0, 98, 0, 77, 7);
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
		assertCounter(112, 1, 112, 112, 0, 0, 0);
		assertRowsCounter(16, 16, 16, 0, 0, 0);

		// Check positions!!!
		assertEquals(1232, table.getVPos());
		assertEquals(2480, table.getHPos());

		// Decrease and test
		robot.interact(() -> table.setRowsHeight(25.0));
		assertEquals(882, table.getVPos());
		assertEquals(2480, table.getHPos());
		assertState(table, IntegerRange.of(31, 49), IntegerRange.of(9, 15));
		assertCounter(21, 1, 21, 21, 0, 0, 0);
		assertRowsCounter(3, 3, 3, 0, 0, 0);

		// Increase and test
		robot.interact(() -> table.setRowsHeight(44.0));
		assertEquals(882, table.getVPos());
		assertEquals(2480, table.getHPos());
		assertState(table, IntegerRange.of(18, 30), IntegerRange.of(9, 15));
		assertCounter(0, 1, 0, 91, 0, 42, 0);
		assertRowsCounter(0, 13, 13, 0, 6, 0);
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
		assertCounter(105, 1, 105, 105, 0, 0, 0);
		assertRowsCounter(15, 15, 15, 0, 0, 0);

		// Decrease and test
		robot.interact(() -> table.setColumnsSize(100, 24));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 7));
		assertCounter(23, 1, 23, 23, 0, 0, 0);
		assertRowsCounter(1, 1, 1, 0, 0, 0);

		// Increase and test
		robot.interact(() -> table.setColumnsSize(240, 100));
		assertState(table, IntegerRange.of(0, 13), IntegerRange.of(0, 5));
		assertCounter(0, 1, 0, 0, 0, 44, 12);
		assertRowsCounter(0, 0, 0, 0, 2, 0);

		// Change, do not cause any actual change
		robot.interact(() -> table.setColumnsSize(230, 90));
		assertState(table, IntegerRange.of(0, 13), IntegerRange.of(0, 5));
		assertCounter(0, 1, 0, 0, 0, 0, 0);
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
		assertCounter(105, 1, 105, 105, 0, 0, 0);
		assertRowsCounter(15, 15, 15, 0, 0, 0);
		assertScrollable(table, 50 * 32 - 400 + 52, 180 * 16 - 400);

		// Decrease and test
		robot.interact(() -> table.setColumnsSize(100, 24));
		assertEquals(600.0, table.getVPos());
		assertEquals(1000.0, table.getHPos());
		assertState(table, IntegerRange.of(16, 31), IntegerRange.of(8, 15));
		assertCounter(98, 1, 98, 98, 0, 75, 25);
		assertRowsCounter(1, 1, 1, 0, 0, 0);
		assertScrollable(table, 50 * 32 - 400 + 24, 100 * 16 - 400);
		// Total cells needed 16 rows * 8 columns = 128
		// 2 columns in common. 16 rows * 6 columns = 96 new cells
		// 1 new row, which means that those 2 columns in common still need 2 new cells
		// Total new cells = 96 + 2 = 98 updates (128 total cells - 32 in common + 2 new from commons)

		// Increase and test
		robot.interact(() -> table.setColumnsSize(240, 100));
		assertEquals(600.0, table.getVPos());
		assertEquals(1000.0, table.getHPos());
		assertState(table, IntegerRange.of(16, 29), IntegerRange.of(2, 7));
		assertCounter(34, 1, 84, 84, 50, 128, 48);
		assertRowsCounter(0, 0, 0, 0, 2, 0);
		assertScrollable(table, 50 * 32 - 400 + 100, 240 * 16 - 400);
		// Total cells needed 14 rows * 6 columns = 84 cells
		// Columns [3, 7] were already shown before, and some of their cells are now in cache (cache is full)
		// Created cells = 84 - (5 * 10 from each cache) = 34 new cells
		// Both new and de-cached cells need to be updated though, so we have 84 updates anyway
		// There are no columns in common with the previous range which means that cells for columns [8, 15] are all cached
		// 128 in cache, 16 for each column, max capacity is 10 so, 6 disposals for each column; 6 * 8 = 48 cells disposed

		// Change, do not cause any actual change
		robot.interact(() -> table.setColumnsSize(230, 90));
		assertEquals(600.0, table.getVPos());
		assertEquals(1000.0, table.getHPos());
		assertState(table, IntegerRange.of(16, 29), IntegerRange.of(2, 7));
		assertCounter(0, 1, 0, 0, 0, 0, 0);
		assertRowsCounter(0, 0, 0, 0, 0, 0);
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
		assertCounter(105, 1, 105, 105, 0, 0, 0);
		assertRowsCounter(15, 15, 15, 0, 0, 0);
		assertScrollable(table, 50 * 32 - 400 + 52, 180 * 16 - 400);

		// Decrease and test
		robot.interact(() -> table.setColumnsSize(100, 24));
		assertEquals(1224.0, table.getVPos());
		assertEquals(1200.0, table.getHPos());
		assertState(table, IntegerRange.of(34, 49), IntegerRange.of(8, 15));
		assertCounter(23, 1, 23, 23, 0, 0, 0);
		assertRowsCounter(1, 1, 1, 0, 0, 0);
		assertScrollable(table, 50 * 32 - 400 + 24, 100 * 16 - 400);

		// Increase and test
		robot.interact(() -> table.setColumnsSize(240, 100));
		assertEquals(1224.0, table.getVPos());
		assertEquals(1200.0, table.getHPos());
		assertState(table, IntegerRange.of(36, 49), IntegerRange.of(3, 8));
		assertCounter(70, 1, 70, 70, 0, 114, 42);
		assertRowsCounter(0, 0, 0, 0, 2, 0);
		assertScrollable(table, 50 * 32 - 400 + 100, 240 * 16 - 400);

		// Change, do not cause any actual change
		robot.interact(() -> table.setColumnsSize(230, 90));
		assertEquals(1224.0, table.getVPos());
		assertEquals(1200.0, table.getHPos());
		assertState(table, IntegerRange.of(36, 49), IntegerRange.of(3, 8));
		assertCounter(0, 1, 0, 0, 0, 0, 0);
		assertRowsCounter(0, 0, 0, 0, 0, 0);
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
		assertCounter(105, 1, 105, 105, 0, 0, 0);
		assertRowsCounter(15, 15, 15, 0, 0, 0);

		// Decrease height
		robot.interact(() -> table.setColumnsHeight(24));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(7, 1, 7, 7, 0, 0, 0);
		assertRowsCounter(1, 1, 1, 0, 0, 0);

		// Decrease width
		robot.interact(() -> table.setColumnsWidth(100));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 7));
		assertCounter(16, 1, 16, 16, 0, 0, 0);
		assertRowsCounter(0, 0, 0, 0, 0, 0);

		// Increase height
		robot.interact(() -> table.setColumnsHeight(100));
		assertState(table, IntegerRange.of(0, 13), IntegerRange.of(0, 7));
		assertCounter(0, 1, 0, 0, 0, 16, 0);
		assertRowsCounter(0, 0, 0, 0, 2, 0);

		// Increase width
		robot.interact(() -> table.setColumnsWidth(240));
		assertState(table, IntegerRange.of(0, 13), IntegerRange.of(0, 5));
		assertCounter(0, 1, 0, 0, 0, 28, 12);
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
		assertCounter(0, 0, 0, 0, 0, 0, 0);
		assertRowsCounter(0, 0, 0, 0, 0, 0);

		// Decrease height
		robot.interact(() -> table.setColumnsHeight(24));
		assertState(table, INVALID_RANGE, IntegerRange.of(0, 6), 0);
		assertCounter(0, 0, 0, 0, 0, 0, 0);
		assertRowsCounter(0, 0, 0, 0, 0, 0);

		// Decrease width
		robot.interact(() -> table.setColumnsWidth(100));
		assertState(table, INVALID_RANGE, IntegerRange.of(0, 7), 0);
		assertCounter(0, 0, 0, 0, 0, 0, 0);
		assertRowsCounter(0, 0, 0, 0, 0, 0);

		// Increase height
		robot.interact(() -> table.setColumnsHeight(100));
		assertState(table, INVALID_RANGE, IntegerRange.of(0, 7), 0);
		assertCounter(0, 0, 0, 0, 0, 0, 0);
		assertRowsCounter(0, 0, 0, 0, 0, 0);

		// Increase width
		robot.interact(() -> table.setColumnsWidth(240));
		assertState(table, INVALID_RANGE, IntegerRange.of(0, 5), 0);
		assertCounter(0, 0, 0, 0, 0, 0, 0);
		assertRowsCounter(0, 0, 0, 0, 0, 0);

		// See Table class for why there are no layouts
	}

	@Test
	void testColumnsPermutation(FxRobot robot) {
		StackPane pane = setupStage();
		Table table = new Table(users(20))
			.addEmptyColumns(9);
		robot.interact(() -> pane.getChildren().add(table));

		// Assert init
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(112, 1, 112, 112, 0, 0, 0);

		// Permutation change
		robot.interact(() -> FXCollections.sort(table.getColumns(), Collections.reverseOrder()));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(112, 1, 112, 112, 0, 112, 42);
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
		assertCounter(112, 1, 112, 112, 0, 0, 0);

		// Permutation change
		robot.interact(() -> FXCollections.sort(table.getColumns(), Collections.reverseOrder()));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(3, 9));
		assertCounter(48, 1, 112, 48, 0, 48, 18);
	}

	@Test
	void testSetColumns(FxRobot robot) {
		StackPane pane = setupStage();
		Table table = new Table(users(20))
			.addEmptyColumns(9);
		robot.interact(() -> pane.getChildren().add(table));

		// Assert init
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(112, 1, 112, 112, 0, 0, 0);
		assertRowsCounter(16, 16, 16, 0, 0, 0);

		// Set all (more)
		robot.interact(() -> table.getColumns().setAll(emptyColumns("Set", 15)));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(112, 1, 112, 112, 0, 112, 42);

		// Set all (less)
		robot.interact(() -> table.getColumns().setAll(emptyColumns("Set", 5)));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 4));
		assertCounter(80, 1, 80, 80, 0, 112, 42);

		// Restore old count
		// Go to last and set all (more)
		robot.interact(() -> {
			table.getColumns().setAll(emptyColumns(16));
			table.scrollToLastColumn();
		});
		resetCounters();
		robot.interact(() -> table.getColumns().setAll(emptyColumns("Set", 30)));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(11, 17));
		assertCounter(112, 1, 112, 112, 0, 112, 42);

		// Set random columns
		robot.interact(() -> {
			IntegerRange columnsRange = table.getState().getColumnsRange();
			for (int i = 0; i < 5; i++) {
				int index = RandomUtils.random.nextInt(columnsRange.getMin(), columnsRange.getMax() + 1);
				table.getColumns().set(index, new EmptyColumn("Random", 999 - i));
			}
		});
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(11, 17));
		assertCounter(80, 5, 560, 80, 0, 80, 30);
		// Every time a change occurs, all the cells have their index updated.
		// This is because their parent column may be in a different position after the change.
		// This should be improved on the cell implementation side with a basic check.
		// There are a total of 112 cells in the viewport, for 5 changes... 112 * 5 = 560
		// Also remember... cells here CANNOT be reused as every column produces its own kind
	}

	@Test
	void testAddColumnsAt0(FxRobot robot) {
		StackPane pane = setupStage();
		Table table = new Table(users(20))
			.addEmptyColumns(9);
		robot.interact(() -> pane.getChildren().add(table));

		// Assert init
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(112, 1, 112, 112, 0, 0, 0);
		assertRowsCounter(16, 16, 16, 0, 0, 0);

		// Add all at 0
		robot.interact(() -> table.getColumns().addAll(0, emptyColumns("Added", 4)));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(64, 1, 112, 64, 0, 64, 24);

		// Add at 0 (for)
		robot.interact(() -> {
			for (int i = 0; i < 4; i++) table.getColumns().addFirst(new EmptyColumn("Add for", 999 - i));
		});
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(64, 4, 448, 64, 0, 64, 24);
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
		assertCounter(112, 1, 112, 112, 0, 0, 0);
		assertRowsCounter(16, 16, 16, 0, 0, 0);

		// Add before no intersect
		robot.interact(() -> table.getColumns().addAll(0, emptyColumns("Add bni", 2)));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(3, 9));
		assertCounter(32, 1, 112, 32, 0, 32, 12);

		// Add before intersect
		robot.interact(() -> table.getColumns().addAll(2, emptyColumns("Add bi", 3)));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(3, 9));
		assertCounter(48, 1, 112, 48, 0, 48, 18);

		// Add after intersect
		robot.interact(() -> table.getColumns().addAll(9, emptyColumns("Add ai", 2)));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(3, 9));
		assertCounter(16, 1, 112, 16, 0, 16, 6);

		// Add after no intersect
		robot.interact(() -> table.getColumns().addAll(10, emptyColumns("Add ani", 2)));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(3, 9));
		assertCounter(0, 1, 112, 0, 0, 0, 0);
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
		assertCounter(112, 1, 112, 112, 0, 0, 0);
		assertRowsCounter(16, 16, 16, 0, 0, 0);

		// Add all at end
		robot.interact(() -> table.getColumns().addAll(emptyColumns("End", 2)));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(11, 17));
		assertCounter(32, 1, 112, 32, 0, 32, 12);

		// Add all at end (for)
		robot.interact(() -> {
			for (int i = 0; i < 2; i++)
				table.getColumns().add(new EmptyColumn("EndFor", 999 - i));
		});
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(11, 17));
		assertCounter(0, 2, 224, 0, 0, 0, 0);

		// Add before no intersect
		robot.interact(() -> table.getColumns().addAll(0, emptyColumns("Add bni", 2)));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(11, 17));
		assertCounter(12, 1, 112, 32, 20, 32, 12);

		// Add before intersect
		robot.interact(() -> table.getColumns().addAll(10, emptyColumns("Add bi", 3)));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(11, 17));
		assertCounter(48, 1, 112, 48, 0, 48, 18);
	}

	@Test
	void testRemoveColumnsAt0(FxRobot robot) {
		StackPane pane = setupStage();
		Table table = new Table(users(20))
			.addEmptyColumns(9);
		robot.interact(() -> pane.getChildren().add(table));

		// Assert init
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(112, 1, 112, 112, 0, 0, 0);
		assertRowsCounter(16, 16, 16, 0, 0, 0);

		// Remove all at 0
		robot.interact(() -> Utils.removeAll(table.getColumns(), 0, 1, 2));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(48, 1, 112, 48, 0, 48, 18);

		// Remove at 0 (for)
		robot.interact(() -> {
			for (int i = 0; i < 4; i++) table.getColumns().removeFirst();
		});
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(64, 4, 448, 64, 0, 64, 24);

		// Clear and assert INVALID state
		robot.interact(() -> table.getColumns().clear());
		assertState(table, INVALID_RANGE, INVALID_RANGE);
		assertCounter(0, 0, 0, 0, 0, 112, 42);
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
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(6, 12));
		assertCounter(112, 1, 112, 112, 0, 0, 0);
		assertRowsCounter(16, 16, 16, 0, 0, 0);

		// Remove before no intersect
		robot.interact(() -> Utils.removeAll(table.getColumns(), 0, 1));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(6, 12));
		assertCounter(32, 1, 112, 32, 0, 32, 12);

		// Remove before intersect
		robot.interact(() -> Utils.removeAll(table.getColumns(), 5, 6));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(6, 12));
		assertCounter(32, 1, 112, 32, 0, 32, 12);

		// Remove after intersect
		robot.interact(() -> Utils.removeAll(table.getColumns(), 12, 13));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(6, 12));
		assertCounter(16, 1, 112, 16, 0, 16, 6);

		// Remove after no intersect
		robot.interact(() -> Utils.removeAll(table.getColumns(), 13, 14));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(6, 12));
		assertCounter(0, 1, 112, 0, 0, 0, 0);

		// Clear and assert INVALID state
		robot.interact(() -> table.getColumns().clear());
		assertState(table, INVALID_RANGE, INVALID_RANGE);
		assertCounter(0, 0, 0, 0, 0, 112, 42);
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
		assertCounter(112, 1, 112, 112, 0, 0, 0);
		assertRowsCounter(16, 16, 16, 0, 0, 0);

		// Remove all at end
		robot.interact(() -> Utils.removeAll(table.getColumns(), 19, 20, 21));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(12, 18));
		assertCounter(48, 1, 112, 48, 0, 48, 18);

		// Add all at end (for)
		robot.interact(() -> {
			for (int i = 0; i < 2; i++) table.getColumns().removeLast();
		});
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(10, 16));
		assertCounter(32, 2, 224, 32, 0, 32, 12);

		// Remove before no intersect
		robot.interact(() -> Utils.removeAll(table.getColumns(), 0, 1));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(8, 14));
		assertCounter(0, 1, 112, 0, 0, 0, 0);

		// Remove before intersect
		robot.interact(() -> Utils.removeAll(table.getColumns(), 6, 7, 8));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(5, 11));
		assertCounter(16, 1, 112, 16, 0, 16, 6);

		// Clear and assert INVALID state
		robot.interact(() -> table.getColumns().clear());
		assertState(table, INVALID_RANGE, INVALID_RANGE);
		assertCounter(0, 0, 0, 0, 0, 112, 42);
		assertEquals(0.0, table.getHPos());
	}

	@Test
	void testChangeItemsList(FxRobot robot) {
		StackPane pane = setupStage();
		Table table = new Table(users(50));
		robot.interact(() -> pane.getChildren().add(table));

		// Assert init
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(112, 1, 112, 112, 0, 0, 0);
		assertRowsCounter(16, 16, 16, 0, 0, 0);

		// Change items property
		robot.interact(() -> table.setItems(users(50)));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 112, 0, 0, 0);
		assertRowsCounter(0, 16, 16, 0, 0, 0);

		// Change items property (fewer elements)
		robot.interact(() -> table.setItems(users(10)));
		assertState(table, IntegerRange.of(0, 9), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 70, 0, 42, 0);
		assertRowsCounter(0, 10, 10, 0, 6, 0);

		// Change items property (more elements)
		robot.interact(() -> table.setItems(users(50)));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(0, 1, 42, 112, 42, 0, 0);
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
		assertCounter(0, 1, 0, 105, 0, 7, 0);
		assertRowsCounter(0, 15, 15, 0, 1, 0);

		// Fill the viewport, then scroll to max again and set more
		robot.interact(() -> {
			table.setItems(users(25));
			table.scrollToLastRow();
			resetCounters();
			table.setItems(users(40));
		});
		assertState(table, IntegerRange.of(11, 26), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 112, 0, 0, 0);
		assertRowsCounter(0, 16, 16, 0, 0, 0);

		// Change items to empty
		robot.interact(() -> table.setItems(null));
		assertState(table, INVALID_RANGE, IntegerRange.of(0, 6));
		assertCounter(0, 0, 0, 0, 0, 112, 42);
		assertRowsCounter(0, 0, 0, 0, 16, 6);
	}

	@Test
	void testItemsPermutation(FxRobot robot) {
		StackPane pane = setupStage();
		Table table = new Table(users(50));
		robot.interact(() -> pane.getChildren().add(table));

		// Assert init
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(112, 1, 112, 112, 0, 0, 0);
		assertRowsCounter(16, 16, 16, 0, 0, 0);

		// Permutation change
		robot.interact(() -> FXCollections.sort(table.getItems(), Comparator.comparing(User::id).reversed()));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 112, 0, 0, 0);
		assertRowsCounter(0, 16, 16, 0, 0, 0);
	}

	@Test
	void testSetItems(FxRobot robot) {
		StackPane pane = setupStage();
		Table table = new Table(users(50));
		robot.interact(() -> pane.getChildren().add(table));

		// Assert init
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(112, 1, 112, 112, 0, 0, 0);
		assertRowsCounter(16, 16, 16, 0, 0, 0);

		// Set all (more)
		robot.interact(() -> table.getItems().setAll(users(80)));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 112, 0, 0, 0);
		assertRowsCounter(0, 16, 16, 0, 0, 0);

		// Set all (less)
		robot.interact(() -> table.getItems().setAll(users(10)));
		assertState(table, IntegerRange.of(0, 9), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 70, 0, 42, 0);
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
		assertCounter(0, 1, 0, 14, 0, 0, 0);
		assertRowsCounter(0, 16, 2, 0, 0, 0);

		// Set random items
		robot.interact(() -> {
			for (int i = 0; i < 5; i++) {
				int index = RandomUtils.random.nextInt(26, 42);
				table.getItems().set(index, new User());
			}
		});
		assertState(table, IntegerRange.of(26, 41), IntegerRange.of(0, 6));
		assertCounter(0, 5, 0, 35, 0, 0, 0);
		assertRowsCounter(0, 80, 5, 0, 0, 0);
	}

	@Test
	void testAddItemsAt0(FxRobot robot) {
		StackPane pane = setupStage();
		Table table = new Table(users(50));
		robot.interact(() -> pane.getChildren().add(table));

		// Assert init
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(112, 1, 112, 112, 0, 0, 0);
		assertRowsCounter(16, 16, 16, 0, 0, 0);

		// Add all at 0
		robot.interact(() -> table.getItems().addAll(0, users(4)));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 28, 0, 0, 0);
		assertRowsCounter(0, 16, 4, 0, 0, 0);

		// Add at 0 (for)
		robot.interact(() -> {
			for (int i = 0; i < 3; i++) table.getItems().addFirst(new User());
		});
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(0, 3, 0, 21, 0, 0, 0);
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
		assertCounter(112, 1, 112, 112, 0, 0, 0);
		assertRowsCounter(16, 16, 16, 0, 0, 0);

		// Add before no intersect
		robot.interact(() -> table.getItems().addAll(0, users(3)));
		assertState(table, IntegerRange.of(16, 31), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 21, 0, 0, 0);
		assertRowsCounter(0, 16, 3, 0, 0, 0);

		// Add before intersect
		robot.interact(() -> table.getItems().addAll(14, users(4)));
		assertState(table, IntegerRange.of(16, 31), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 28, 0, 0, 0);
		assertRowsCounter(0, 16, 4, 0, 0, 0);

		// Add after intersect
		robot.interact(() -> table.getItems().addAll(29, users(4)));
		assertState(table, IntegerRange.of(16, 31), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 21, 0, 0, 0);
		assertRowsCounter(0, 16, 3, 0, 0, 0);

		// Add after no intersect
		robot.interact(() -> table.getItems().addAll(users(2)));
		assertState(table, IntegerRange.of(16, 31), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 0, 0, 0, 0);
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
		assertCounter(112, 1, 112, 112, 0, 0, 0);
		assertRowsCounter(16, 16, 16, 0, 0, 0);

		// Add only one at end
		robot.interact(() -> table.getItems().add(new User()));
		assertState(table, IntegerRange.of(35, 50), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 7, 0, 0, 0);
		assertRowsCounter(0, 16, 1, 0, 0, 0);

		// Add all at end (for)
		robot.interact(() -> {
			for (int i = 0; i < 3; i++) table.getItems().add(new User());
		});
		assertState(table, IntegerRange.of(36, 51), IntegerRange.of(0, 6));
		assertCounter(0, 3, 0, 7, 0, 0, 0);
		assertRowsCounter(0, 48, 1, 0, 0, 0);

		// Add before no intersect
		robot.interact(() -> table.getItems().addAll(0, users(2)));
		assertState(table, IntegerRange.of(36, 51), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 14, 0, 0, 0);
		assertRowsCounter(0, 16, 2, 0, 0, 0);

		// Add before intersect
		robot.interact(() -> table.getItems().addAll(34, users(4)));
		assertState(table, IntegerRange.of(36, 51), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 28, 0, 0, 0);
		assertRowsCounter(0, 16, 4, 0, 0, 0);
	}

	@Test
	void testRemoteItemsAt0(FxRobot robot) {
		StackPane pane = setupStage();
		Table table = new Table(users(50));
		robot.interact(() -> pane.getChildren().add(table));

		// Assert init
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(112, 1, 112, 112, 0, 0, 0);
		assertRowsCounter(16, 16, 16, 0, 0, 0);

		// Remove all at 0
		robot.interact(() -> Utils.removeAll(table, 0, 1, 2, 3));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 28, 0, 0, 0);
		assertRowsCounter(0, 16, 4, 0, 0, 0);

		// Remove at 0 (for)
		robot.interact(() -> {
			for (int i = 0; i < 4; i++) table.getItems().removeFirst();
		});
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(0, 4, 0, 28, 0, 0, 0);
		assertRowsCounter(0, 64, 4, 0, 0, 0);

		// Remove until cannot fill viewport
		robot.interact(() -> Utils.removeAll(table, IntegerRange.of(0, 31)));
		assertEquals(10, table.size());
		assertState(table, IntegerRange.of(0, 9), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 70, 0, 42, 0);
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
		assertCounter(112, 1, 112, 112, 0, 0, 0);
		assertRowsCounter(16, 16, 16, 0, 0, 0);

		// Remove before no intersect
		robot.interact(() -> Utils.removeAll(table, 0, 1));
		assertState(table, IntegerRange.of(16, 31), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 14, 0, 0, 0);
		assertRowsCounter(0, 16, 2, 0, 0, 0);

		// Remove before intersect
		robot.interact(() -> Utils.removeAll(table, 14, 15, 16, 17));
		assertState(table, IntegerRange.of(16, 31), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 28, 0, 0, 0);
		assertRowsCounter(0, 16, 4, 0, 0, 0);

		// Remove after intersect
		robot.interact(() -> Utils.removeAll(table, 31, 32, 33));
		assertState(table, IntegerRange.of(16, 31), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 7, 0, 0, 0);
		assertRowsCounter(0, 16, 1, 0, 0, 0);

		// Remove after no intersect
		robot.interact(() -> Utils.removeAll(table, 32, 33));
		assertState(table, IntegerRange.of(16, 31), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 0, 0, 0, 0);
		assertRowsCounter(0, 16, 0, 0, 0, 0);

		// Remove enough to change vPos and range
		robot.interact(() -> Utils.removeAll(table, IntegerRange.of(0, 18)));
		assertEquals(20, table.size());
		assertEquals(272, table.getVPos());
		assertState(table, IntegerRange.of(4, 19), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 49, 0, 0, 0);
		assertRowsCounter(0, 16, 7, 0, 0, 0);

		// Do it again but this time from bottom
		robot.interact(() -> Utils.removeAll(table, 18, 19));
		assertEquals(18, table.size());
		assertEquals(208, table.getVPos());
		assertState(table, IntegerRange.of(2, 17), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 14, 0, 0, 0);
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
		assertCounter(112, 1, 112, 112, 0, 0, 0);
		assertRowsCounter(16, 16, 16, 0, 0, 0);

		// Remove all at end
		robot.interact(() -> Utils.removeAll(table, 46, 47, 48, 49));
		assertEquals(1104.0, table.getVPos());
		assertState(table, IntegerRange.of(30, 45), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 28, 0, 0, 0);
		assertRowsCounter(0, 16, 4, 0, 0, 0);

		// Remove at end (for)
		robot.interact(() -> {
			for (int i = 0; i < 4; i++) table.getItems().removeLast();
		});
		assertEquals(976.0, table.getVPos());
		assertState(table, IntegerRange.of(26, 41), IntegerRange.of(0, 6));
		assertCounter(0, 4, 0, 28, 0, 0, 0);
		assertRowsCounter(0, 64, 4, 0, 0, 0);

		// Remove before no intersect
		robot.interact(() -> Utils.removeAll(table, 0, 1));
		assertEquals(912.0, table.getVPos());
		assertState(table, IntegerRange.of(24, 39), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 0, 0, 0, 0);
		assertRowsCounter(0, 16, 0, 0, 0, 0);

		// Remove before intersect
		robot.interact(() -> Utils.removeAll(table, 22, 23, 24, 25));
		assertEquals(784.0, table.getVPos());
		assertState(table, IntegerRange.of(20, 35), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 14, 0, 0, 0);
		assertRowsCounter(0, 16, 2, 0, 0, 0);

		// Remove enough to cache cells
		robot.interact(() -> Utils.removeAll(table, IntegerRange.of(0, 21)));
		assertEquals(80.0, table.getVPos());
		assertState(table, IntegerRange.of(0, 13), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 0, 0, 14, 0);
		assertRowsCounter(0, 14, 0, 0, 2, 0);
	}

	@Test
	void testRemoveItemsSparse(FxRobot robot) {
		StackPane pane = setupStage();
		Table table = new Table(users(50));
		robot.interact(() -> pane.getChildren().add(table));

		// Assert init
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(112, 1, 112, 112, 0, 0, 0);
		assertRowsCounter(16, 16, 16, 0, 0, 0);

		robot.interact(() -> Utils.removeAll(table, 0, 3, 4, 8, 10, 11));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(0, 1, 0, 42, 0, 0, 0);
		assertRowsCounter(0, 16, 6, 0, 0, 0);
	}

	@Test
	void testVariableMode(FxRobot robot) {
		// Don't worry about the large layouts numbers, those are 'partial' layouts and are much lighter than full layouts
		// Only some cells are laid out, the rest become hidden and ignored
		StackPane pane = setupStage();
		Table table = new Table(users(50))
			.addEmptyColumns(3);
		robot.interact(() -> pane.getChildren().add(table));

		// Assert init
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(112, 1, 112, 112, 0, 0, 0);
		assertRowsCounter(16, 16, 16, 0, 0, 0);
		assertLength(table, 50 * 32, 10 * 180);

		// Switch mode
		robot.interact(table::switchColumnsLayoutMode);
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 9));
		assertCounter(48, 0, 48, 48, 0, 0, 0);
		assertLength(table, 50 * 32, 10 * 180);
		assertEquals(48, table.getHelper().visibleCells());

		// Increase width of column to random value
		int w = RandomUtils.random.nextInt((int) table.getColumnsSize().getWidth() + 1, 300);
		robot.interact(() -> setColumnWidth(table, 6, w));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 9));
		assertCounter(0, 0, 0, 0, 0, 0, 0); // 0 layouts because none of the columns from 6 are in the viewport
		assertLength(table, 50 * 32, (10 * 180) - 180 + w);

		// Increase width of column (in viewport) to random value
		int w2 = RandomUtils.random.nextInt((int) table.getColumnsSize().getWidth() + 1, 200);
		robot.interact(() -> setColumnWidth(table, 1, w2));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 9));
		assertCounter(0, 32, 0, 0, 0, 0, 0);
		assertLength(table, 50 * 32, (10 * 180) - 360 + w + w2);

		// Decrease below minimum
		robot.interact(() -> setColumnWidth(table, 6, 100));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 9));
		assertCounter(0, 0, 0, 0, 0, 0, 0); // 0 layouts because none of the columns from 6 are in the viewport
		assertLength(table, 50 * 32, (10 * 180) - 180 + w2);

		// Decrease below the minimum (in viewport)
		robot.interact(() -> setColumnWidth(table, 1, 100));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 9));
		assertCounter(0, 32, 0, 0, 0, 0, 0); // 32 layouts because only columns 1 and 2 are in the viewport
		assertLength(table, 50 * 32, 10 * 180);

		// Increase table's width and test the last column
		robot.interact(() -> {
			setWindowPos(table, 0, Double.NaN);
			setWindowSize(table, 1920, -1);
		});
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 9));
		assertCounter(0, 112, 0, 0, 0, 0, 0);
		assertEquals(300, table.getColumns().getLast().getWidth());
		// Columns from 3 to 9 are now in the viewport and need to lay out

		// Decrease table's width and test the last column
		robot.interact(() -> setWindowSize(table, 1840, -1));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 9));
		assertCounter(0, 16, 0, 0, 0, 0, 0);
		assertLength(table, 50 * 32, 1840);
		assertEquals(220, table.getColumns().getLast().getWidth());

		// Now increase the last column's width
		// Window size is still at 1840
		robot.interact(() -> table.getColumns().getLast().resize(250));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 9));
		assertCounter(0, 16, 0, 0, 0, 0, 0);
		assertLength(table, 50 * 32, 1870);
		assertEquals(250, table.getColumns().getLast().getWidth(), 1); // Fucking scaling settings may make the tests fail for no real reason

		// Increase window size
		// Column is now at 300
		robot.interact(() -> setWindowSize(table, 1920, -1));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 9));
		assertCounter(0, 16, 0, 0, 0, 0, 0);
		assertLength(table, 50 * 32, 1920);
		assertEquals(300, table.getColumns().getLast().getWidth());

		// Decrease by a lot
		robot.interact(() -> setWindowSize(table, 720, -1));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 9));
		assertCounter(0, 0, 0, 0, 0, 0, 0);
		assertLength(table, 50 * 32, 1870);
		// 0 layouts because positions and visibility are valid

		// We must do two checks here
		// There is the actual width that is 300.0
		// And the supposed width which is 250.0
		// The thing is, the actual width does not change because of the "partial layout" mechanism.
		// Since the last column is not visible anymore, it is not updated, thus it still has the old width
		VFXTableColumn<User, ? extends VFXTableCell<User>> last = table.getColumns().getLast();
		assertEquals(300, last.getWidth());
		assertEquals(250, table.getHelper().getColumnWidth(last), 1); // Fucking scaling settings may make the tests fail for no real reason
	}

	@Test
	void testVariableModeLC(FxRobot robot) { // LC -> List changes
		StackPane pane = setupStage();
		Table table = new Table(users(50))
			.addEmptyColumns(3);
		robot.interact(() -> {
			// Switch mode
			table.switchColumnsLayoutMode();
			// Resize every column at 200
			table.getColumns().forEach(c -> c.resize(200));
			// Set window size and pos
			setWindowPos(pane, 0, Double.NaN);
			setWindowSize(pane, 1920, -1);
			pane.getChildren().add(table);
		});

		// Assert init
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 9));
		assertCounter(160, 160, 160, 160, 0, 0, 0);
		assertRowsCounter(16, 16, 16, 0, 0, 0);
		assertLength(table, 50 * 32, 10 * 200);
		// 160. All invalid because of the cumulative changes made above

		// Remove random column
		int rIdx = RandomUtils.random.nextInt(0, table.getColumns().size());
		robot.interact(() -> table.getColumns().remove(rIdx));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 8));
		assertCounter(0, (rIdx == 9) ? 16 : (9 - rIdx) * 16, 144, 0, 0, 16, 6);
		assertLength(table, 50 * 32, 1920); // 1920 because the windows' width is 1920px
		// Layouts will never be 0 because removing the last column means that the new last column may need to be resized

		// Add columns at random pos, set width to 250
		int rIdx2 = RandomUtils.random.nextInt(0, table.getColumns().size());
		robot.interact(() -> {
			for (int i = 0; i < 2; i++) {
				EmptyColumn c = new EmptyColumn("Rand " + i, i);
				c.resize(250);
				table.getColumns().add(rIdx2, c);
			}
			resetCounters(); // Irrelevant because it will be a full layout anyway
		});
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 10));
		assertLength(table, 50 * 32, (9 * 200) + (2 * 250));

		// Swap two columns
		robot.interact(() -> swapColumns(table, 3, 6));
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 10));
		assertTrue(IntegerRange.inRangeOf(counter.getLayoutCnt(), IntegerRange.of(32, 64)));
		assertEquals(176, counter.getUpdIndexCnt());
		assertLength(table, 50 * 32, (9 * 200) + (2 * 250));
		resetCounters();
		// Layouts can vary between 32 and 64 because we swap columns at indexes 3 and 6.
		// Which means: worst case scenario I have to lay out again all columns 3, 4, 5, and 6

		// Swap two columns (last)
		double wBefore = table.getColumns().getLast().getWidth();
		robot.interact(() -> {
			// Change columns width before
			table.setColumnsWidth(150);
			for (VFXTableColumn<User, ? extends VFXTableCell<User>> c : table.getColumns()) {
				if (c.getWidth() == 250.0) continue;
				c.resize(-1);
			}
			resetCounters(); // Not relevant

			swapColumns(table, 0, table.getColumns().size() - 1);
		});
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 10));
		assertLength(table, 50 * 32, 1920);
		assertTrue(table.getColumns().getFirst().getWidth() < wBefore);
		System.out.println(table.getColumns().getLast().getWidth());
		assertTrue(table.getColumns().getLast().getWidth() >= 215); // Fucking scaling settings may make the tests fail for no real reason

		// Clear
		robot.interact(() -> table.getColumns().clear());
		resetCounters(); // Not relevant
		assertState(table, INVALID_RANGE, INVALID_RANGE);
		assertLength(table, 0, 0);
	}

	@Test
	void testVariableModeSwitchBack(FxRobot robot) {
		StackPane pane = setupStage();
		Table table = new Table(users(50))
			.addEmptyColumns(3);
		robot.interact(() -> {
			// Randomize widths
			table.getColumns().forEach(c -> c.resize(RandomUtils.random.nextInt(
				(int) (table.getColumnsSize().getWidth() + 1),
				240
			)));
			table.setHPos(table.getMaxHScroll() / 2.0);
			pane.getChildren().add(table);
		});

		robot.interact(table::switchColumnsLayoutMode);

		// Assert init
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 9));

		// Switch back
		robot.interact(table::switchColumnsLayoutMode);
		assertState(table, IntegerRange.of(0, 15), table.getHelper().columnsRange());

		// Scroll to max and check again
		robot.interact(table::scrollToLastColumn);
		assertState(table, IntegerRange.of(0, 15), table.getHelper().columnsRange());
	}

	@Test
	void testAutosizeVariable(FxRobot robot) {
		StackPane pane = setupStage();
		Table table = new Table(FXCollections.observableArrayList(AUTOSIZE_USERS));
		robot.interact(() -> {
			// Increase window size and change pos
			setWindowPos(pane, 0, Double.NaN);
			setWindowSize(pane, 1200, -1);

			// Decrease minimum size
			table.setColumnsWidth(80.0);

			// Switch mode
			table.switchColumnsLayoutMode();

			// Issue autosize here to also test the "delay" functionality
			table.autosizeColumns();
			pane.getChildren().add(table);
		});

		double[] vals = new double[]{
			239.0,
			96.0,
			93.0,
			109.0,
			199.0,
			80.0,
			384.0
		};
		for (int i = 0; i < table.getColumns().size(); i++) {
			VFXTableColumn<User, ? extends VFXTableCell<User>> c = table.getColumns().get(i);
			try {
				assertEquals(vals[i], c.getWidth(), 5);
			} catch (AssertionError er) {
				System.err.println("Failed assertion for column: " + c.getText());
			}
		}
	}

	@Test
	void testAutosizeFixed(FxRobot robot) {
		StackPane pane = setupStage();
		Table table = new Table(FXCollections.observableArrayList(AUTOSIZE_USERS));
		robot.interact(() -> {
			// Increase window size and change pos
			setWindowPos(pane, 0, Double.NaN);
			setWindowSize(pane, 1200, -1);

			// Decrease minimum size
			table.setColumnsWidth(80.0);

			// Issue autosize here to also test the "delay" functionality
			table.autosizeColumns();
			pane.getChildren().add(table);
		});

		for (VFXTableColumn<User, ? extends VFXTableCell<User>> c : table.getColumns()) {
			assertEquals(240, c.getWidth(), 5);
		}
	}

	@Test
	void testManualUpdate(FxRobot robot) {
		StackPane pane = setupStage();
		Table table = new Table(users(20));
		robot.interact(() -> pane.getChildren().add(table));

		// Assert init
		assertState(table, IntegerRange.of(0, 15), IntegerRange.of(0, 6));
		assertCounter(112, 1, 112, 112, 0, 0, 0);
		assertRowsCounter(16, 16, 16, 0, 0, 0);

		// Get text before change row 5
		VFXTableCell<User> row5 = table.getState().getRowsByIndexUnmodifiable().get(5).getCellsUnmodifiable().get(0);
		Label label5 = (Label) row5.toNode().lookup(".label");
		String text5 = label5.getText();

		// Change item 5
		robot.interact(() -> table.getItems().get(5).setFirstName(faker.name().firstName()));
		assertEquals(text5, label5.getText()); // No automatic update

		// Force update (all)
		robot.interact(() -> table.update());
		assertNotEquals(text5, label5.getText());

		// Get text before change row 7
		VFXTableCell<User> row7 = table.getState().getRowsByIndexUnmodifiable().get(7).getCellsUnmodifiable().get(0);
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
		table.setColumnsSize(Size.of(180, 32));
		CSSFragment.Builder.build()
			.addSelector(".vfx-table")
			.border("#353839")
			.closeSelector()
			.addSelector(".vfx-table > .viewport > .columns")
			.border("transparent transparent #353839 transparent")
			.closeSelector()
			.addSelector(".vfx-table > .viewport > .columns > .vfx-column")
			.padding("0px 10px 0px 10px")
			.border("transparent #353839 transparent transparent")
			.closeSelector()
			.addSelector(".vfx-table > .viewport > .columns > .vfx-column:hover > .overlay")
			.addSelector(".vfx-table > .viewport > .columns > .vfx-column:dragged > .overlay")
			.background("rgba(53, 56, 57, 0.1)")
			.closeSelector()
			.addSelector(".vfx-table > .viewport > .rows > .vfx-row")
			.border("#353839")
			.borderInsets("1.25px")
			.addStyle("-fx-border-width: 0.5px")
			.closeSelector()
			.addSelector(".vfx-table > .viewport > .rows > .vfx-row > .table-cell")
			.padding("0px 10px 0px 10px")
			.closeSelector()
			.applyOn(table);

		int ICON_SIZE = 18;
		Color ICON_COLOR = Color.rgb(53, 57, 53);

		VFXDefaultTableColumn<FXUser, VFXObservingTableCell<FXUser, String>> firstNameColumn = new VFXDefaultTableColumn<>("First name");
		firstNameColumn.setCellFactory(u -> new VFXObservingTableCell<>(u, FXUser::firstNameProperty));
		firstNameColumn.setGraphic(new MFXFontIcon(FontAwesomeSolid.USER, ICON_SIZE, ICON_COLOR));
		VFXDefaultTableColumn<FXUser, VFXObservingTableCell<FXUser, String>> lastNameColumn = new VFXDefaultTableColumn<>("Last name");
		lastNameColumn.setCellFactory(u -> new VFXObservingTableCell<>(u, FXUser::lastNameProperty));
		lastNameColumn.setGraphic(new MFXFontIcon(FontAwesomeSolid.USER, ICON_SIZE, ICON_COLOR));
		VFXDefaultTableColumn<FXUser, VFXObservingTableCell<FXUser, Number>> birthColumn = new VFXDefaultTableColumn<>("Birth year");
		birthColumn.setCellFactory(u -> new VFXObservingTableCell<>(u, FXUser::birthYearProperty));
		birthColumn.setGraphic(new MFXFontIcon(FontAwesomeSolid.CAKE_CANDLES, ICON_SIZE, ICON_COLOR));
		VFXDefaultTableColumn<FXUser, VFXObservingTableCell<FXUser, String>> zodiacColumn = new VFXDefaultTableColumn<>("Zodiac Sign");
		zodiacColumn.setCellFactory(u -> new VFXObservingTableCell<>(u, FXUser::zodiacProperty));
		zodiacColumn.setGraphic(new MFXFontIcon(FontAwesomeSolid.STAR, ICON_SIZE, ICON_COLOR));
		VFXDefaultTableColumn<FXUser, VFXObservingTableCell<FXUser, String>> countryColumn = new VFXDefaultTableColumn<>("Country");
		countryColumn.setCellFactory(u -> new VFXObservingTableCell<>(u, FXUser::countryProperty));
		countryColumn.setGraphic(new MFXFontIcon(FontAwesomeSolid.GLOBE, ICON_SIZE, ICON_COLOR));
		VFXDefaultTableColumn<FXUser, VFXObservingTableCell<FXUser, String>> bloodColumn = new VFXDefaultTableColumn<>("Blood");
		bloodColumn.setCellFactory(u -> new VFXObservingTableCell<>(u, FXUser::bloodProperty));
		bloodColumn.setGraphic(new MFXFontIcon(FontAwesomeSolid.DROPLET, ICON_SIZE, ICON_COLOR));
		VFXDefaultTableColumn<FXUser, VFXObservingTableCell<FXUser, String>> animalColumn = new VFXDefaultTableColumn<>("Pet");
		animalColumn.setCellFactory(u -> new VFXObservingTableCell<>(u, FXUser::petProperty));
		animalColumn.setGraphic(new MFXFontIcon(FontAwesomeSolid.PAW, ICON_SIZE, ICON_COLOR));
		table.getColumns().addAll(firstNameColumn, lastNameColumn, birthColumn, zodiacColumn, countryColumn, bloodColumn, animalColumn);
		robot.interact(() -> pane.getChildren().add(table));

		// Assert init
		assertEquals(IntegerRange.of(0, 15), table.getRowsRange());
		assertEquals(IntegerRange.of(0, 6), table.getColumnsRange());

		// Get text before change row 5
		VFXTableCell<FXUser> row5 = table.getState().getRowsByIndexUnmodifiable().get(5).getCellsUnmodifiable().get(0);
		Label label5 = (Label) row5.toNode().lookup(".label");
		String text5 = label5.getText();

		// Change item 5
		robot.interact(() -> table.getItems().get(5).setFirstName(faker.name().firstName()));
		assertNotEquals(text5, label5.getText());

		// Get text before change row 7
		VFXTableCell<FXUser> row7 = table.getState().getRowsByIndexUnmodifiable().get(7).getCellsUnmodifiable().get(0);
		Label label7 = (Label) row7.toNode().lookup(".label");
		String text7 = label7.getText();

		// Change item 7
		robot.interact(() -> table.getItems().get(7).setFirstName(faker.name().firstName()));
		assertNotEquals(text7, label7.getText());

		// Scroll to check everything is ok
		VFXTableHelper<FXUser> helper = table.getHelper();
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
}
