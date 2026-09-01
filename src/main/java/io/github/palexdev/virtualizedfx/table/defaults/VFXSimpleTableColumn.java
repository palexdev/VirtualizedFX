/*
 * Copyright (C) 2026 Parisi Alessandro - alessandro.parisi406@gmail.com
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
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableObjectProperty;
import io.github.palexdev.mfxcore.controls.MFXSkinBase;
import io.github.palexdev.mfxcore.utils.fx.StyleUtils;
import io.github.palexdev.virtualizedfx.cells.base.VFXTableCell;
import io.github.palexdev.virtualizedfx.table.VFXTableColumn;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleablePropertyFactory;
import javafx.geometry.HPos;
import javafx.scene.Node;

public class VFXSimpleTableColumn<T, C extends VFXTableCell<T>> extends VFXTableColumn<T, C> {

    //================================================================================
    // Constructors
    //================================================================================

    public VFXSimpleTableColumn() {}

    public VFXSimpleTableColumn(String text) {
        super(text);
    }

    public VFXSimpleTableColumn(String text, Node graphic) {
        super(text, graphic);
    }

    {
        // Prevent overlay from capturing mouse events on rows (mouse transparent is not enough)
        setPickOnBounds(false);
        // Start with a wider gap by default
        StyleUtils.initProperty(graphicTextGapProperty(), 8.0);
    }

    //================================================================================
    // Overridden Methods
    //================================================================================

    @Override
    public Supplier<MFXSkinBase<? extends Node>> defaultSkinFactory() {
        return () -> new VFXSimpleTableColumnSkin<>(this);
    }

    //================================================================================
    // Styleable Properties
    //================================================================================

    private final StyleableObjectProperty<HPos> graphicAlignment = new StyleableObjectProperty<>(
        StyleableProperties.GRAPHIC_ALIGNMENT,
        this,
        "graphicAlignment",
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

    public HPos getGraphicAlignment() {
        return graphicAlignment.get();
    }

    public StyleableObjectProperty<HPos> graphicAlignmentProperty() {
        return graphicAlignment;
    }

    public void setGraphicAlignment(HPos graphicAlignment) {
        this.graphicAlignment.set(graphicAlignment);
    }

    public boolean isEnableOverlay() {
        return enableOverlay.get();
    }

    public StyleableBooleanProperty enableOverlayProperty() {
        return enableOverlay;
    }

    public void setEnableOverlay(boolean enableOverlay) {
        this.enableOverlay.set(enableOverlay);
    }

    public boolean isOverlayOnHeader() {
        return overlayOnHeader.get();
    }

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
        private static final StyleablePropertyFactory<VFXSimpleTableColumn<?, ?>> FACTORY = new StyleablePropertyFactory<>(VFXTableColumn.getClassCssMetaData());
        private static final List<CssMetaData<? extends Styleable, ?>> cssMetaDataList;

        private static final CssMetaData<VFXSimpleTableColumn<?, ?>, HPos> GRAPHIC_ALIGNMENT =
            FACTORY.createEnumCssMetaData(
                HPos.class,
                "-vfx-graphic-alignment",
                VFXSimpleTableColumn::graphicAlignmentProperty,
                HPos.RIGHT
            );

        private static final CssMetaData<VFXSimpleTableColumn<?, ?>, Boolean> ENABLE_OVERLAY =
            FACTORY.createBooleanCssMetaData(
                "-vfx-enable-overlay",
                VFXSimpleTableColumn::enableOverlayProperty,
                true
            );

        private static final CssMetaData<VFXSimpleTableColumn<?, ?>, Boolean> OVERLAY_ON_HEADER =
            FACTORY.createBooleanCssMetaData(
                "-vfx-overlay-on-header",
                VFXSimpleTableColumn::overlayOnHeaderProperty,
                false
            );

        static {
            cssMetaDataList = StyleUtils.cssMetaDataList(
                VFXTableColumn.getClassCssMetaData(),
                GRAPHIC_ALIGNMENT, ENABLE_OVERLAY, OVERLAY_ON_HEADER
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
