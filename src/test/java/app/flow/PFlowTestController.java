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

package app.flow;

import app.cells.flow.DetailedCell;
import app.model.Model;
import io.github.palexdev.materialfx.controls.MFXFilterComboBox;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.enums.FloatMode;
import io.github.palexdev.materialfx.utils.NodeUtils;
import io.github.palexdev.mfxcore.builders.bindings.StringBindingBuilder;
import io.github.palexdev.mfxcore.utils.fx.RegionUtils;
import io.github.palexdev.mfxresources.builders.IconBuilder;
import io.github.palexdev.mfxresources.builders.IconWrapperBuilder;
import io.github.palexdev.mfxresources.fonts.MFXIconWrapper;
import io.github.palexdev.virtualizedfx.cell.Cell;
import io.github.palexdev.virtualizedfx.controls.VirtualScrollPane;
import io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums.LayoutMode;
import io.github.palexdev.virtualizedfx.flow.paginated.PaginatedVirtualFlow;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;

public class PFlowTestController implements Initializable {

    @FXML
    private StackPane rootPane;
    @FXML
    private MFXFilterComboBox<PFlowTestActions> actionsBox;
    @FXML
    private MFXIconWrapper runIcon;
    @FXML
    private StackPane contentPane;
    @FXML
    private HBox footer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Init Parameters
        FlowTestParameters parameters = new FlowTestParameters(rootPane, DetailedCell.class);

        // Flow Initialization
        PaginatedVirtualFlow<Integer, Cell<Integer>> flow = new PaginatedVirtualFlow<>(
                FXCollections.observableArrayList(Model.integers),
                DetailedCell::new
        );
        flow.setCellSize(64);

        // Init Actions
        actionsBox.setItems(FXCollections.observableArrayList(PFlowTestActions.values()));
        actionsBox.selectFirst();

        runIcon.setOnMouseClicked(event -> actionsBox.getSelectedItem().run(parameters, flow));
        runIcon.enableRippleGenerator(true);
        RegionUtils.makeRegionCircular(runIcon);

        // Init Content Pane
        VirtualScrollPane vsp = flow.wrap();
        vsp.setLayoutMode(LayoutMode.COMPACT);
        vsp.setAutoHideBars(true);

        contentPane.getChildren().setAll(vsp);

        // Footer
        // TODO spinner models are absolute garbage
        MFXIconWrapper goFirst = IconWrapperBuilder.build()
                .setIcon(IconBuilder.build().setDescription("fas-backward-step").setSize(16).get())
                .setSize(32)
                .enableRippleGenerator(true)
                .addEventHandler(MouseEvent.MOUSE_CLICKED, event -> flow.scrollToFirst()).get();
        NodeUtils.makeRegionCircular(goFirst);

        MFXIconWrapper goBack = IconWrapperBuilder.build()
                .setIcon(IconBuilder.build().setDescription("fas-caret-left").setSize(16).get())
                .setSize(32)
                .enableRippleGenerator(true)
                .addEventHandler(MouseEvent.MOUSE_CLICKED, event -> flow.setCurrentPage(Math.max(0, flow.getCurrentPage() - 1))).get();
        NodeUtils.makeRegionCircular(goBack);

        MFXIconWrapper goForward = IconWrapperBuilder.build()
                .setIcon(IconBuilder.build().setDescription("fas-caret-right").setSize(16).get())
                .setSize(32)
                .enableRippleGenerator(true)
                .addEventHandler(MouseEvent.MOUSE_CLICKED, event -> flow.setCurrentPage(flow.getCurrentPage() + 1)).get();
        NodeUtils.makeRegionCircular(goForward);

        MFXIconWrapper goLast = IconWrapperBuilder.build()
                .setIcon(IconBuilder.build().setDescription("fas-forward-step").setSize(16).get())
                .setSize(32)
                .enableRippleGenerator(true)
                .addEventHandler(MouseEvent.MOUSE_CLICKED, event -> flow.scrollToLast()).get();
        NodeUtils.makeRegionCircular(goLast);

        MFXTextField label = MFXTextField.asLabel();
        label.setFloatMode(FloatMode.DISABLED);
        label.setAlignment(Pos.CENTER);
        label.setLeadingIcon(new HBox(5, goFirst, goBack));
        label.setTrailingIcon(new HBox(5, goForward, goLast));
        label.textProperty().bind(StringBindingBuilder.build()
                .setMapper(() -> flow.getCurrentPage() + "/" + flow.getMaxPage())
                .addSources(flow.currentPageProperty(), flow.maxPageProperty())
                .get()
        );

        footer.getChildren().addAll(label);
    }
}
