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

package app.table;

import io.github.palexdev.virtualizedfx.cell.TableCell;
import io.github.palexdev.virtualizedfx.table.TableColumn;
import io.github.palexdev.virtualizedfx.table.VirtualTable;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.util.*;

@SuppressWarnings({"rawtypes", "unchecked"})
public class TableTestParameters<T> {
	private final ReadOnlyObjectWrapper<Stage> stage = new ReadOnlyObjectWrapper<>();
	private final Pane root;
	private final Map<TableColumn<T, ? extends TableCell<T>>, Class<? extends TableCell<T>>> lastCellMap = new HashMap<>();
	private final List<TableColumn<T, ? extends TableCell<T>>> columns = new ArrayList<>();
	private Comparator<T> lastComparator;

	public TableTestParameters(Pane root) {
		this.root = root;
		stage.bind(root.sceneProperty().flatMap(Scene::windowProperty).map(w -> ((Stage) w)));
	}

	public void initCellsMap(VirtualTable<T> table, Class cf) {
		for (TableColumn<T, ? extends TableCell<T>> column : table.getColumns()) {
			setLastCell(column, cf);
		}
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

	public Map<TableColumn<T, ? extends TableCell<T>>, Class<? extends TableCell<T>>> getLastCellMap() {
		return lastCellMap;
	}

	public Class getLastCell(TableColumn<T, ? extends TableCell<T>> column) {
		return lastCellMap.getOrDefault(column, null);
	}

	public void setLastCell(TableColumn<T, ? extends TableCell<T>> column, Class cf) {
		lastCellMap.put(column, cf);
	}

	public void setColumns(Collection<TableColumn<T, ? extends TableCell<T>>> columns) {
		this.columns.clear();
		this.columns.addAll(columns);
	}

	public List<TableColumn<T, ? extends TableCell<T>>> getColumns() {
		return columns;
	}

	public Comparator<T> getLastComparator() {
		return lastComparator;
	}

	public void setLastComparator(Comparator<T> lastComparator) {
		this.lastComparator = lastComparator;
	}
}
