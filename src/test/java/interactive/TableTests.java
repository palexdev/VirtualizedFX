package interactive;

import app.model.Model;
import app.model.User;
import app.table.TableTestController;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.virtualizedfx.cell.TableCell;
import io.github.palexdev.virtualizedfx.table.VirtualTable;
import io.github.palexdev.virtualizedfx.table.defaults.DefaultTableColumn;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(ApplicationExtension.class)
public class TableTests {
	private VirtualTable<User> table;

	@Start
	void start(Stage stage) {
		StackPane root = new StackPane();
		root.setPadding(new Insets(20));

		table = new VirtualTable<>(Model.users);
		DefaultTableColumn<User, TableCell<User>> didc = createColumn(table, "ID", User::id);
		DefaultTableColumn<User, TableCell<User>> dnc = createColumn(table, "Name", User::name);
		DefaultTableColumn<User, TableCell<User>> dac = createColumn(table, "Age", User::age);
		table.getColumns().addAll(didc, dnc, dac);

		root.getChildren().add(table);
		Scene scene = new Scene(root, 800, 800);
		stage.setScene(scene);
		stage.setTitle("Table Tests");
		stage.show();
	}

	private <E> DefaultTableColumn<User, TableCell<User>> createColumn(VirtualTable<User> table, String text, Function<User, E> extractor) {
		DefaultTableColumn<User, TableCell<User>> column = new DefaultTableColumn<>(table, text);
		column.setCellFactory(u -> new TableTestController.AltCell<>(u, extractor));
		column.setOnMouseClicked(e -> {
			if (e.getButton() == MouseButton.SECONDARY)
				table.getColumns().remove(column);
		});
		return column;
	}

	@Test
	void testEmptyInit(FxRobot robot) {
		robot.interact(() -> table.setItems(null));
		assertEquals(IntegerRange.of(0, 2), table.getState().getColumnsRange());

	}
}
