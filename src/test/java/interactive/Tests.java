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

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.net.URL;

public enum Tests {
	FLOW("flow/FlowTest.fxml"),
	PFLOW("flow/PFlowTest.fxml"),
	GRID("grid/GridTest.fxml"),
	PGRID("grid/PGridTest.fxml"),
	TABLE("table/TableTest.fxml"),
	PTABLE("table/PTableTest.fxml");

	private final String fxml;

	Tests(String fxml) {
		this.fxml = fxml;
	}

	public String getFXML() {
		return fxml;
	}

	public URL toUrl() {
		return Tests.class.getResource(getFXML());
	}

	public String toExternalForm() {
		return toUrl().toExternalForm();
	}

	public Parent load() throws IOException {
		FXMLLoader loader = new FXMLLoader(toUrl());
		return loader.load();
	}
}
