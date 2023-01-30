package interactive;

import app.Tests;
import app.model.Model;
import app.model.User;
import app.table.TableTestController;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.virtualizedfx.cell.TableCell;
import io.github.palexdev.virtualizedfx.table.TableState;
import io.github.palexdev.virtualizedfx.table.VirtualTable;
import io.github.palexdev.virtualizedfx.table.defaults.DefaultTableColumn;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.function.Function;

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
		column.setCellFactory(u -> new TableTestController.AltCell<>(u, extractor));
		column.setOnMousePressed(e -> {
			System.out.println(e);
			if (e.getButton() == MouseButton.SECONDARY)
				table.getColumns().remove(column);
		});
		return column;
	}

	@Test
	void testEmptyInit(FxRobot robot) {
		StackPane root = robot.lookup(".content").query();
		VirtualTable<User> table = createTable(true);
		robot.interact(() -> root.getChildren().setAll(table));
		assertEquals(IntegerRange.of(0, 2), table.getState().getColumnsRange());
	}

	@Test
	void testSetEmpty(FxRobot robot) {
		StackPane root = robot.lookup(".content").query();
		VirtualTable<User> table = createTable(false);
		robot.interact(() -> root.getChildren().setAll(table));
		assertNotSame(TableState.EMPTY, table.getState());
		assertTrue(table.getState().size() != 0);
		assertEquals(IntegerRange.of(0, 2), table.getState().getColumnsRange());
		robot.interact(() -> table.getItems().clear());
		assertSame(TableState.EMPTY, table.getState());
		assertEquals(IntegerRange.of(0, 2), table.getState().getColumnsRange());
	}

	@Test
	void testEmptyInitAndClearColumns(FxRobot robot) {
		StackPane root = robot.lookup(".content").query();
		VirtualTable<User> table = createTable(true);
		robot.interact(() -> root.getChildren().setAll(table));
		assertEquals(IntegerRange.of(0, 2), table.getState().getColumnsRange());
		robot.interact(() -> table.getColumns().clear());
		assertEquals(IntegerRange.of(-1, -1), table.getState().getColumnsRange());
	}

	@Test
	void testClearResetColumns(FxRobot robot) {
		StackPane root = robot.lookup(".content").query();
		VirtualTable<User> table = createTable(false);
		robot.interact(() -> root.getChildren().setAll(table));
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
		StackPane root = robot.lookup(".content").query();
		VirtualTable<User> table = createTable(false);
		robot.interact(() -> root.getChildren().setAll(table));
		assertEquals(IntegerRange.of(0, 2), table.getState().getColumnsRange());
		robot.interact(() -> table.getColumns().remove(table.getColumns().size() - 1));
		assertEquals(IntegerRange.of(0, 1), table.getState().getColumnsRange());
	}
}
