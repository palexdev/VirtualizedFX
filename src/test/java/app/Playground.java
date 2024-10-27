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
import io.github.palexdev.mfxcore.utils.fx.CSSFragment;
import io.github.palexdev.virtualizedfx.base.VFXScrollable;
import io.github.palexdev.virtualizedfx.controls.VFXScrollPane;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import static model.User.users;
import static utils.Utils.debugView;

public class Playground extends Application {

	@Override
	public void start(Stage primaryStage) {
		StackPane pane = new StackPane();
		pane.setAlignment(Pos.CENTER);

		TableTestUtils.Table container = new TableTestUtils.Table(users(100));

		VFXScrollPane sp = container.makeScrollable();
		sp.setSmoothScroll(true);
		sp.setDragToScroll(true);
		//sp.setDragSmoothScroll(true);
		//sp.setShowButtons(true);
		//sp.setVBarPolicy(ScrollBarPolicy.NEVER);
		VFXScrollable.setSpeed(sp, container, 0.5, 0.5, true);
		CSSFragment.Builder.build()
			.addSelector(".vfx-scroll-pane")
			.padding("5px")
			.closeSelector()
			.applyOn(sp);

		pane.getChildren().addAll(sp);
		Scene scene = new Scene(pane, 600, 400);
		primaryStage.setScene(scene);
		primaryStage.show();
		primaryStage.centerOnScreen();

		debugView(null, pane);
	}
}
