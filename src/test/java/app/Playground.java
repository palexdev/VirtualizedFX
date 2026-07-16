/*
 * Copyright (C) 2026 Parisi Alessandro - alessandro.parisi406@gmail.com
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
import io.github.palexdev.mfxcore.utils.fx.CSSFragment;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import static io.github.palexdev.mfxcore.utils.fx.InsetsUtils.uniform;
import static src.model.User.users;

public class Playground extends Application {
    @Override
    public void start(Stage stage) {
        StackPane root = new StackPane();

        TableTestUtils.Table table = new TableTestUtils.Table(users(100));
        table.getStylesheets().clear();
        root.getChildren().add(table.makeScrollable());

        CSSFragment.applyOn("""
            .vfx-scroll-pane > .viewport {
              -fx-padding: 32px;
            }
            
            .vfx-table {
              -fx-border-color: grey;
              -fx-border-radius: 16px;
              -vfx-clip-border-radius: 32px;
            }
            
            .columns {
              -fx-border-color: transparent transparent grey transparent;
              -fx-paddind: 0px 8px;
            }
            
            .vfx-row {
              -fx-background-color: white;
              -fx-background-insets: 0px 2px 0px 0px;
            }
            
            .vfx-row:nth-child(even) {
              -fx-background-color: lightgray;
            }
            
            .vfx-column {
              -vfx-enable-overlay: true;
              -vfx-overlay-on-header: true;
              -vfx-icon-alignment: LEFT;
            }
            
            .vfx-column:hover .overlay {
              -fx-background-color: rgba(0, 0, 255, 0.2);
            }
            """, root);

        Scene scene = new Scene(root, 1024, 720);
        stage.setScene(scene);
        stage.show();
    }
}
