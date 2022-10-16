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

package interactive.cells.grid;

import interactive.cells.CellsDebugger;
import io.github.palexdev.virtualizedfx.cell.GridCell;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.text.MessageFormat;

public class SimpleGridCell extends HBox implements GridCell<Integer> {
	private Integer item;
	private int index;

	private final Label label = new Label();

	public SimpleGridCell(Integer item) {
		updateItem(item);
		label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		label.setAlignment(Pos.CENTER);

		setHgrow(label, Priority.ALWAYS);
		getStyleClass().add("cell");
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
		CellsDebugger.randBackground(this, 0.5f, item, 1, 0);
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
}
