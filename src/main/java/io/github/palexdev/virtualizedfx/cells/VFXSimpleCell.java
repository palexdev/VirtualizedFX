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

import io.github.palexdev.mfxcore.controls.Control;
import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.mfxcore.utils.converters.FunctionalStringConverter;
import io.github.palexdev.mfxcore.utils.fx.CSSFragment;
import javafx.util.StringConverter;

import java.util.List;
import java.util.function.Function;

/**
 * Simple extension of {@link VFXCellBase} which by default uses the skin {@link VFXLabeledCellSkin} to display its data
 * as text. This also expands the default style classes to be: '.cell-base' and '.cell'.
 * <p></p>
 * <b>Q:</b> Why not extend Labeled rather than Control? (Also considering that the default skin uses a Label)
 * <p>
 * <b>A:</b> Long story short, for flexibility. While it's true that in the vast majority of user cases, cells will just
 * display their data as a String, it's not always the case. Extending {@link Control} does not allow users
 * to set the label's properties (font, text color, etc.) directly (you can still do it by CSS or by using
 * {@link CSSFragment} in code!!), but it indeed makes the architecture more flexible. See also {@link VFXCellBase}
 * <p></p>
 * <b>Note:</b> to make this even more flexible, I decided to add a {@link StringConverter} to convert an item {@link T}
 * to the {@code String} visualized by the label. However, to make use of it, the cell extends inline the {@link VFXLabeledCellSkin}
 * here {@link #buildSkin()} to override its {@link VFXLabeledCellSkin#update()} method.
 */
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
    public List<String> defaultStyleClasses() {
        return List.of("cell-base", "cell");
    }

    @Override
    protected SkinBase<?, ?> buildSkin() {
        return new VFXLabeledCellSkin<>(this) {
            @Override
            protected void update() {
                T item = getItem();
                label.setText(converter.toString(item));
            }
        };
    }

    //================================================================================
    // Getters/Setters
    //================================================================================

    /**
     * @return the {@link StringConverter} used to convert an item {@link T} to a {@code String}
     */
    public StringConverter<T> getConverter() {
        return converter;
    }

    /**
     * Sets the {@link StringConverter} used to convert an extracted value {@link T} to a {@code String}
     */
    public VFXSimpleCell<T> setConverter(StringConverter<T> converter) {
        this.converter = converter;
        return this;
    }

    /**
     * Allows easily setting a {@link StringConverter} for the cell by just giving a {@link Function} as parameter,
     * makes use of {@link FunctionalStringConverter#to(Function)}.
     */
    public VFXSimpleCell<T> setConverter(Function<T, String> fn) {
        return setConverter(FunctionalStringConverter.to(fn));
    }
}
