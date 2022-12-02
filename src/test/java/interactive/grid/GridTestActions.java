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

package interactive.grid;

import interactive.cells.CellsDebugger;
import interactive.cells.grid.AlternativeGridCell;
import interactive.cells.grid.SimpleGridCell;
import interactive.others.Constraint;
import interactive.others.Utils;
import io.github.palexdev.mfxcore.base.beans.Size;
import io.github.palexdev.mfxcore.collections.ObservableGrid;
import io.github.palexdev.mfxcore.utils.RandomUtils;
import io.github.palexdev.virtualizedfx.cell.GridCell;
import io.github.palexdev.virtualizedfx.controls.VirtualScrollPane;
import io.github.palexdev.virtualizedfx.grid.VirtualGrid;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import javafx.scene.control.Control;
import javafx.scene.layout.Region;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.IntStream;

import static interactive.others.DialogUtils.getIntFromUser;
import static interactive.others.DialogUtils.getSizeFromUser;

public enum GridTestActions {
	INIT_WITH_0((p, grid) -> {
		CellsDebugger.clear();
		grid.init(10, 10, 0);
	}),
	INIT_CUSTOM((p, grid) -> {
		CellsDebugger.clear();
		int rows = getIntFromUser(p.getRoot(), "Input Rows Number", "Type 0 to use current size", Constraint.of("Invalid, >= 0", i -> i >= 0));
		int columns = getIntFromUser(p.getRoot(), "Input Columns Number", "Type 0 to use current size", Constraint.of("Invalid, >= 0", i -> i >= 0));
		rows = (rows == 0) ? grid.getRowsNum() : rows;
		columns = (columns == 0) ? grid.getColumnsNum() : columns;
		if (rows == -1 || columns == -1) return;
		grid.init(rows, columns, (row, col) -> RandomUtils.random.nextInt(100));
	}),
	TRANSPOSE((p, grid) -> grid.getItems().transpose()),
	REPLACE_ELEMENT((p, grid) -> {
		int rNum = grid.getRowsNum();
		int cNum = grid.getColumnsNum();
		int row = getIntFromUser(p.getRoot(), "Element Replacement", "Input row index", Constraint.of("Invalid, [%d...%d]".formatted(0, rNum - 1), i -> i >= 0 && i < rNum));
		int column = getIntFromUser(p.getRoot(), "Element Replacement", "Input column index", Constraint.of("Invalid, [%d...%d]".formatted(0, cNum - 1), i -> i >= 0 && i < cNum));
		if (row > rNum - 1 || column > cNum - 1) return;
		grid.getItems().setElement(row, column, RandomUtils.random.nextInt(100));
	}),
	REPLACE_DIAGONAL((p, grid) -> {
		if (grid.getRowsNum() != grid.getColumnsNum()) return;
		List<Integer> diag = IntStream.range(0, grid.getRowsNum())
				.map(i -> RandomUtils.random.nextInt(100))
				.boxed().toList();
		grid.getItems().setDiagonal(diag);
	}),
	REPLACE_ROW((p, grid) -> {
		int rNum = grid.getRowsNum();
		int row = getIntFromUser(p.getRoot(), "Row Replacement", "Input row index", Constraint.of("Invalid, [%d...%d]".formatted(0, rNum - 1), i -> i >= 0 && i < rNum));
		if (row > rNum - 1) return;

		List<Integer> nRow = IntStream.range(0, grid.getColumnsNum())
				.map(i -> RandomUtils.random.nextInt(100))
				.boxed().toList();
		grid.getItems().setRow(row, nRow);
	}),
	REPLACE_COLUMN((p, grid) -> {
		int cNum = grid.getColumnsNum();
		int column = getIntFromUser(p.getRoot(), "Column Replacement", "Input column index", Constraint.of("Invalid, [%d...%d]".formatted(0, cNum - 1), i -> i >= 0 && i < cNum));
		if (column > cNum - 1) return;

		List<Integer> nCol = IntStream.range(0, grid.getRowsNum())
				.map(i -> RandomUtils.random.nextInt(100))
				.boxed().toList();
		grid.getItems().setColumn(column, nCol);
	}),
	ADD_ROW((p, grid) -> {
		int rNum = grid.getRowsNum();
		int index = getIntFromUser(p.getRoot(), "Add Row at...", "Input insertion index", Constraint.of("Invalid, [%d...%d]".formatted(0, rNum), i -> i >= 0 && i <= rNum));
		if (index == -1) return;

		List<Integer> nRow = IntStream.range(0, grid.getColumnsNum())
				.map(i -> RandomUtils.random.nextInt(100))
				.boxed().toList();
		grid.getItems().addRow(index, nRow);
	}),
	REMOVE_ROW((p, grid) -> {
		int rNum = grid.getRowsNum();
		if (rNum == 0) return;

		int index = getIntFromUser(p.getRoot(), "Remove Row at...", "Input removal index", Constraint.of("Invalid, [%d...%d]".formatted(0, rNum - 1), i -> i >= 0 && i < rNum));
		if (index == -1) return;
		grid.getItems().removeRow(index);
	}),
	ADD_COLUMN((p, grid) -> {
		int cNum = grid.getColumnsNum();
		int index = getIntFromUser(p.getRoot(), "Add Column at...", "Input insertion index", Constraint.of("Invalid...[%d...%d]".formatted(0, cNum), i -> i >= 0 && i <= cNum));
		if (index == -1) return;

		List<Integer> nCol = IntStream.range(0, grid.getRowsNum())
				.map(i -> RandomUtils.random.nextInt(100))
				.boxed().toList();
		grid.getItems().addColumn(index, nCol);
	}),
	REMOVE_COLUMN((p, grid) -> {
		int cNum = grid.getColumnsNum();
		if (cNum == 0) return;

		int index = getIntFromUser(p.getRoot(), "Remove Column at...", "Input removal index", Constraint.of("Invalid, [%d...%d]".formatted(0, cNum - 1), i -> i >= 0 && i < cNum));
		if (index == -1) return;
		grid.getItems().removeColumn(index);
	}),
	REPLACE_ALL((p, grid) -> {
		int rows = getIntFromUser(p.getRoot(), "Input Rows Number", "Type 0 to use current size", Constraint.of("Invalid, >= 0", i -> i >= 0));
		int columns = getIntFromUser(p.getRoot(), "Input Columns Number", "Type 0 to use current size", Constraint.of("Invalid, >= 0", i -> i >= 0));
		rows = (rows == 0) ? grid.getRowsNum() : rows;
		columns = (columns == 0) ? grid.getColumnsNum() : columns;
		if (rows == -1 || columns == -1) return;
		Integer[][] integers = Utils.randMatrix(rows, columns);
		grid.init(rows, columns, (row, column) -> integers[row][column]);
	}),
	REPLACE_ITEMS((p, grid) -> {
		int rows = getIntFromUser(p.getRoot(), "Input Rows Number", "Type 0 to use current size", Constraint.of("Invalid, >= 0", i -> i >= 0));
		int columns = getIntFromUser(p.getRoot(), "Input Columns Number", "Type 0 to use current size", Constraint.of("Invalid, >= 0", i -> i >= 0));
		rows = (rows == 0) ? grid.getRowsNum() : rows;
		columns = (columns == 0) ? grid.getColumnsNum() : columns;
		if (rows == -1 || columns == -1) return;
		Integer[][] integers = Utils.randMatrix(rows, columns);
		ObservableGrid<Integer> newGrid = ObservableGrid.fromMatrix(integers);
		grid.setItems(newGrid);
	}),
	CLEAR_ITEMS((p, grid) -> grid.clear()),
	CHANGE_VIEWPORT_SIZE((p, grid) -> {
		Region region = getRegion(grid);
		double width = getSizeFromUser(p.getRoot(), "Change Width To...", "Current Width: " + region.getWidth(), Constraint.of("Invalid!", i -> i > 0));
		double height = getSizeFromUser(p.getRoot(), "Change Height To...", "Current Height: " + region.getHeight(), Constraint.of("Invalid!", i -> i > 0));

		if (width != -1.0) {
			region.setPrefWidth(width);
			region.setMaxWidth(Region.USE_PREF_SIZE);
		}

		if (height != -1.0) {
			region.setPrefHeight(height);
			region.setMaxHeight(Region.USE_PREF_SIZE);
		}
	}),
	RESET_VIEWPORT_SIZE((p, grid) -> {
		VirtualScrollPane vsp = (VirtualScrollPane) grid.getParent().getParent();
		vsp.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
		vsp.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
	}),
	CHANGE_CELL_SIZE((p, grid) -> {
		Size cellSize = grid.getCellSize();
		Size newSize = Size.of(cellSize.getWidth(), cellSize.getHeight());

		double newWidth = getSizeFromUser(p.getRoot(), "Change Cell Width To...", "Current Width: " + cellSize.getWidth(), Constraint.of("Invalid!", i -> i > 0));
		if (newWidth > 0) newSize.setWidth(newWidth);

		double newHeight = getSizeFromUser(p.getRoot(), "Change Cell Height To...", "Current Height: " + cellSize.getHeight(), Constraint.of("Invalid!", i -> i > 0));
		if (newHeight > 0) newSize.setHeight(newHeight);

		grid.setCellSize(newSize);
	}),
	SWITCH_CELL_FACTORY((p, grid) -> {
		Function<Integer, GridCell<Integer>> factory;
		if (p.getLastCell() == SimpleGridCell.class) {
			factory = AlternativeGridCell::new;
			p.setLastCell(AlternativeGridCell.class);
		} else {
			factory = SimpleGridCell::new;
			p.setLastCell(SimpleGridCell.class);
		}
		grid.setCellFactory(factory);
	}),
	SCROLL_TO_ROW((p, grid) -> {
		int index = getIntFromUser(p.getRoot(), "Choose Row...", "Number of rows: " + grid.getRowsNum(), Constraint.of("Invalid!", i -> i >= 0));
		grid.scrollToRow(index);
	}),
	SCROLL_TO_COLUMN((p, grid) -> {
		int index = getIntFromUser(p.getRoot(), "Choose Column...", "Number of columns: " + grid.getColumnsNum(), Constraint.of("Invalid!", i -> i >= 0));
		grid.scrollToColumn(index);
	}),
	SCROLL_VERTICAL_BY((p, grid) -> {
		double amount = getSizeFromUser(p.getRoot(), "How Many Pixels To Scroll?", "Current Position: " + grid.getVPos(), Constraint.of("Invalid!", i -> i >= 0));
		grid.scrollBy(amount, Orientation.VERTICAL);
	}),
	SCROLL_VERTICAL_TO((p, grid) -> {
		double val = getSizeFromUser(p.getRoot(), "Input Pixel Value To Scroll To...", "Curren Position: " + grid.getVPos(), Constraint.of("Invalid!", i -> i >= 0));
		grid.scrollTo(val, Orientation.VERTICAL);
	}),
	SCROLL_HORIZONTAL_BY((p, grid) -> {
		double amount = getSizeFromUser(p.getRoot(), "How Many Pixels To Scroll?", "Current Position: " + grid.getHPos(), Constraint.of("Invalid!", i -> i >= 0));
		grid.scrollBy(amount, Orientation.HORIZONTAL);
	}),
	SCROLL_HORIZONTAL_TO((p, grid) -> {
		double val = getSizeFromUser(p.getRoot(), "Input Pixel Value To Scroll To...", "Curren Position: " + grid.getHPos(), Constraint.of("Invalid!", i -> i >= 0));
		grid.scrollTo(val, Orientation.HORIZONTAL);
	});

	private final BiConsumer<GridTestParameters, VirtualGrid<Integer, GridCell<Integer>>> action;

	GridTestActions(BiConsumer<GridTestParameters, VirtualGrid<Integer, GridCell<Integer>>> action) {
		this.action = action;
	}

	public void run(GridTestParameters parameters, VirtualGrid<Integer, GridCell<Integer>> grid) {
		action.accept(parameters, grid);
	}

	private static Region getRegion(Control control) {
		assert control != null;
		Parent parent = control;
		Set<String> classes;

		parent = parent.getParent();
		classes = Optional.ofNullable(parent)
				.map(p -> new HashSet<>(p.getStyleClass()))
				.orElse(new HashSet<>());
		while (!(parent instanceof Region) || (classes.contains("viewport") || classes.contains("container"))) {
			parent = parent.getParent();
			classes = Optional.ofNullable(parent)
					.map(p -> new HashSet<>(p.getStyleClass()))
					.orElse(new HashSet<>());
		}
		return (Region) parent;
	}

	@Override
	public String toString() {
		return name().charAt(0) + name().substring(1).replace("_", " ").toLowerCase();
	}
}
