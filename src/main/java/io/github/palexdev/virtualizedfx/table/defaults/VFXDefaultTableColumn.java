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

package io.github.palexdev.virtualizedfx.table.defaults;

import java.util.List;
import java.util.function.Supplier;

import io.github.palexdev.mfxcore.base.properties.styleable.StyleableBooleanProperty;
import io.github.palexdev.mfxcore.controls.MFXSkinBase;
import io.github.palexdev.mfxcore.utils.fx.StyleUtils;
import io.github.palexdev.virtualizedfx.cells.base.VFXTableCell;
import io.github.palexdev.virtualizedfx.table.VFXTable;
import io.github.palexdev.virtualizedfx.table.VFXTableColumn;
import javafx.css.*;
import javafx.geometry.HPos;
import javafx.scene.Node;

/**
 * Concrete and simple implementation of {@link VFXTableColumn}. Has its own skin: {@link VFXDefaultTableColumnSkin}.
 * <p>
 * These are the features this default implementation offers:
 * <p> - the {@link #iconAlignmentProperty()} allows you to specify the column's icon position
 * <p> - the {@link #enableOverlayProperty()} makes the column display an extra node which can be used to indicate
 * selection or hovering. By default, the node is not visible; you'll have to define its style in CSS.
 * This behavior is defined in the default skin.
 * <p> - the {@link #overlayOnHeaderProperty()} makes the aforementioned node cover the column's header too
 */
public class VFXDefaultTableColumn<T, C extends VFXTableCell<T>> extends VFXTableColumn<T, C> {
    //================================================================================
    // Properties
    //================================================================================
    public static final PseudoClass DRAGGED = PseudoClass.getPseudoClass("dragged");

    //================================================================================
    // Constructors
    //================================================================================
    public VFXDefaultTableColumn() {}

    public VFXDefaultTableColumn(String text) {
        super(text);
    }

    public VFXDefaultTableColumn(String text, Node graphic) {
        super(text, graphic);
    }

    //================================================================================
    // Overridden Methods
    //================================================================================
    @Override
    public Supplier<MFXSkinBase<? extends Node>> defaultSkinFactory() {
        return () -> new VFXDefaultTableColumnSkin<>(this);
    }

    //================================================================================
    // Styleable Properties
    //================================================================================
    private final StyleableObjectProperty<HPos> iconAlignment = new SimpleStyleableObjectProperty<>(
        StyleableProperties.ICON_ALIGNMENT,
        this,
        "iconAlignment",
        HPos.RIGHT
    );

    private final StyleableBooleanProperty enableOverlay = new StyleableBooleanProperty(
        StyleableProperties.ENABLE_OVERLAY,
        this,
        "enableOverlay",
        true
    );

    private final StyleableBooleanProperty overlayOnHeader = new StyleableBooleanProperty(
        StyleableProperties.OVERLAY_ON_HEADER,
        this,
        "overlayOnHeader",
        false
    );

    public HPos getIconAlignment() {
        return iconAlignment.get();
    }

    /**
     * Specifies the side on which the icon will be placed.
     * <p></p>
     * By setting the alignment to {@link HPos#CENTER} the default skin, {@link VFXDefaultTableColumnSkin}, will hide the
     * text and show only the icon at the center.
     * <p></p>
     * This is settable via CSS with the "-vfx-icon-alignment" property.
     */
    public StyleableObjectProperty<HPos> iconAlignmentProperty() {
        return iconAlignment;
    }

    public void setIconAlignment(HPos iconAlignment) {
        this.iconAlignment.set(iconAlignment);
    }

    public boolean isEnableOverlay() {
        return enableOverlay.get();
    }

    /**
     * Specifies whether the default skin should enable the overlay.
     * <p></p>
     * {@link VFXTable} is organized by rows. This means that by default, there is no way in the UI to display
     * when a column is selected or hovered by the mouse. The default skin allows to do this by adding an extra node that
     * extends from the column all the way down to the table's bottom. This allows doing cool tricks with CSS.
     * <p></p>
     * One thing to keep in mind, though, is that if you define a background color for the overlay, make sure that it is
     * opaque otherwise it will end up covering the cells.
     * <p></p>
     * This is also settable via CSS with the "-vfx-enable-overlay" property.
     */
    public StyleableBooleanProperty enableOverlayProperty() {
        return enableOverlay;
    }

    public void setEnableOverlay(boolean enableOverlay) {
        this.enableOverlay.set(enableOverlay);
    }

    public boolean isOverlayOnHeader() {
        return overlayOnHeader.get();
    }

    /**
     * Specifies whether the overlay should also cover the header of the column,
     * the part where the text and the icon reside.
     * <p></p>
     * This is also settable via CSS with the "-vfx-overlay-on-header" property.
     */
    public StyleableBooleanProperty overlayOnHeaderProperty() {
        return overlayOnHeader;
    }

    public void setOverlayOnHeader(boolean overlayOnHeader) {
        this.overlayOnHeader.set(overlayOnHeader);
    }

    //================================================================================
    // CssMetaData
    //================================================================================
    private static class StyleableProperties {
        private static final StyleablePropertyFactory<VFXDefaultTableColumn<?, ?>> FACTORY = new StyleablePropertyFactory<>(VFXTableColumn.getClassCssMetaData());
        private static final List<CssMetaData<? extends Styleable, ?>> cssMetaDataList;

        private static final CssMetaData<VFXDefaultTableColumn<?, ?>, HPos> ICON_ALIGNMENT =
            FACTORY.createEnumCssMetaData(
                HPos.class,
                "-vfx-icon-alignment",
                VFXDefaultTableColumn::iconAlignmentProperty,
                HPos.RIGHT
            );

        private static final CssMetaData<VFXDefaultTableColumn<?, ?>, Boolean> ENABLE_OVERLAY =
            FACTORY.createBooleanCssMetaData(
                "-vfx-enable-overaly",
                VFXDefaultTableColumn::enableOverlayProperty,
                true
            );

        private static final CssMetaData<VFXDefaultTableColumn<?, ?>, Boolean> OVERLAY_ON_HEADER =
            FACTORY.createBooleanCssMetaData(
                "-vfx-overlay-on-header",
                VFXDefaultTableColumn::overlayOnHeaderProperty,
                false
            );

        static {
            cssMetaDataList = StyleUtils.cssMetaDataList(
                VFXTableColumn.getClassCssMetaData(),
                ICON_ALIGNMENT, ENABLE_OVERLAY, OVERLAY_ON_HEADER
            );
        }
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return getClassCssMetaData();
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.cssMetaDataList;
    }
}
