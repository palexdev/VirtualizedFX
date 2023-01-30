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

package app.grid;

import io.github.palexdev.virtualizedfx.cell.GridCell;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class GridTestParameters {
	private final ReadOnlyObjectWrapper<Stage> stage = new ReadOnlyObjectWrapper<>();
	private final Pane root;
	private Class<? extends GridCell<?>> lastCell;

	public GridTestParameters(Pane root) {
		this.root = root;
		stage.bind(root.sceneProperty().flatMap(Scene::windowProperty).map(w -> (Stage) w));
	}

	public GridTestParameters(Pane root, Class<? extends GridCell<?>> lastCell) {
		this(root);
		this.lastCell = lastCell;
	}

	public Stage getStage() {
		return stage.get();
	}

	public ReadOnlyObjectProperty<Stage> stageProperty() {
		return stage.getReadOnlyProperty();
	}

	public Pane getRoot() {
		return root;
	}

	public Class<? extends GridCell<?>> getLastCell() {
		return lastCell;
	}

	public void setLastCell(Class<? extends GridCell<?>> lastCell) {
		this.lastCell = lastCell;
	}
}
