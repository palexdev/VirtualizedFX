/*
 * Copyright (C) 2022 Parisi Alessandro
 * This file is part of VirtualizedFX (https://github.com/palexdev/VirtualizedFX).
 *
 * VirtualizedFX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * VirtualizedFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with VirtualizedFX.  If not, see <http://www.gnu.org/licenses/>.
 */

package app.grid;

import app.cells.grid.SimpleGridCell;
import app.others.Utils;
import io.github.palexdev.materialfx.controls.MFXFilterComboBox;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.enums.FloatMode;
import io.github.palexdev.materialfx.utils.NodeUtils;
import io.github.palexdev.mfxcore.builders.bindings.StringBindingBuilder;
import io.github.palexdev.mfxcore.builders.nodes.IconWrapperBuilder;
import io.github.palexdev.mfxcore.collections.ObservableGrid;
import io.github.palexdev.mfxcore.controls.MFXIconWrapper;
import io.github.palexdev.mfxcore.utils.fx.RegionUtils;
import io.github.palexdev.mfxresources.builders.IconBuilder;
import io.github.palexdev.virtualizedfx.cell.GridCell;
import io.github.palexdev.virtualizedfx.controls.VirtualScrollPane;
import io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums;
import io.github.palexdev.virtualizedfx.grid.paginated.PaginatedVirtualGrid;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;

public class PGridTestController implements Initializable {

	@FXML StackPane rootPane;
	@FXML MFXFilterComboBox<PGridTestActions> actionsBox;
	@FXML MFXIconWrapper runIcon;
	@FXML StackPane contentPane;
	@FXML HBox footer;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// Init Parameters
		GridTestParameters parameters = new GridTestParameters(rootPane, SimpleGridCell.class);

		// Grid Initialization
		PaginatedVirtualGrid<Integer, GridCell<Integer>> grid = new PaginatedVirtualGrid<>(
				ObservableGrid.fromMatrix(Utils.randMatrix(50, 10)),
				SimpleGridCell::new
		);

		// Init Actions
		actionsBox.setItems(FXCollections.observableArrayList(PGridTestActions.values()));
		actionsBox.selectFirst();
		runIcon.setOnMouseClicked(event -> actionsBox.getSelectedItem().run(parameters, grid));
		runIcon.defaultRippleGeneratorBehavior();
		RegionUtils.makeRegionCircular(runIcon);

		// Init Content Pane
		VirtualScrollPane vsp = grid.wrap();
		vsp.setLayoutMode(ScrollPaneEnums.LayoutMode.COMPACT);
		vsp.setAutoHideBars(true);

		contentPane.getChildren().setAll(vsp);

		// Footer
		// TODO spinner models are absolute garbage
		MFXIconWrapper goFirst = IconWrapperBuilder.build()
				.setIcon(IconBuilder.build().setDescription("fas-backward-step").setSize(16).get())
				.setSize(32)
				.defaultRippleGeneratorBehavior()
				.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> grid.goToFirstPage()).getNode();
		NodeUtils.makeRegionCircular(goFirst);

		MFXIconWrapper goBack = IconWrapperBuilder.build()
				.setIcon(IconBuilder.build().setDescription("fas-caret-left").setSize(16).get())
				.setSize(32)
				.defaultRippleGeneratorBehavior()
				.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> grid.setCurrentPage(Math.max(1, grid.getCurrentPage() - 1))).getNode();
		NodeUtils.makeRegionCircular(goBack);

		MFXIconWrapper goForward = IconWrapperBuilder.build()
				.setIcon(IconBuilder.build().setDescription("fas-caret-right").setSize(16).get())
				.setSize(32)
				.defaultRippleGeneratorBehavior()
				.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> grid.setCurrentPage(grid.getCurrentPage() + 1)).getNode();
		NodeUtils.makeRegionCircular(goForward);

		MFXIconWrapper goLast = IconWrapperBuilder.build()
				.setIcon(IconBuilder.build().setDescription("fas-forward-step").setSize(16).get())
				.setSize(32)
				.defaultRippleGeneratorBehavior()
				.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> grid.goToLastPage()).getNode();
		NodeUtils.makeRegionCircular(goLast);

		MFXTextField label = MFXTextField.asLabel();
		label.setFloatMode(FloatMode.DISABLED);
		label.setAlignment(Pos.CENTER);
		label.setLeadingIcon(new HBox(5, goFirst, goBack));
		label.setTrailingIcon(new HBox(5, goForward, goLast));
		label.textProperty().bind(StringBindingBuilder.build()
				.setMapper(() -> grid.getCurrentPage() + "/" + grid.getMaxPage())
				.addSources(grid.currentPageProperty(), grid.maxPageProperty())
				.get()
		);

		footer.getChildren().addAll(label);
	}
}
