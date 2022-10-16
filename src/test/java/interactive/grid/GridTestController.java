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

package interactive.grid;

import interactive.cells.grid.SimpleGridCell;
import interactive.others.Utils;
import io.github.palexdev.materialfx.controls.MFXCheckbox;
import io.github.palexdev.materialfx.controls.MFXFilterComboBox;
import io.github.palexdev.mfxcore.collections.ObservableGrid;
import io.github.palexdev.mfxcore.controls.MFXIconWrapper;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.mfxcore.utils.fx.RegionUtils;
import io.github.palexdev.virtualizedfx.cell.GridCell;
import io.github.palexdev.virtualizedfx.controls.VirtualScrollPane;
import io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums;
import io.github.palexdev.virtualizedfx.grid.VirtualGrid;
import io.github.palexdev.virtualizedfx.utils.VSPUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class GridTestController implements Initializable {

	@FXML StackPane rootPane;
	@FXML MFXFilterComboBox<GridTestActions> actionsBox;
	@FXML MFXIconWrapper runIcon;
	@FXML StackPane contentPane;
	@FXML MFXCheckbox ssCheck;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// Init Parameters
		GridTestParameters parameters = new GridTestParameters(rootPane, SimpleGridCell.class);

		// Grid Initialization
		VirtualGrid<Integer, GridCell<Integer>> grid = new VirtualGrid<>(
				ObservableGrid.fromMatrix(Utils.randMatrix(50, 10)),
				SimpleGridCell::new
		);

		// Init Actions
		actionsBox.setItems(FXCollections.observableArrayList(GridTestActions.values()));
		actionsBox.selectFirst();
		runIcon.setOnMouseClicked(event -> actionsBox.getSelectedItem().run(parameters, grid));
		runIcon.defaultRippleGeneratorBehavior();
		RegionUtils.makeRegionCircular(runIcon);

		// Init Content Pane
		VirtualScrollPane vsp = VSPUtils.wrap(grid);
		vsp.setLayoutMode(ScrollPaneEnums.LayoutMode.COMPACT);
		vsp.setAutoHideBars(true);
		vsp.setMinHeight(500);
		vsp.setMaxWidth(1000);
		Runnable speedAction = () -> {
			double ch = grid.getCellSize().getHeight();
			double cw = grid.getCellSize().getWidth();
			VSPUtils.setVSpeed(vsp, ch / 3, ch / 2, ch / 2);
			VSPUtils.setHSpeed(vsp, cw / 3, cw / 2, cw / 2);
		};
		When.onInvalidated(grid.cellSizeProperty())
				.then(i -> speedAction.run())
				.executeNow()
				.listen();

		contentPane.getChildren().setAll(vsp);

		// Init Settings
		Consumer<Boolean> ssAction = val -> {
			vsp.setSmoothScroll(true);
			vsp.setTrackSmoothScroll(true);
		};
		ssCheck.selectedProperty().addListener((observable, oldValue, newValue) -> ssAction.accept(newValue));
	}
}
