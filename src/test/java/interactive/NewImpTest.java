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

package interactive;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXIconWrapper;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialogBuilder;
import io.github.palexdev.materialfx.dialogs.MFXStageDialog;
import io.github.palexdev.materialfx.effects.ConsumerTransition;
import io.github.palexdev.materialfx.effects.Interpolators;
import io.github.palexdev.materialfx.enums.ButtonType;
import io.github.palexdev.materialfx.enums.FloatMode;
import io.github.palexdev.materialfx.factories.InsetsFactory;
import io.github.palexdev.materialfx.font.MFXFontIcon;
import io.github.palexdev.materialfx.utils.AnimationUtils;
import io.github.palexdev.materialfx.utils.ColorUtils;
import io.github.palexdev.materialfx.utils.NodeUtils;
import io.github.palexdev.materialfx.utils.RandomUtils;
import io.github.palexdev.virtualizedfx.cell.Cell;
import io.github.palexdev.virtualizedfx.controls.VirtualScrollPane;
import io.github.palexdev.virtualizedfx.flow.VirtualFlow;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.scenicview.ScenicView;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NewImpTest extends Application {
	private final ObservableList<Integer> integers;
	private final VirtualFlow<Integer, Cell<Integer>> svf;

	public NewImpTest() {
		integers = IntStream.rangeClosed(0, 50)
				.boxed()
				.collect(Collectors.collectingAndThen(Collectors.toList(), FXCollections::observableArrayList));
		svf = new VirtualFlow<>(integers, SimpleCell::new);
		svf.setPrefSize(200, 400);
		svf.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
		svf.setCellSize(64);

		//svf.vPosProperty().addListener((observable, oldValue, newValue) -> System.out.println(newValue));
/*		svf.setOnScroll(event -> {
			if (event.getDeltaY() == 0) return;
			int mul = event.getDeltaY() < 0 ? 1 : -1;
			if (svf.getOrientation() == Orientation.VERTICAL) {
				svf.setVPos(svf.getVPos() + (10 * mul));
			} else {
				svf.setHPos(svf.getHPos() + (10 * mul));
			}
		});*/
		svf.setStyle("-fx-border-color: red");
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		BorderPane bp = new BorderPane();

		VirtualScrollPane vsp = VirtualScrollPane.wrap(svf);
		vsp.setSmoothScroll(true);
		vsp.setTrackSmoothScroll(true);
		vsp.setButtonsVisible(true);
		vsp.setUnitIncrement(0.02);

		FlowPane fp = buildButtonsPane(bp, primaryStage);
		bp.setTop(fp);
		bp.setCenter(vsp);

		Scene scene = new Scene(bp, 800, 800);
		primaryStage.setScene(scene);
		primaryStage.show();

		ScenicView.show(scene);
	}

	private FlowPane buildButtonsPane(Pane rootPane, Stage stage) {
		MFXButton updateInside = buildButton("Update Inside", event -> svf.getItems().set(5, RandomUtils.random.nextInt(0, 5)));
		MFXButton updateOutside = buildButton("Update Outside", event -> svf.getItems().set(25, RandomUtils.random.nextInt(0, 25)));
		MFXButton add = buildButton("Add", event -> svf.getItems().add(0, RandomUtils.random.nextInt(0, 1000)));
		MFXButton addMultiple = buildButton("Add Multiple", event -> svf.getItems().addAll(2, List.of(220, 220, 220)));
		MFXButton deleteMiddle = buildButton("Delete Middle", event -> svf.getItems().remove(2));
		MFXButton deleteSparse = buildButton("Delete Sparse", event -> svf.getItems().removeAll(
				integers.get(2),
				integers.get(5),
				integers.get(6),
				integers.get(8))
		);
		MFXButton deleteFirst = buildButton("Delete First", event -> svf.getItems().remove(0));
		MFXButton deleteLast = buildButton("Delete Last", event -> svf.getItems().remove(svf.getItems().size() - 1));
		MFXButton deleteAllVisible = buildButton("Delete All Visible", event -> svf.getItems().remove(0, 14));
		MFXButton setAll = buildButton("Set All", event -> {
			ObservableList<Integer> ni = FXCollections.observableArrayList();
			IntStream.rangeClosed(20, 25).forEach(ni::add);
			svf.getItems().setAll(ni);
		});
		MFXButton sort = buildButton("Sort Reverse", event -> svf.getItems().sort((o1, o2) -> Comparator.<Integer>reverseOrder().compare(o1, o2)));
		MFXButton clear = buildButton("Clear", event -> svf.getItems().clear());
		MFXButton changeSizeBy = buildButton("Change Size By", event -> {
			MFXTextField inputField = new MFXTextField();
			inputField.setAlignment(Pos.CENTER);
			inputField.setFloatMode(FloatMode.DISABLED);
			StackPane dc = new StackPane(inputField);
			dc.setAlignment(Pos.CENTER);

			MFXStageDialog sd = MFXGenericDialogBuilder.build()
					.setHeaderText("Change size by...")
					.setShowAlwaysOnTop(false)
					.setShowMinimize(false)
					.setContent(dc)
					.toStageDialogBuilder()
					.initModality(Modality.APPLICATION_MODAL)
					.initOwner(stage)
					.setAlwaysOnTop(true)
					.setOwnerNode(rootPane)
					.setCenterInOwnerNode(true)
					.setScrimOwner(true)
					.get();

			MFXButton okAction = buildButton("OK", e -> {
				try {
					double value = Double.parseDouble(inputField.getText());
					if (value == 0.0) return;

					if (svf.getOrientation() == Orientation.VERTICAL) {
						svf.setPrefHeight(svf.getHeight() + value);
					} else {
						svf.setPrefWidth(svf.getWidth() + value);
					}
					sd.close();
				} catch (Exception ignored) {
				}
			});
			MFXButton cancelAction = buildButton("Cancel", e -> sd.close());
			((MFXGenericDialog) sd.getContent()).addActions(okAction, cancelAction);

			sd.show();
		});
		MFXButton changeCellSizeTo = buildButton("Change Cell Size To", event -> {
			MFXTextField inputField = new MFXTextField();
			inputField.setAlignment(Pos.CENTER);
			inputField.setFloatMode(FloatMode.DISABLED);
			StackPane dc = new StackPane(inputField);
			dc.setAlignment(Pos.CENTER);

			MFXStageDialog sd = MFXGenericDialogBuilder.build()
					.setHeaderText("Change cell size to...")
					.setShowAlwaysOnTop(false)
					.setShowMinimize(false)
					.setContent(dc)
					.toStageDialogBuilder()
					.initModality(Modality.APPLICATION_MODAL)
					.initOwner(stage)
					.setAlwaysOnTop(true)
					.setOwnerNode(rootPane)
					.setCenterInOwnerNode(true)
					.setScrimOwner(true)
					.get();

			MFXButton okAction = buildButton("OK", e -> {
				try {
					double value = Double.parseDouble(inputField.getText());
					if (value == 0.0) return;
					svf.setCellSize(value);
					sd.close();
				} catch (Exception ignored) {
				}
			});
			MFXButton cancelAction = buildButton("Cancel", e -> sd.close());
			((MFXGenericDialog) sd.getContent()).addActions(okAction, cancelAction);

			sd.show();
		});
		MFXButton replaceList = buildButton("Replace List", e -> {
			ObservableList<Integer> newL = IntStream.rangeClosed(150, 300)
					.boxed()
					.collect(Collectors.collectingAndThen(Collectors.toList(), FXCollections::observableArrayList));
			svf.setItems(newL);
		});
		MFXButton factory = buildButton("Change Factory", e -> svf.setCellFactory(AdvancedCell::new));
		MFXButton orientation = buildButton("Change Orientation", e -> {
			Bounds bounds = svf.getLayoutBounds();
			Orientation newOr;
			if (svf.getOrientation() == Orientation.VERTICAL) {
				newOr = Orientation.HORIZONTAL;
				svf.setOrientation(newOr);
				svf.setPrefSize(bounds.getHeight(), bounds.getWidth());
				svf.setCellSize(100);
			} else {
				newOr = Orientation.VERTICAL;
				svf.setOrientation(newOr);
				svf.setPrefSize(bounds.getWidth(), bounds.getHeight());
				svf.setCellSize(64);
			}
		});
		MFXButton scrollBy = buildButton("Scroll by 64", e -> svf.scrollBy(64.0));
		MFXButton scrollToPixel = buildButton("Scroll to Pixel 64", e -> svf.scrollToPixel(64.0));
		MFXButton scrollTo = buildButton("Scroll to Index 20", e -> svf.scrollToIndex(20));
		MFXButton scrollToFirst = buildButton("Scroll to First", e -> svf.scrollToFirst());
		MFXButton scrollToLast = buildButton("Scroll to Last", e -> svf.scrollToLast());

		FlowPane fp = new FlowPane(20, 20);
		fp.getChildren().addAll(
				updateInside, updateOutside, add, addMultiple,
				deleteMiddle, deleteSparse, deleteFirst, deleteLast, deleteAllVisible,
				setAll, sort, clear, changeSizeBy, changeCellSizeTo, replaceList, factory,
				orientation, scrollBy, scrollTo, scrollToFirst, scrollToLast, scrollToPixel
		);
		return fp;
	}

	private MFXButton buildButton(String name, EventHandler<ActionEvent> action) {
		MFXButton button = new MFXButton(name);
		button.setButtonType(ButtonType.RAISED);
		button.setOnAction(action);
		return button;
	}

	private static class SimpleCell extends HBox implements Cell<Integer> {
		private final Label label;
		private boolean selected = false;

		private Integer item;
		private int index = 0;

		public SimpleCell(Integer item) {
			this.item = item;
			label = new Label(toString());
			label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
			setHgrow(label, Priority.ALWAYS);
			getChildren().setAll(label);

			setOnMousePressed(event -> {
				selected = !selected;
				Color color = selected ? Color.rgb(0, 0, 255, 0.1) : Color.TRANSPARENT;
				NodeUtils.setBackground(this, color);
			});
		}

		@Override
		public Node getNode() {
			return this;
		}

		@Override
		public void updateIndex(int index) {
			this.index = index;
			label.setText(toString());
		}

		@Override
		public void updateItem(Integer item) {
			if (item.equals(this.item)) return;
			this.item = item;
			label.setText(toString());

			animateBackground(this);
		}

		@Override
		public String toString() {
			return "T: String " + item + " I:" + index;
		}
	}

	private static class AdvancedCell extends HBox implements Cell<Integer> {
		private final Label label;
		private final MFXIconWrapper icon;

		private Integer item;
		private int index = 0;

		public AdvancedCell(Integer item) {
			this.item = item;
			label = new Label(toString());
			label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
			icon = new MFXIconWrapper(randomIcon(), 32.0);
			getChildren().setAll(icon, label);

			setAlignment(Pos.CENTER_LEFT);
			setPadding(InsetsFactory.all(5));
			setSpacing(10);
			setPrefWidth(300);

			NodeUtils.setBackground(this, ColorUtils.getRandomColor());
		}

		private Node randomIcon() {
			return MFXFontIcon.getRandomIcon(16, ColorUtils.getRandomColor());
		}

		@Override
		public Node getNode() {
			return this;
		}

		@Override
		public void updateIndex(int index) {
			this.index = index;
		}

		@Override
		public void updateItem(Integer item) {
			if (item.equals(this.item)) return;
			this.item = item;
			icon.setIcon(randomIcon());
			label.setText(toString());

			animateBackground(this);
		}

		@Override
		public String toString() {
			return "String " + item;
		}
	}

	static void animateBackground(Region region) {
		int r = RandomUtils.random.nextInt(0, 255);
		int g = RandomUtils.random.nextInt(0, 255);
		int b = RandomUtils.random.nextInt(0, 255);
		ConsumerTransition ct1 = ConsumerTransition.of(frac -> NodeUtils.setBackground(region, Color.rgb(r, g, b, frac)))
				.setDuration(300)
				.setInterpolatorFluent(Interpolators.INTERPOLATOR_V1);
		ConsumerTransition ct2 = ConsumerTransition.of(frac -> NodeUtils.setBackground(region, Color.rgb(r, g, b, 1.0 - frac)))
				.setDuration(300)
				.setInterpolatorFluent(Interpolators.INTERPOLATOR_V1);
		AnimationUtils.SequentialBuilder.build()
				.add(ct1)
				.add(ct2)
				.getAnimation()
				.play();
	}
}
