package interactive.table;

import interactive.model.Model;
import interactive.model.User;
import interactive.table.TableTestController.AltCell;
import io.github.palexdev.materialfx.controls.MFXFilterComboBox;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.enums.FloatMode;
import io.github.palexdev.materialfx.utils.NodeUtils;
import io.github.palexdev.mfxcore.builders.bindings.StringBindingBuilder;
import io.github.palexdev.mfxcore.builders.nodes.IconWrapperBuilder;
import io.github.palexdev.mfxcore.controls.MFXIconWrapper;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.mfxcore.utils.fx.RegionUtils;
import io.github.palexdev.mfxresources.builders.IconBuilder;
import io.github.palexdev.virtualizedfx.cell.TableCell;
import io.github.palexdev.virtualizedfx.controls.VirtualScrollPane;
import io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums;
import io.github.palexdev.virtualizedfx.table.VirtualTable;
import io.github.palexdev.virtualizedfx.table.defaults.DefaultTableColumn;
import io.github.palexdev.virtualizedfx.table.paginated.PaginatedVirtualTable;
import io.github.palexdev.virtualizedfx.utils.VSPUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public class PTableTestController implements Initializable {

	@FXML StackPane rootPane;
	@FXML MFXFilterComboBox<PTableTestActions> actionsBox;
	@FXML MFXIconWrapper runIcon;
	@FXML StackPane contentPane;
	@FXML HBox footer;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// Create Test Parameters
		TableTestParameters<User> parameters = new TableTestParameters<>(rootPane);

		// Table Initialization
		PaginatedVirtualTable<User> table = new PaginatedVirtualTable<>(Model.users);

		// Columns Initialization
		DefaultTableColumn<User, TableCell<User>> idxc = createColumn(table, "", null);
		DefaultTableColumn<User, TableCell<User>> didc = createColumn(table, "ID", User::id);
		DefaultTableColumn<User, TableCell<User>> dnc = createColumn(table, "Name", User::name);
		DefaultTableColumn<User, TableCell<User>> dac = createColumn(table, "Age", User::age);
		DefaultTableColumn<User, TableCell<User>> dhc = createColumn(table, "Height", User::height);
		DefaultTableColumn<User, TableCell<User>> gc = createColumn(table, "Gender", User::gender);
		DefaultTableColumn<User, TableCell<User>> nc = createColumn(table, "Nationality", User::nationality);
		DefaultTableColumn<User, TableCell<User>> cpc = createColumn(table, "Phone", User::cellPhone);
		DefaultTableColumn<User, TableCell<User>> uc = createColumn(table, "University", User::university);
		table.getColumns().setAll(idxc, didc, dnc, dac, dhc, gc, nc, cpc, uc);

		// Init Parameters
		parameters.initCellsMap(table, AltCell.class);
		parameters.setColumns(table.getColumns());

		// Init Actions
		actionsBox.setItems(FXCollections.observableArrayList(PTableTestActions.values()));
		actionsBox.selectFirst();
		runIcon.setOnMouseClicked(e -> actionsBox.getSelectedItem().run(parameters, table));
		runIcon.defaultRippleGeneratorBehavior();
		RegionUtils.makeRegionCircular(runIcon);

		// Init Content Pane
		VirtualScrollPane vsp = table.wrap();
		vsp.setLayoutMode(ScrollPaneEnums.LayoutMode.COMPACT);
		vsp.setAutoHideBars(true);
		Runnable speedAction = () -> {
			double ch = table.getCellHeight();
			double cw = table.getColumnSize().getWidth();
			VSPUtils.setVSpeed(vsp, ch / 3, ch / 2, ch / 2);
			VSPUtils.setHSpeed(vsp, cw / 3, cw / 2, cw / 2);
		};
		When.onInvalidated(table.cellHeightProperty())
				.then(i -> speedAction.run())
				.executeNow()
				.listen();
		When.onInvalidated(table.columnSizeProperty())
				.then(i -> speedAction.run())
				.executeNow()
				.listen();

		contentPane.getChildren().add(vsp);

		// Footer
		// TODO spinner models are absolute garbage
		MFXIconWrapper goFirst = IconWrapperBuilder.build()
				.setIcon(IconBuilder.build().setDescription("mfx-step-backward").setSize(16).get())
				.setSize(32)
				.defaultRippleGeneratorBehavior()
				.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> table.goToFirstPage()).getNode();
		NodeUtils.makeRegionCircular(goFirst);

		MFXIconWrapper goBack = IconWrapperBuilder.build()
				.setIcon(IconBuilder.build().setDescription("mfx-caret-left").setSize(16).get())
				.setSize(32)
				.defaultRippleGeneratorBehavior()
				.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> table.setCurrentPage(Math.max(1, table.getCurrentPage() - 1))).getNode();
		NodeUtils.makeRegionCircular(goBack);

		MFXIconWrapper goForward = IconWrapperBuilder.build()
				.setIcon(IconBuilder.build().setDescription("mfx-caret-right").setSize(16).get())
				.setSize(32)
				.defaultRippleGeneratorBehavior()
				.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> table.setCurrentPage(table.getCurrentPage() + 1)).getNode();
		NodeUtils.makeRegionCircular(goForward);

		MFXIconWrapper goLast = IconWrapperBuilder.build()
				.setIcon(IconBuilder.build().setDescription("mfx-step-forward").setSize(16).get())
				.setSize(32)
				.defaultRippleGeneratorBehavior()
				.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> table.goToLastPage()).getNode();
		NodeUtils.makeRegionCircular(goLast);

		MFXTextField label = MFXTextField.asLabel();
		label.setFloatMode(FloatMode.DISABLED);
		label.setAlignment(Pos.CENTER);
		label.setLeadingIcon(new HBox(5, goFirst, goBack));
		label.setTrailingIcon(new HBox(5, goForward, goLast));
		label.textProperty().bind(StringBindingBuilder.build()
				.setMapper(() -> table.getCurrentPage() + "/" + table.getMaxPage())
				.addSources(table.currentPageProperty(), table.maxPageProperty())
				.get()
		);

		footer.getChildren().addAll(label);
	}

	private <E> DefaultTableColumn<User, TableCell<User>> createColumn(VirtualTable<User> table, String text, Function<User, E> extractor) {
		DefaultTableColumn<User, TableCell<User>> column = new DefaultTableColumn<>(table, text);
		column.setCellFactory(u -> new AltCell<>(u, extractor));
		column.setOnMouseClicked(e -> {
			if (e.getButton() == MouseButton.SECONDARY)
				table.getColumns().remove(column);
		});
		return column;
	}
}
