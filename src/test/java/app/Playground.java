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
import java.util.stream.IntStream;

import fr.brouillard.oss.cssfx.CSSFX;
import interactive.table.TableTestUtils;
import io.github.palexdev.mfxcore.utils.EnumUtils;
import io.github.palexdev.mfxcore.utils.fx.ColorUtils;
import io.github.palexdev.virtualizedfx.base.VFXScrollable;
import io.github.palexdev.virtualizedfx.controls.VFXScrollPane;
import io.github.palexdev.virtualizedfx.enums.ColumnsLayoutMode;
import io.github.palexdev.virtualizedfx.table.defaults.VFXDefaultTableColumn;
import io.github.palexdev.virtualizedfx.utils.ScrollParams;
import javafx.application.Application;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

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
    public void start(Stage stage) {
        startPlayground(stage);
    }

    private void startPlayground(Stage stage) {
        StackPane pane = new StackPane();
        pane.setAlignment(Pos.CENTER);

        TableTestUtils.Table vc = new TableTestUtils.Table(users(15));
        vc.setColumnsWidth(50.0);
        vc.setColumnsLayoutMode(ColumnsLayoutMode.VARIABLE);
        vc.autosizeColumns();

        vc.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() != MouseButton.SECONDARY) return;
            vc.getColumns().forEach(c -> {
                HPos next = EnumUtils.next(HPos.class, ((VFXDefaultTableColumn) c).getIconAlignment());
                ((VFXDefaultTableColumn) c).setIconAlignment(next);
            });
        });

/*        ListTestUtils.List vc = new ListTestUtils.List(items(100));
        vc.setCellFactory(i -> new TestCell<>(i) {
            {
                setMinWidth(ThreadLocalRandom.current().nextInt(100, 1000));
            }
        });
        vc.setFitToViewport(false);*/

//        GridTestUtils.Grid vc = new GridTestUtils.Grid(items(500));
//        vc.setCellSize(150.0, 100.0);
//        vc.setColumnsNum(15);
//
//        Label label = new Label(LOREM);
//        label.setBorder(Border.stroke(ColorUtils.getRandomColor()));

        Rectangle rt = new Rectangle(3000, 3000);
        rt.setStyle("-fx-fill: linear-gradient(to bottom, red, yellow);");
        rt.setStroke(ColorUtils.getRandomColor());

        Label ll = new Label(LOREM);
        ll.setWrapText(true);


        VBox box = new VBox(IntStream.range(0, 500)
            .mapToObj(i -> "String " + i)
            .map(Label::new)
            .toArray(Node[]::new));

        //VFXScrollPane sp = vc.makeScrollable();
        VFXScrollPane sp = new VFXScrollPane(vc);
        sp.setSmoothScroll(true);
        //sp.setFitToWidth(true);
        //sp.setFitToHeight(true);
        //sp.setDragToScroll(true);
        //sp.setDragSmoothScroll(true);
        //sp.setShowButtons(true);
        //sp.setVBarPolicy(ScrollPaneEnums.ScrollBarPolicy.NEVER);
        //Platform.runLater(() -> sp.setPadding(uniform(24.0).get()));
        //sp.setScrollBarsGap(0.0);
        //sp.setAutoHideBars(true);
        //sp.setScrollBarsPos(Pos.TOP_RIGHT);
        //sp.setVUnitIncrement(0.025);
        //sp.setHUnitIncrement(0.05);
        VFXScrollable.bindSpeed(sp, ScrollParams.percentage(0.15), ScrollParams.percentage(0.15));

        sp.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() != MouseButton.SECONDARY) return;
            sp.setCompact(!sp.isCompact());
        });

        pane.getChildren().addAll(sp);
        Scene scene = new Scene(pane, 600, 400);
        stage.setScene(scene);
        stage.show();
        stage.centerOnScreen();

        debugView(null, pane);
        CSSFX.start(scene);
    }
}
