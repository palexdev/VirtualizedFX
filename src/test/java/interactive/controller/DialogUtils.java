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

package interactive.controller;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.dialogs.MFXDialogs;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialogBuilder;
import io.github.palexdev.materialfx.dialogs.MFXStageDialog;
import io.github.palexdev.materialfx.enums.DialogType;
import io.github.palexdev.materialfx.enums.FloatMode;
import javafx.geometry.Pos;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class DialogUtils {

	private DialogUtils() {
	}

	public static double getSizeFromUser(Pane owner, String title, String fieldText) {
		MFXTextField field = new MFXTextField("", "", fieldText);
		field.setFloatMode(FloatMode.INLINE);
		field.setPrefSize(240, 32);
		StackPane content = new StackPane(field);
		content.setAlignment(Pos.CENTER);

		MFXStageDialog sd = MFXGenericDialogBuilder.build()
				.setHeaderText(title)
				.setShowAlwaysOnTop(false)
				.setShowMinimize(false)
				.setContent(content)
				.toStageDialogBuilder()
				.initModality(Modality.WINDOW_MODAL)
				.initOwner(owner.getScene().getWindow())
				.setOwnerNode(owner)
				.setCenterInOwnerNode(true)
				.setScrimOwner(true)
				.get();

		AtomicReference<Double> val = new AtomicReference<>((double) 0);
		MFXButton okAction = new MFXButton("OK");
		okAction.setOnAction(event -> {
			try {
				val.set(Double.parseDouble(field.getText()));
				if (val.get() <= 0.0) {
					field.setFloatingText("Invalid value, > 0");
					return;
				}
				sd.close();
			} catch (NumberFormatException ignored) {
			}
		});
		MFXButton cancelAction = new MFXButton("Cancel");
		cancelAction.setOnAction(event -> {
			val.set(-1.0);
			sd.close();
		});
		((MFXGenericDialog) sd.getContent()).addActions(okAction, cancelAction);
		sd.showAndWait();
		return val.get();
	}

	public static int getIntFromUser(Pane owner, String title, String fieldText) {
		MFXTextField field = new MFXTextField("", "", fieldText);
		field.setFloatMode(FloatMode.INLINE);
		field.setPrefSize(240, 32);
		StackPane content = new StackPane(field);
		content.setAlignment(Pos.CENTER);

		MFXStageDialog sd = MFXGenericDialogBuilder.build()
				.setHeaderText(title)
				.setShowAlwaysOnTop(false)
				.setShowMinimize(false)
				.setContent(content)
				.toStageDialogBuilder()
				.initModality(Modality.WINDOW_MODAL)
				.initOwner(owner.getScene().getWindow())
				.setOwnerNode(owner)
				.setCenterInOwnerNode(true)
				.setScrimOwner(true)
				.get();

		AtomicInteger val = new AtomicInteger(0);
		MFXButton okAction = new MFXButton("OK");
		okAction.setOnAction(event -> {
			try {
				val.set(Integer.parseInt(field.getText()));
				if (val.get() <= 0) {
					field.setFloatingText("Invalid value, > 0");
					return;
				}
				sd.close();
			} catch (NumberFormatException ignored) {
			}
		});
		MFXButton cancelAction = new MFXButton("Cancel");
		cancelAction.setOnAction(event -> {
			val.set(-1);
			sd.close();
		});
		((MFXGenericDialog) sd.getContent()).addActions(okAction, cancelAction);
		sd.showAndWait();
		return val.get();
	}

	public static void showDialog(Pane owner, DialogType type, String title, String details) {
		MFXGenericDialogBuilder builder = switch (type) {
			case ERROR -> MFXDialogs.error();
			case WARNING -> MFXDialogs.warn();
			case INFO -> MFXDialogs.info();
			case GENERIC -> MFXGenericDialogBuilder.build();
		};
		MFXStageDialog sd = builder
				.setHeaderText(title)
				.setContentText(details)
				.setShowAlwaysOnTop(false)
				.setShowMinimize(false)
				.toStageDialogBuilder()
				.initModality(Modality.WINDOW_MODAL)
				.initOwner(owner.getScene().getWindow())
				.setOwnerNode(owner)
				.setCenterInOwnerNode(true)
				.setScrimOwner(true)
				.get();
		sd.showDialog();
	}
}
