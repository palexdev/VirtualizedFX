package interactive;

import app.Tests;
import app.model.Model;
import app.model.User;
import app.table.TableTestController.AltCell;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.utils.fx.FXCollectors;
import io.github.palexdev.virtualizedfx.cell.TableCell;
import io.github.palexdev.virtualizedfx.enums.ColumnsLayoutMode;
import io.github.palexdev.virtualizedfx.table.TableRow;
import io.github.palexdev.virtualizedfx.table.TableState;
import io.github.palexdev.virtualizedfx.table.VirtualTable;
import io.github.palexdev.virtualizedfx.table.defaults.DefaultTableColumn;
import io.github.palexdev.virtualizedfx.table.defaults.SimpleTableCell;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import others.SettableUser;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
public class TableTests {

	@Start
	void start(Stage stage) {
		StackPane root = new StackPane();
		root.setPadding(new Insets(20));
		root.getStyleClass().setAll("content");

		Scene scene = new Scene(root, 800, 800);
		scene.getStylesheets().add(Tests.class.getResource("table/TableTest.css").toExternalForm());
		stage.setScene(scene);
		stage.setTitle("Table Tests");
		stage.show();
	}

	private VirtualTable<User> createTable(boolean empty) {
		VirtualTable<User> table = empty ? new VirtualTable<>() : new VirtualTable<>(Model.users);
		DefaultTableColumn<User, TableCell<User>> didc = createColumn(table, "ID", User::id);
		DefaultTableColumn<User, TableCell<User>> dnc = createColumn(table, "Name", User::name);
		DefaultTableColumn<User, TableCell<User>> dac = createColumn(table, "Age", User::age);
		table.getColumns().addAll(didc, dnc, dac);
		return table;
	}

	private <E> DefaultTableColumn<User, TableCell<User>> createColumn(VirtualTable<User> table, String text, Function<User, E> extractor) {
		DefaultTableColumn<User, TableCell<User>> column = new DefaultTableColumn<>(table, text);
		column.setCellFactory(u -> new AltCell<>(u, extractor));
		column.setOnMousePressed(e -> {
			System.out.println(e);
			if (e.getButton() == MouseButton.SECONDARY)
				table.getColumns().remove(column);
		});
		return column;
	}

	VirtualTable<User> setup(FxRobot robot, boolean empty) {
		StackPane root = robot.lookup(".content").query();
		VirtualTable<User> table = createTable(empty);
		robot.interact(() -> root.getChildren().setAll(table));
		return table;
	}

	@Test
	void testEmptyInit(FxRobot robot) {
		VirtualTable<User> table = setup(robot, true);
		assertEquals(IntegerRange.of(0, 2), table.getState().getColumnsRange());
	}

	@Test
	void testSetEmpty(FxRobot robot) {
		VirtualTable<User> table = setup(robot, false);
		assertFalse(table.getState().isEmptyAll());
		assertTrue(table.getState().size() != 0);
		assertEquals(IntegerRange.of(0, 2), table.getState().getColumnsRange());
		robot.interact(() -> table.getItems().clear());
		assertTrue(table.getState().isEmpty());
		assertFalse(table.getState().isEmptyAll());
		assertEquals(IntegerRange.of(0, 2), table.getState().getColumnsRange());
	}

	@Test
	void testEmptyInitAndClearColumns(FxRobot robot) {
		VirtualTable<User> table = setup(robot, true);
		assertEquals(IntegerRange.of(0, 2), table.getState().getColumnsRange());
		robot.interact(() -> table.getColumns().clear());
		assertEquals(IntegerRange.of(-1, -1), table.getState().getColumnsRange());
	}

	@Test
	void testClearResetColumns(FxRobot robot) {
		VirtualTable<User> table = setup(robot, false);
		assertEquals(IntegerRange.of(0, 2), table.getState().getColumnsRange());
		robot.interact(() -> table.getColumns().clear());
		assertEquals(IntegerRange.of(-1, -1), table.getState().getColumnsRange());

		robot.interact(() -> {
			DefaultTableColumn<User, TableCell<User>> didc = createColumn(table, "ID", User::id);
			DefaultTableColumn<User, TableCell<User>> dnc = createColumn(table, "Name", User::name);
			DefaultTableColumn<User, TableCell<User>> dac = createColumn(table, "Age", User::age);
			table.getColumns().addAll(didc, dnc, dac);
		});
		assertEquals(IntegerRange.of(0, 2), table.getState().getColumnsRange());
		Region container = robot.lookup(".columns-container").queryAs(Region.class);
		assertFalse(container.getChildrenUnmodifiable().isEmpty());
	}

	@Test
	void testRemoveColumns(FxRobot robot) {
		VirtualTable<User> table = setup(robot, false);
		assertEquals(IntegerRange.of(0, 2), table.getState().getColumnsRange());
		robot.interact(() -> table.getColumns().remove(table.getColumns().size() - 1));
		assertEquals(IntegerRange.of(0, 1), table.getState().getColumnsRange());
	}

	@Test
	void testCorrectInit(FxRobot robot) {
		VirtualTable<User> table = setup(robot, false);
		TableState<User> state = table.getState();
		Map<Integer, TableRow<User>> rows = state.getRowsUnmodifiable();
		for (TableRow<User> r : rows.values()) {
			for (Node node : r.getCellsAsNodes()) {
				assertEquals(table.getCellHeight(), node.getLayoutBounds().getHeight());
			}
		}
	}

	@Test
	void testAutosizeEmpty(FxRobot robot) {
		VirtualTable<User> table = setup(robot, true);
		robot.interact(() -> table.setColumnsLayoutMode(ColumnsLayoutMode.VARIABLE));
		table.getTableHelper().autosizeColumns();
	}

	@Test
	void testProgrammaticUpdate(FxRobot robot) {
		ObservableList<SettableUser> users = IntStream.range(0, 10)
				.mapToObj(i -> SettableUser.random())
				.collect(FXCollectors.toList());
		StackPane root = robot.lookup(".content").query();
		VirtualTable<SettableUser> table = new VirtualTable<>(users);
		DefaultTableColumn<SettableUser, TableCell<SettableUser>> didc = new DefaultTableColumn<>(table, "First Name");
		DefaultTableColumn<SettableUser, TableCell<SettableUser>> dnc = new DefaultTableColumn<>(table, "Last Name");
		DefaultTableColumn<SettableUser, TableCell<SettableUser>> dac = new DefaultTableColumn<>(table, "Age");
		didc.setCellFactory(u -> new SimpleTableCell<>(u, SettableUser::getName));
		dnc.setCellFactory(u -> new SimpleTableCell<>(u, SettableUser::getSurname));
		dac.setCellFactory(u -> new SimpleTableCell<>(u, SettableUser::getAge));
		table.getColumns().addAll(didc, dnc, dac);
		robot.interact(() -> root.getChildren().setAll(table));

		SettableUser user = table.getItems().get(0);
		user.setName("CHANGED");

		robot.interact(() -> table.updateTable(false));
		TableState<SettableUser> state = table.getState();
		TableRow<SettableUser> firstRow = state.getRowsUnmodifiable().get(0);
		SimpleTableCell<SettableUser, ?> nameCell = (SimpleTableCell<SettableUser, ?>) firstRow.getCellsUnmodifiable().get(0);
		assertEquals("CHANGED", ((Label) nameCell.getChildren().get(0)).getText());
	}
}
