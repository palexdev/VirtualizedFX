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

package interactive.others;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXFilterComboBox;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.dialogs.MFXDialogs;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialogBuilder;
import io.github.palexdev.materialfx.dialogs.MFXStageDialog;
import io.github.palexdev.materialfx.enums.DialogType;
import io.github.palexdev.materialfx.enums.FloatMode;
import javafx.geometry.Pos;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class DialogUtils {

	private DialogUtils() {
	}

	public static double getSizeFromUser(Pane owner, String title, String fieldText, Constraint<Double> constraint) {
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

		AtomicReference<Double> val = new AtomicReference<>((double) -1);
		MFXButton okAction = new MFXButton("OK");
		okAction.setOnAction(event -> {
			try {
				double parsed = Double.parseDouble(field.getText());
				val.set(parsed);
				if (!constraint.isValid(parsed)) {
					field.setFloatingText(constraint.message());
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
		sd.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
			if (event.getCode() == KeyCode.ENTER)
				okAction.fire();

			if (event.getCode() == KeyCode.ESCAPE)
				cancelAction.fire();
		});
		sd.showAndWait();
		return val.get();
	}

	public static int getIntFromUser(Pane owner, String title, String fieldText, Constraint<Integer> constraint) {
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

		AtomicInteger val = new AtomicInteger(-1);
		MFXButton okAction = new MFXButton("OK");
		okAction.setOnAction(event -> {
			try {
				int parsed = Integer.parseInt(field.getText());
				val.set(parsed);
				if (!constraint.isValid(parsed)) {
					field.setFloatingText(constraint.message());
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
		sd.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
			if (event.getCode() == KeyCode.ENTER)
				okAction.fire();

			if (event.getCode() == KeyCode.ESCAPE)
				cancelAction.fire();
		});
		sd.showAndWait();
		return val.get();
	}

	public static <T> T getChoice(Pane owner, String title, String comboText, Collection<T> choices) {
		MFXFilterComboBox<T> combo = new MFXFilterComboBox<>();
		combo.getItems().addAll(choices);
		combo.setFloatMode(FloatMode.INLINE);
		combo.setFloatingText(comboText);
		combo.setPrefSize(240, 32);
		StackPane content = new StackPane(combo);
		combo.setAlignment(Pos.CENTER);

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

		AtomicReference<T> val = new AtomicReference<>(null);
		MFXButton okAction = new MFXButton("OK");
		okAction.setOnAction(event -> {
			val.set(combo.getSelectedItem());
			sd.close();
		});
		MFXButton cancelAction = new MFXButton("Cancel");
		cancelAction.setOnAction(event -> {
			val.set(null);
			sd.close();
		});
		((MFXGenericDialog) sd.getContent()).addActions(okAction, cancelAction);
		sd.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
			if (event.getCode() == KeyCode.ENTER)
				okAction.fire();

			if (event.getCode() == KeyCode.ESCAPE)
				cancelAction.fire();
		});
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
