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

import io.github.palexdev.mfxcore.base.beans.Position;
import io.github.palexdev.mfxcore.utils.fx.LayoutUtils;
import io.github.palexdev.mfxresources.builders.IconWrapperBuilder;
import io.github.palexdev.mfxresources.fonts.IconsProviders;
import io.github.palexdev.mfxresources.fonts.MFXIconWrapper;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.paint.Color;
import javafx.util.Pair;

import java.text.MessageFormat;
import java.util.Optional;

public class DetailedCell extends CommonCell {
    private final MFXIconWrapper icon;

    public DetailedCell(Integer item) {
        super(item);

        icon = IconWrapperBuilder.build()
                .setIcon(IconsProviders.FONTAWESOME_SOLID.randomIcon(12, Color.LAWNGREEN))
                .setSize(16)
                .setManaged(false)
                .get();

        layoutBoundsProperty().addListener((observable, oldValue, newValue) -> {
            Pair<Double, Double> o = Optional.ofNullable(oldValue)
                    .map(b -> new Pair<>(b.getWidth(), b.getHeight()))
                    .orElse(new Pair<>(-1.0, -1.0));
            Pair<Double, Double> n = new Pair<>(newValue.getWidth(), newValue.getHeight());
            if (!n.equals(o)) label.setText(dataToString());
        });

        getChildren().addAll(icon);
    }

    protected String dataToString() {
        int spaces = (item > 0) ?
                10 - (int) (Math.log10(item) + 1) :
                9;
        return "Data: " + item +
                " ".repeat(spaces) +
                "Index: " + index +
                " ".repeat(spaces) +
                "Size[W/H]: " + MessageFormat.format("[{0}/{1}]", getWidth(), getHeight());
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        double w = getWidth();
        double h = getHeight();

        Position pos = LayoutUtils.computePosition(
                this, icon,
                0, 0, w - 8.0, h, 0,
                getPadding(),
                HPos.RIGHT, VPos.CENTER
        );
        icon.resizeRelocate(pos.getX(), pos.getY(), icon.getSize(), icon.getSize());
    }
}
