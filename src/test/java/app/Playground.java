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

import interactive.table.TableTestUtils;
import io.github.palexdev.mfxcore.builders.InsetsBuilder;
import io.github.palexdev.virtualizedfx.base.VFXScrollable;
import io.github.palexdev.virtualizedfx.controls.VFXScrollPane;
import io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums;
import io.github.palexdev.virtualizedfx.utils.ScrollParams;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static src.model.User.users;
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

        TableTestUtils.Table table = new TableTestUtils.Table(users(100));
        /*ListTestUtils.List list = new ListTestUtils.List(items(100));
        list.setFitToViewport(false);*/

        /*GridTestUtils.Grid grid = new GridTestUtils.Grid(items(500));
        grid.setColumnsNum(20);*/

        VFXScrollPane sp = new VFXScrollPane(table);
        //sp.setSmoothScroll(true);
        //sp.setDragToScroll(true);
        //sp.setDragSmoothScroll(true);
        //sp.setShowButtons(true);
        //sp.setVBarPolicy(ScrollPaneEnums.ScrollBarPolicy.NEVER);
        sp.setLayoutMode(ScrollPaneEnums.LayoutMode.COMPACT);
        Platform.runLater(() -> sp.setPadding(InsetsBuilder.uniform(4.0).withTop(40.0).get()));
        //sp.setScrollBarsGap(0.0);
        //sp.setAutoHideBars(true);
        //sp.setScrollBarsPos(Pos.TOP_RIGHT);
        //sp.setVUnitIncrement(0.025);
        //sp.setHUnitIncrement(0.05);

/*
        Label label = new Label(LOREM);
        sp = new VFXScrollPane(label);
*/

        VFXScrollable.bindSpeed(sp, ScrollParams.cells(1.25), ScrollParams.percentage(0.05));

        pane.getChildren().addAll(sp);
        Scene scene = new Scene(pane, 600, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.centerOnScreen();

        debugView(null, pane);
    }
}
