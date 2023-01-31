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

package app.cells.grid;

import app.cells.CellsDebugger;
import io.github.palexdev.materialfx.factories.InsetsFactory;
import io.github.palexdev.materialfx.font.FontResources;
import io.github.palexdev.mfxcore.collections.Grid;
import io.github.palexdev.mfxcore.utils.EnumUtils;
import io.github.palexdev.mfxcore.utils.fx.ColorUtils;
import io.github.palexdev.mfxresources.font.MFXFontIcon;
import io.github.palexdev.virtualizedfx.cell.GridCell;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.text.MessageFormat;

public class AlternativeGridCell extends VBox implements GridCell<Integer> {
	private Integer item;
	private int index;
	private Grid.Coordinates coordinates = Grid.Coordinates.of(-1, -1);

	private final Label label = new Label();
	private final MFXFontIcon icon = new MFXFontIcon();

	public AlternativeGridCell(Integer item) {
		updateItem(item);
		label.setPadding(InsetsFactory.of(5, 0, 5, 0));
		label.setAlignment(Pos.CENTER);
		icon.setSize(16);

		setSpacing(10);
		setAlignment(Pos.CENTER);
		getStyleClass().addAll("cell", "altCell");
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
		CellsDebugger.randBackground(this, 0.3, item, 1, 0);
	}

	@Override
	public void updateIndex(int index) {
		this.index = index;
		updateLabel();
	}

	@Override
	public void updateCoordinates(int row, int column) {
		coordinates = Grid.Coordinates.of(row, column);
		updateLabel();
	}

	@Override
	public String toString() {
		return MessageFormat.format("Item: {0}\nIndex: [L{1}\\R{2}\\C{3}]",
				item, index, coordinates.getRow(), coordinates.getColumn());
	}

	public void updateLabel() {
		label.setText(toString());
	}
}
