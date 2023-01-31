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

package app.flow;

import io.github.palexdev.mfxcore.utils.EnumUtils;
import io.github.palexdev.virtualizedfx.cell.Cell;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

public class FlowTestParameters {
	public enum Mode {
		OLD_IMP, NEW_IMP, BOTH;

		@Override
		public String toString() {
			return name().charAt(0) + name().substring(1).replace("_", " ").toLowerCase();
		}
	}

	private final ReadOnlyObjectWrapper<Stage> stage = new ReadOnlyObjectWrapper<>();
	private final Pane root;
	private final Map<Integer, Class<? extends Cell<?>>> lastCells = new HashMap<>();
	private final ObjectProperty<Mode> mode = new SimpleObjectProperty<>(Mode.NEW_IMP);

	public FlowTestParameters(Pane root) {
		this.root = root;
	}

	public FlowTestParameters(Pane root, Class<? extends Cell<?>> initCell) {
		this.root = root;
		lastCells.put(0, initCell);
		lastCells.put(1, initCell);
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

	public Class<? extends Cell<?>> getLastCell(int id) {
		return lastCells.get(id);
	}

	public void setLastCell(int id, Class<? extends Cell<?>> clazz) {
		lastCells.put(id, clazz);
	}

	public Mode getMode() {
		return mode.get();
	}

	public ObjectProperty<Mode> modeProperty() {
		return mode;
	}

	public void setMode(Mode mode) {
		this.mode.set(mode);
	}

	public void switchMode() {
		Mode newMode = EnumUtils.next(Mode.class, getMode());
		if (newMode == Mode.BOTH) newMode = EnumUtils.next(Mode.class, newMode);
		setMode(newMode);
	}
}
