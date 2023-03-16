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

package app.cells.flow;

import io.github.palexdev.mfxcore.controls.MFXIconWrapper;
import io.github.palexdev.mfxresources.fonts.IconsProviders;
import javafx.scene.Node;
import javafx.scene.paint.Color;

public class AlternativeCell extends CommonCell {
	private final MFXIconWrapper icon;

	public AlternativeCell(Integer item) {
		super(item);
		icon = new MFXIconWrapper(randomIcon(), 32.0);
		getChildren().setAll(icon, label);
		setSpacing(20);
	}

	private Node randomIcon() {
		return IconsProviders.FONTAWESOME_SOLID.randomIcon(18, Color.web("#35ce8f"));
	}

	@Override
	protected String dataToString() {
		return "Data: " + item;
	}
}
