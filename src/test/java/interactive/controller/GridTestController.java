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

import io.github.palexdev.materialfx.controls.MFXCheckbox;
import io.github.palexdev.materialfx.controls.MFXFilterComboBox;
import io.github.palexdev.materialfx.factories.InsetsFactory;
import io.github.palexdev.mfxcore.base.beans.SizeBean;
import io.github.palexdev.mfxcore.collections.Grid.Coordinate;
import io.github.palexdev.mfxcore.collections.ObservableGrid;
import io.github.palexdev.mfxcore.controls.MFXIconWrapper;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.mfxcore.utils.EnumUtils;
import io.github.palexdev.mfxcore.utils.RandomUtils;
import io.github.palexdev.mfxcore.utils.fx.ColorUtils;
import io.github.palexdev.mfxcore.utils.fx.RegionUtils;
import io.github.palexdev.mfxcore.utils.fx.StyleUtils;
import io.github.palexdev.mfxresources.font.FontResources;
import io.github.palexdev.mfxresources.font.MFXFontIcon;
import io.github.palexdev.virtualizedfx.cell.GridCell;
import io.github.palexdev.virtualizedfx.controls.VirtualScrollPane;
import io.github.palexdev.virtualizedfx.grid.VirtualGrid;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.net.URL;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static interactive.controller.DialogUtils.getIntFromUser;
import static interactive.controller.DialogUtils.getSizeFromUser;

public class GridTestController implements Initializable {
	private static Pane root;
	private static Class<? extends GridCell<Integer>> lastFactory;
	private final Stage stage;
	private final VirtualGrid<Integer, GridCell<Integer>> grid;

	@FXML StackPane rootPane;
	@FXML GridPane header;
	@FXML Label headerLabel;
	@FXML MFXFilterComboBox<Action> actionsBox;
	@FXML MFXIconWrapper runIcon;
	@FXML StackPane contentPane;
	@FXML MFXCheckbox ssCheck;

	private double xOffset;
	private double yOffset;
	private boolean canDrag;

	public GridTestController(Stage stage) {
		this.stage = stage;
		this.grid = new VirtualGrid<>(
				ObservableGrid.fromList(List.of(), 0),
				SimpleGridCell::new
		);
		lastFactory = SimpleGridCell.class;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// Static fields
		root = rootPane;

		// Grid
		ObservableGrid<Integer> data = ObservableGrid.fromList(
				IntStream.range(0, 100).boxed().collect(Collectors.toList()),
				10
		);
		grid.setItems(data);
		grid.setCellSize(SizeBean.of(200, 200));

		// Header
		headerLabel.textProperty().bind(stage.titleProperty());
		header.setOnMousePressed(event -> {
			if (event.getPickResult().getIntersectedNode().getClass() == MFXFontIcon.class) {
				canDrag = false;
				return;
			}
			xOffset = stage.getX() - event.getScreenX();
			yOffset = stage.getY() - event.getScreenY();
			canDrag = true;
		});
		header.setOnMouseDragged(event -> {
			if (canDrag) {
				stage.setX(event.getScreenX() + xOffset);
				stage.setY(event.getScreenY() + yOffset);
			}
		});

		// Actions Pane
		actionsBox.setItems(FXCollections.observableArrayList(Action.values()));
		actionsBox.selectFirst();

		runIcon.defaultRippleGeneratorBehavior();
		RegionUtils.makeRegionCircular(runIcon);

		// Content
		VirtualScrollPane vsp = VirtualScrollPane.wrap(grid);
		Runnable speedAction = () -> {
			SizeBean size = grid.getCellSize();
			VirtualScrollPane.setVSpeed(vsp, 0.25 * size.getHeight(), grid.getCellSize().getHeight() * 0.75, 0.5 * size.getHeight());
			VirtualScrollPane.setHSpeed(vsp, 0.25 * size.getWidth(), grid.getCellSize().getWidth() * 0.75, 0.5 * size.getWidth());
		};
		When.onInvalidated(grid.cellSizeProperty())
				.then(i -> speedAction.run())
				.executeNow()
				.listen();
		contentPane.getChildren().setAll(vsp);

		// Settings
		ssCheck.selectedProperty().addListener((observable, oldValue, newValue) -> {
			vsp.setSmoothScroll(newValue);
			vsp.setTrackSmoothScroll(newValue);
		});

		grid.stateProperty().addListener((observable, oldValue, newValue) -> System.out.println(newValue.getColumnsRange()));
	}

	@FXML
	void runAction() {
		Optional.ofNullable(actionsBox.getSelectedItem())
				.ifPresent(action -> action.run(grid));
	}

	@FXML
	void exit() {
		System.exit(0);
	}

	@FXML
	void minimize() {
		stage.setIconified(!stage.isIconified());
	}

	@FXML
	void maximize() {
		stage.setMaximized(!stage.isMaximized());
	}

	//================================================================================
	// Inner Classes
	//================================================================================
	@FunctionalInterface
	private interface TestAction<T> {
		void accept(VirtualGrid<T, GridCell<T>> grid);

		default Consumer<VirtualGrid<T, GridCell<T>>> andThen(TestAction<T> action) {
			Objects.requireNonNull(action);

			return (grid) -> {
				accept(grid);
				action.accept(grid);
			};
		}
	}

	private enum Action {
		INIT_WITH_0(grid -> execute(() -> grid.init(10, 10, 0))),
		INIT_CUSTOM(grid -> execute(() -> {
			int rows = getIntFromUser(root, "Input Rows Number", "Type 0 to use current size");
			int columns = getIntFromUser(root, "Input Columns Number", "Type 0 to use current size");
			grid.init(rows, columns, (row, col) -> RandomUtils.random.nextInt(100));
		})),
		TRANSPOSE(grid -> execute(() -> grid.getItems().transpose())),
		SET_ELEMENT_AT_00(grid -> execute(() -> grid.getItems().setElement(0, -1))),
		SET_ELEMENT_AT_33(grid -> execute(() -> grid.getItems().setElement(3, 3, -1))),
		SET_ELEMENT_AT_99(grid -> execute(() -> grid.getItems().setElement(9, 9, -1))),
		SET_ROW_AT_0(grid -> execute(() -> grid.getItems().setRow(0, getRandInt(grid.getColumnsNum())))),
		SET_ROW_AT_3(grid -> execute(() -> grid.getItems().setRow(3, getRandInt(grid.getColumnsNum())))),
		SET_ROW_AT_9(grid -> execute(() -> grid.getItems().setRow(9, getRandInt(grid.getColumnsNum())))),
		SET_COL_AT_0(grid -> execute(() -> grid.getItems().setColumn(0, getRandInt(grid.getRowsNum())))),
		SET_COL_AT_3(grid -> execute(() -> grid.getItems().setColumn(3, getRandInt(grid.getRowsNum())))),
		SET_COL_AT_9(grid -> execute(() -> grid.getItems().setColumn(9, getRandInt(grid.getRowsNum())))),
		SET_DIAGONAL(grid -> execute(() -> grid.getItems().setDiagonal(getRandInt(grid.getRowsNum())))),
		ADD_ROW_AT_0(grid -> execute(() -> grid.getItems().addRow(0, getRandInt(grid.getColumnsNum())))),
		ADD_ROW_AT_MIDDLE(grid -> execute(() -> grid.getItems().addRow(grid.getRowsNum() / 2, getRandInt(grid.getColumnsNum())))),
		ADD_ROW_AT_END(grid -> execute(() -> grid.getItems().addRow(grid.getRowsNum(), getRandInt(grid.getColumnsNum())))),
		ADD_COLUMN_AT_0(grid -> execute(() -> grid.getItems().addColumn(0, getRandInt(grid.getRowsNum())))),
		ADD_COLUMN_AT_MIDDLE(grid -> execute(() -> grid.getItems().addColumn(grid.getColumnsNum() / 2, getRandInt(grid.getRowsNum())))),
		ADD_COLUMN_AT_END(grid -> execute(() -> grid.getItems().addColumn(grid.getColumnsNum(), getRandInt(grid.getRowsNum())))),
		REMOVE_ROW_AT_0(grid -> execute(() -> grid.getItems().removeRow(0))),
		REMOVE_ROW_AT_MIDDLE(grid -> execute(() -> grid.getItems().removeRow(grid.getRowsNum() / 2))),
		REMOVE_ROW_AT_END(grid -> execute(() -> grid.getItems().removeRow(grid.getRowsNum() - 1))),
		REMOVE_COLUMN_AT_0(grid -> execute(() -> grid.getItems().removeColumn(0))),
		REMOVE_COLUMN_AT_MIDDLE(grid -> execute(() -> grid.getItems().removeColumn(grid.getColumnsNum() / 2))),
		REMOVE_COLUMN_AT_END(grid -> execute(() -> grid.getItems().removeColumn(grid.getColumnsNum() - 1))),
		REPLACE_ALL(grid -> execute(() -> {
			Integer[][] integers = getRandMatrix(3, 3);
			grid.init(3, 3, (row, column) -> integers[row][column]);
		})),
		REPLACE_ITEMS(grid -> execute(() -> {
			Integer[][] integers = getRandMatrix(4, 5);
			ObservableGrid<Integer> newGrid = ObservableGrid.fromMatrix(integers);
			grid.setItems(newGrid);
		})),
		CLEAR_ITEMS(grid -> execute(grid::clear)),
		CHANGE_VIEWPORT_SIZE_TO(grid -> execute(() -> {
			VirtualScrollPane vsp = (VirtualScrollPane) grid.getParent().getParent();
			double width = getSizeFromUser(root, "Change Width To...", "Current Width: " + vsp.getWidth());
			double height = getSizeFromUser(root, "Change Height To...", "Current Height: " + vsp.getHeight());

			if (width != -1.0) {
				vsp.setPrefWidth(width);
				vsp.setMaxWidth(Region.USE_PREF_SIZE);
			}

			if (height != -1.0) {
				vsp.setPrefHeight(height);
				vsp.setMaxHeight(Region.USE_PREF_SIZE);
			}
		})),
		RESET_VIEWPORT_SIZE(grid -> execute(() -> {
			VirtualScrollPane vsp = (VirtualScrollPane) grid.getParent().getParent();
			vsp.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
			vsp.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
		})),
		CHANGE_CELL_SIZES_TO(grid -> execute(() -> {
			SizeBean cellSize = grid.getCellSize();
			SizeBean newSize = SizeBean.of(cellSize.getWidth(), cellSize.getHeight());

			double newWidth = getSizeFromUser(root, "Change Cell Width To...", "Current Width: " + cellSize.getWidth());
			if (newWidth > 0) newSize.setWidth(newWidth);

			double newHeight = getSizeFromUser(root, "Change Cell Height To...", "Current Height: " + cellSize.getHeight());
			if (newHeight > 0) newSize.setHeight(newHeight);

			grid.setCellSize(newSize);
		})),
		SWITCH_CELL_FACTORY(grid -> execute(() -> {
			Function<Integer, GridCell<Integer>> factory;
			if (lastFactory == SimpleGridCell.class) {
				factory = i -> new AlternativeGridCell(grid, i);
				lastFactory = AlternativeGridCell.class;
			} else {
				factory = SimpleGridCell::new;
				lastFactory = SimpleGridCell.class;
			}
			grid.setCellFactory(factory);
		})),
		SCROLL_TO_ROW(grid -> execute(() -> {
			int index = getIntFromUser(root, "Choose Row...", "Number of rows: " + grid.getRowsNum());
			grid.scrollToRow(index);
		})),
		SCROLL_TO_COLUMN(grid -> execute(() -> {
			int index = getIntFromUser(root, "Choose Column...", "Number of columns: " + grid.getColumnsNum());
			grid.scrollToColumn(index);
		})),
		SCROLL_VERTICAL_BY(grid -> execute(() -> {
			double amount = getSizeFromUser(root, "How Many Pixels To Scroll?", "Current Position: " + grid.getVPos());
			grid.scrollBy(amount, Orientation.VERTICAL);
		})),
		SCROLL_VERTICAL_TO(grid -> execute(() -> {
			double val = getSizeFromUser(root, "Input Pixel Value To Scroll To...", "Curren Position: " + grid.getVPos());
			grid.scrollTo(val, Orientation.VERTICAL);
		})),
		SCROLL_HORIZONTAL_BY(grid -> execute(() -> {
			double amount = getSizeFromUser(root, "How Many Pixels To Scroll?", "Current Position: " + grid.getHPos());
			grid.scrollBy(amount, Orientation.HORIZONTAL);
		})),
		SCROLL_HORIZONTAL_TO(grid -> execute(() -> {
			double val = getSizeFromUser(root, "Input Pixel Value To Scroll To...", "Curren Position: " + grid.getHPos());
			grid.scrollTo(val, Orientation.HORIZONTAL);
		}));

		private final TestAction<Integer> action;

		Action(TestAction<Integer> action) {
			this.action = action;
		}

		public TestAction<Integer> getAction() {
			return action;
		}

		public void run(VirtualGrid<Integer, GridCell<Integer>> grid) {
			action.accept(grid);
		}

		private static void execute(Runnable action) {
			action.run();
		}

		private static List<Integer> getRandInt(int num) {
			return IntStream.range(0, num)
					.mapToObj(i -> -RandomUtils.random.nextInt(100))
					.collect(Collectors.toList());
		}

		private static Integer[][] getRandMatrix(int rows, int columns) {
			Integer[][] matrix = new Integer[rows][columns];
			for (int i = 0; i < rows; i++) {
				for (int j = 0; j < columns; j++) {
					matrix[i][j] = RandomUtils.random.nextInt(100);
				}
			}
			return matrix;
		}

		@Override
		public String toString() {
			return name().charAt(0) + name().substring(1).replace("_", " ").toLowerCase();
		}
	}

	public static class SimpleGridCell extends HBox implements GridCell<Integer> {
		private Integer item;
		private int index;

		private final Label label = new Label();

		public SimpleGridCell(Integer item) {
			updateItem(item);
			label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
			label.setAlignment(Pos.CENTER);

			setHgrow(label, Priority.ALWAYS);
			getChildren().setAll(label);
		}

		@Override
		public Node getNode() {
			return this;
		}

		@Override
		public void updateItem(Integer item) {
			this.item = item;
			label.setText(toString());
			randBackground(this, 0.5f);
		}

		@Override
		public void updateIndex(int index) {
			this.index = index;
			label.setText(toString());
		}

		@Override
		public String toString() {
			return MessageFormat.format("Cell [{0}, {1}]", item, index);
		}

		public static void randBackground(Region r, float opacity) {
			StyleUtils.setBackground(r,
					Color.color(
							RandomUtils.random.nextFloat(),
							RandomUtils.random.nextFloat(),
							RandomUtils.random.nextFloat(),
							opacity
					)
			);
		}
	}

	public static class AlternativeGridCell extends VBox implements GridCell<Integer> {
		private final VirtualGrid<Integer, ?> grid;
		private Integer item;
		private int index;
		private Coordinate coordinates = Coordinate.of(-1, -1);

		private final Label label = new Label();
		private final MFXFontIcon icon = new MFXFontIcon();

		public AlternativeGridCell(VirtualGrid<Integer, ?> grid, Integer item) {
			this.grid = grid;
			updateItem(item);
			label.setPadding(InsetsFactory.of(5, 0, 5, 0));
			label.setAlignment(Pos.CENTER);
			icon.setSize(24);

			setSpacing(10);
			setAlignment(Pos.CENTER);
			getChildren().setAll(label, icon);
		}

		@Override
		public Node getNode() {
			return this;
		}

		@Override
		public void updateItem(Integer item) {
			this.item = item;
			updateLabel();
			icon.setDescription(EnumUtils.randomEnum(FontResources.class).getDescription());
			icon.setColor(ColorUtils.getRandomColor());
			SimpleGridCell.randBackground(this, 0.3f);
		}

		@Override
		public void updateIndex(int index) {
			this.index = index;
			updateLabel();
		}

		@Override
		public void updateCoordinates(int linearIndex) {
			coordinates = Coordinate.linear(linearIndex, grid.getColumnsNum());
			updateLabel();
		}

		@Override
		public void updateCoordinates(int row, int column) {
			coordinates = Coordinate.of(row, column);
			updateLabel();
		}

		@Override
		public String toString() {
			return MessageFormat.format("Cell Item: {0}\nCell Index: [L{1}\\R{2}\\C{3}]",
					item, index, coordinates.getRow(), coordinates.getColumn());
		}

		public void updateLabel() {
			label.setText(toString());
		}
	}
}
