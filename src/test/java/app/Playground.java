/*
 * Copyright (C) 2024 Parisi Alessandro - alessandro.parisi406@gmail.com
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

package app;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import fr.brouillard.oss.cssfx.CSSFX;
import io.github.palexdev.mfxcore.builders.InsetsBuilder;
import io.github.palexdev.mfxcore.utils.fx.ColorUtils;
import io.github.palexdev.virtualizedfx.VFXResources;
import io.github.palexdev.virtualizedfx.base.VFXScrollable;
import io.github.palexdev.virtualizedfx.controls.VFXScrollPane;
import io.github.palexdev.virtualizedfx.utils.ScrollParams;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Border;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import static src.utils.Utils.debugView;

public class Playground extends Application {
    private static final String LOREM;

    static {
        try {
            ClassLoader cl = Playground.class.getClassLoader();
            InputStream is = cl.getResourceAsStream("assets/Lorem20.md");
            byte[] bytes = is.readAllBytes();
            LOREM = new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        StackPane pane = new StackPane();
        pane.setAlignment(Pos.CENTER);
        pane.setPadding(InsetsBuilder.uniform(4.0).get());

        //TableTestUtils.Table table = new TableTestUtils.Table(users(10000));
        //table.setColumnsWidth(50.0);
        //table.setColumnsLayoutMode(ColumnsLayoutMode.VARIABLE);
        //table.autosizeColumns();

/*        ListTestUtils.List list = new ListTestUtils.List(items(100));
        list.setFitToViewport(false);*/

        //GridTestUtils.Grid grid = new GridTestUtils.Grid(items(500));
        //grid.setColumnsNum(5);

        Label label = new Label(LOREM);
        label.setBorder(Border.stroke(ColorUtils.getRandomColor()));

        Rectangle rt = new Rectangle(2000, 2000, ColorUtils.getRandomColor());

        VFXScrollPane sp = new VFXScrollPane(label);
        sp.setSmoothScroll(true);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);
        sp.setDragToScroll(true);
        //sp.setDragSmoothScroll(true);
        //sp.setShowButtons(true);
        //sp.setVBarPolicy(ScrollPaneEnums.ScrollBarPolicy.NEVER);
        //sp.setLayoutMode(ScrollPaneEnums.LayoutMode.COMPACT);
        //Platform.runLater(() -> sp.setPadding(InsetsBuilder.uniform(4.0).withTop(40.0).get()));
        //sp.setScrollBarsGap(0.0);
        //sp.setAutoHideBars(true);
        //sp.setScrollBarsPos(Pos.TOP_RIGHT);
        //sp.setVUnitIncrement(0.025);
        //sp.setHUnitIncrement(0.05);
        VFXScrollable.bindSpeed(sp, ScrollParams.percentage(0.2), ScrollParams.percentage(0.2));

        pane.getChildren().addAll(sp);
        Scene scene = new Scene(pane, 600, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.centerOnScreen();

        debugView(null, pane);

        sp.getStylesheets().add(VFXResources.loadResource("VFXScrollPane.css"));
        CSSFX.start(sp);
    }
}
