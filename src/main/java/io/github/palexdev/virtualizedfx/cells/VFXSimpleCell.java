/*
 * Copyright (C) 2024 Parisi Alessandro - alessandro.parisi406@gmail.com
 * This file is part of VirtualizedFX (https://github.com/palexdev/VirtualizedFX)
 *
 * VirtualizedFX is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * VirtualizedFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with VirtualizedFX. If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.palexdev.virtualizedfx.cells;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import io.github.palexdev.mfxcore.controls.MFXControl;
import io.github.palexdev.mfxcore.controls.MFXSkinBase;
import io.github.palexdev.mfxcore.utils.converters.FunctionalStringConverter;
import io.github.palexdev.mfxcore.utils.fx.CSSFragment;
import javafx.scene.Node;
import javafx.util.StringConverter;

/// Simple extension of [VFXCellBase] which by default uses the skin [VFXLabeledCellSkin] to display its data
/// as text. This also expands the default style classes to be: '.cell-base' and '.cell'.
///
/// **Q:** Why not extend Labeled rather than Control? (Also considering that the default skin uses a Label)
///
/// **A:** Long story short, for flexibility. While it's true that in the vast majority of user cases, cells will just
/// display their data as a String, it's not always the case. Extending [MFXControl] does not allow users
/// to set the label's properties (font, text color, etc.) directly (you can still do it by CSS or by using
/// [CSSFragment] in code!!), but it indeed makes the architecture more flexible. See also [VFXCellBase]
///
/// **Note:** to make this even more flexible, I decided to add a [StringConverter] to convert an item [T]
/// to the `String` visualized by the label. However, to make use of it, the cell extends inline the [VFXLabeledCellSkin]
/// here [#buildSkin()] to override its [VFXLabeledCellSkin#update()] method.
public class VFXSimpleCell<T> extends VFXCellBase<T> {
    //================================================================================
    // Properties
    //================================================================================
    private StringConverter<T> converter;

    //================================================================================
    // Constructors
    //================================================================================
    public VFXSimpleCell(T item) {
        this(item, FunctionalStringConverter.to(t -> (t != null) ? t.toString() : ""));
    }

    public VFXSimpleCell(T item, StringConverter<T> converter) {
        super(item);
        this.converter = converter;
    }

    //================================================================================
    // Overridden Methods
    //================================================================================
    @Override
    public Supplier<MFXSkinBase<? extends Node>> defaultSkinFactory() {
        return () -> new VFXLabeledCellSkin<>(this) {
            @Override
            protected void update() {
                T item = getItem();
                label.setText(converter.toString(item));
            }
        };
    }

    @Override
    public List<String> defaultStyleClasses() {
        return List.of("cell-base", "cell");
    }

    //================================================================================
    // Getters/Setters
    //================================================================================

    /// @return the [StringConverter] used to convert an item [T] to a `String`
    public StringConverter<T> getConverter() {
        return converter;
    }

    /// Sets the [StringConverter] used to convert an extracted value [T] to a `String`
    public VFXSimpleCell<T> setConverter(StringConverter<T> converter) {
        this.converter = converter;
        return this;
    }

    /// Allows easily setting a [StringConverter] for the cell by just giving a [Function] as parameter,
    /// makes use of [FunctionalStringConverter#to(Function)].
    public VFXSimpleCell<T> setConverter(Function<T, String> fn) {
        return setConverter(FunctionalStringConverter.to(fn));
    }
}
