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

package interactive.flow;

import interactive.cells.flow.DetailedCell;
import interactive.cells.flow.GlobalCellParameters;
import interactive.flow.FlowTestParameters.Mode;
import interactive.model.Model;
import io.github.palexdev.materialfx.controls.MFXCheckbox;
import io.github.palexdev.materialfx.controls.MFXFilterComboBox;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.controls.MFXTooltip;
import io.github.palexdev.mfxcore.builders.bindings.StringBindingBuilder;
import io.github.palexdev.mfxcore.controls.MFXIconWrapper;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.mfxcore.utils.fx.RegionUtils;
import io.github.palexdev.virtualizedfx.cell.Cell;
import io.github.palexdev.virtualizedfx.controls.VirtualScrollPane;
import io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums.LayoutMode;
import io.github.palexdev.virtualizedfx.flow.VirtualFlow;
import io.github.palexdev.virtualizedfx.unused.simple.SimpleVirtualFlow;
import io.github.palexdev.virtualizedfx.utils.VSPUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;

public class FlowTestController implements Initializable {

	@FXML private StackPane rootPane;
	@FXML private Label selectionLabel;
	@FXML private StackPane leftPane;
	@FXML private StackPane rightPane;
	@FXML private MFXFilterComboBox<FlowTestActions> actionsBox;
	@FXML private MFXIconWrapper runIcon;
	@FXML private MFXIconWrapper switchIcon;
	@FXML private MFXCheckbox ssCheck;
	@FXML private MFXCheckbox modeCheck;
	@FXML private MFXCheckbox animCheck;
	@FXML private MFXTextField durationField;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// Init Parameters
		FlowTestParameters parameters = new FlowTestParameters(rootPane, DetailedCell.class);

		// Flow Initializations
		SimpleVirtualFlow<Integer, Cell<Integer>> lFlow = new SimpleVirtualFlow<>(
				FXCollections.observableArrayList(Model.integers),
				DetailedCell::new,
				Orientation.VERTICAL
		);

		VirtualFlow<Integer, Cell<Integer>> rFlow = new VirtualFlow<>(
				FXCollections.observableArrayList(Model.integers),
				DetailedCell::new
		);
		rFlow.setCellSize(64);

		// Init Actions
		selectionLabel.textProperty().bind(StringBindingBuilder.build()
				.setMapper(() -> {
					Mode mode = parameters.getMode();
					return "Action Target: " + mode;
				})
				.addSources(parameters.modeProperty())
				.get()
		);
		actionsBox.setItems(FXCollections.observableArrayList(FlowTestActions.values()));
		actionsBox.selectFirst();

		runIcon.setOnMouseClicked(event -> actionsBox.getSelectedItem().run(parameters, lFlow, rFlow));
		switchIcon.setOnMouseClicked(event -> {
			if (modeCheck.isSelected()) return;
			parameters.switchMode();
		});
		runIcon.defaultRippleGeneratorBehavior();
		switchIcon.defaultRippleGeneratorBehavior();
		RegionUtils.makeRegionCircular(runIcon);
		RegionUtils.makeRegionCircular(switchIcon);

		// Init Content Pane
		VirtualScrollPane vsp = rFlow.wrap();
		vsp.setLayoutMode(LayoutMode.COMPACT);
		vsp.setAutoHideBars(true);
		Runnable speedAction = () -> {
			VSPUtils.setVSpeed(vsp, 15, rFlow.getCellSize() * 2, 15);
			VSPUtils.setHSpeed(vsp, 15, rFlow.getCellSize() * 2, 15);
		};
		When.onInvalidated(rFlow.cellSizeProperty())
				.then(i -> speedAction.run())
				.executeNow()
				.listen();

		leftPane.getChildren().setAll(lFlow);
		rightPane.getChildren().setAll(vsp);

		// Init Settings
		ssCheck.selectedProperty().addListener((ob, o, n) -> {
			if (n) {
				lFlow.features().enableSmoothScrolling(1);
			} else {
				lFlow.features().disableSmoothScrolling();
			}
			vsp.setSmoothScroll(n);
			vsp.setTrackSmoothScroll(n);
		});
		MFXTooltip tooltip = MFXTooltip.of(ssCheck,
				"""
						Notice how in the new implementation
						the smooth scrolling is much more reliable thanks to the new MFXScrollbar.
						Check the documentation and source code to know more about it.
						"""
		).install();
		MFXTooltip.fixPosition(tooltip, true);
		modeCheck.selectedProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue) {
				parameters.setMode(Mode.BOTH);
			} else {
				parameters.setMode(Mode.NEW_IMP);
			}
		});

		durationField.delegateSetTextFormatter(new TextFormatter<>(change -> change.getControlNewText().matches("[0-9]*") ? change : null));
		durationField.textProperty().addListener(invalidated -> GlobalCellParameters.setAnimationDuration(Double.parseDouble(durationField.getText())));

		When.onChanged(animCheck.selectedProperty())
				.then((oldValue, newValue) -> GlobalCellParameters.setAnimEnabled(newValue))
				.executeNow()
				.listen();
	}
}
